package com.example.diagnostics

import com.example.domain.ActiveConnectionInfo
import com.example.domain.ConnectivityCheckResult
import com.example.domain.LatencyDiagnosticResult
import com.example.domain.ProbeSample
import com.example.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.abs

/**
 * Performs genuine network connectivity, DNS resolution, gateway reachability,
 * RTT latency, jitter, and packet-loss diagnostics using standard Java/Android sockets.
 */
class RouterDiagnosticsEngine {

    private val allowedHostPattern = Regex("^[a-zA-Z0-9._:-]{1,253}$")

    fun isValidTargetHost(host: String): Boolean {
        val trimmed = host.trim()
        return trimmed.isNotBlank() && allowedHostPattern.matches(trimmed)
    }

    suspend fun runConnectivityDiagnostics(
        connection: ActiveConnectionInfo
    ): ConnectivityCheckResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        var dnsMs: Long? = null
        var resolvedIp: String? = null
        var internetReachable = false
        var internetLatency: Long? = null
        var gatewayReachable = false
        var gatewayLatency: Long? = null

        // 1. DNS Resolution Timing
        try {
            val start = System.nanoTime()
            val addr = InetAddress.getByName("dns.google")
            dnsMs = ((System.nanoTime() - start) / 1_000_000L).coerceAtLeast(1L)
            resolvedIp = addr.hostAddress
        } catch (e: Exception) {
            AppLogger.w("Diagnostics", "DNS resolution failed: ${e.message}")
        }

        // 2. TCP Reachability to well-known public DNS over TLS/DNS endpoint (1.1.1.1:443)
        val tcpProbe = measureSingleProbe("1.1.1.1", 443, 1800)
        internetReachable = tcpProbe.reachable
        internetLatency = tcpProbe.latencyMs

        // 3. Gateway Reachability Check
        val gw = connection.gatewayIp
        if (!gw.isNullOrBlank()) {
            val gwProbe = measureGatewayReachability(gw, 1200)
            gatewayReachable = gwProbe.reachable
            gatewayLatency = gwProbe.latencyMs
        }

        val status = when {
            connection.isCaptivePortal -> "Captive portal detected — sign-in required before full internet routing."
            internetReachable && gatewayReachable -> "Gateway and upstream internet are both reachable."
            internetReachable -> "Upstream internet reachable (gateway did not reply to direct TCP/ICMP probe)."
            gatewayReachable -> "Local gateway reachable, but upstream internet routing failed."
            else -> "No upstream connectivity detected. Verify Wi-Fi association and DHCP lease."
        }

        ConnectivityCheckResult(
            dnsResolutionMs = dnsMs,
            resolvedTestHost = resolvedIp,
            tcpInternetReachable = internetReachable,
            tcpInternetLatencyMs = internetLatency,
            gatewayReachable = gatewayReachable,
            gatewayLatencyMs = gatewayLatency,
            isCaptivePortal = connection.isCaptivePortal,
            timestampMs = now,
            statusMessage = status
        )
    }

    suspend fun runLatencyAndLossTest(
        targetHost: String,
        port: Int = 443,
        probeCount: Int = 6,
        timeoutMs: Int = 1500,
        onProgress: (ProbeSample) -> Unit = {}
    ): LatencyDiagnosticResult = withContext(Dispatchers.IO) {
        val cleanHost = targetHost.trim()
        val safePort = port.coerceIn(1, 65535)
        val count = probeCount.coerceIn(1, 16)
        val now = System.currentTimeMillis()

        if (!isValidTargetHost(cleanHost)) {
            return@withContext LatencyDiagnosticResult(
                targetHost = cleanHost,
                resolvedIp = null,
                targetPort = safePort,
                samples = emptyList(),
                packetsSent = 0,
                packetsReceived = 0,
                packetLossPercent = 100.0,
                minLatencyMs = null,
                avgLatencyMs = null,
                maxLatencyMs = null,
                jitterMs = null,
                timestampMs = now,
                summary = "Invalid host syntax. Enter a valid IPv4/IPv6 address or hostname."
            )
        }

        val resolvedIp = runCatching { InetAddress.getByName(cleanHost).hostAddress }.getOrNull()
        val hostForProbe = resolvedIp ?: cleanHost
        val samples = mutableListOf<ProbeSample>()

        for (seq in 1..count) {
            val sample = measureSingleProbe(hostForProbe, safePort, timeoutMs, seq)
            samples += sample
            onProgress(sample)
            if (seq < count) {
                delay(180)
            }
        }

        val successfulRtts = samples.mapNotNull { if (it.reachable) it.latencyMs else null }
        val received = successfulRtts.size
        val lossPercent = ((count - received).toDouble() / count.toDouble()) * 100.0
        val minMs = successfulRtts.minOrNull()
        val maxMs = successfulRtts.maxOrNull()
        val avgMs = if (successfulRtts.isNotEmpty()) successfulRtts.average() else null

        val jitterMs = if (successfulRtts.size >= 2) {
            val diffs = successfulRtts.zipWithNext { a, b -> abs(b - a).toDouble() }
            diffs.average()
        } else {
            null
        }

        val summary = when {
            received == 0 -> "100% probe loss to $cleanHost:$safePort. Host may be offline, unreachable, or filtering port $safePort."
            lossPercent > 0.0 -> "Partial reachability ($received/$count replies, ${"%.1f".format(lossPercent)}% loss, avg ${"%.1f".format(avgMs)} ms)."
            (avgMs ?: 0.0) <= 35.0 -> "Optimal low-latency path ($received/$count replies, avg ${"%.1f".format(avgMs)} ms)."
            (avgMs ?: 0.0) <= 100.0 -> "Stable path with moderate latency ($received/$count replies, avg ${"%.1f".format(avgMs)} ms)."
            else -> "Elevated latency detected ($received/$count replies, avg ${"%.1f".format(avgMs)} ms)."
        }

        LatencyDiagnosticResult(
            targetHost = cleanHost,
            resolvedIp = resolvedIp,
            targetPort = safePort,
            samples = samples,
            packetsSent = count,
            packetsReceived = received,
            packetLossPercent = lossPercent,
            minLatencyMs = minMs,
            avgLatencyMs = avgMs,
            maxLatencyMs = maxMs,
            jitterMs = jitterMs,
            timestampMs = now,
            summary = summary
        )
    }

    private fun measureGatewayReachability(gatewayIp: String, timeoutMs: Int): ProbeSample {
        // Try standard local gateway ports (53 DNS, 80 HTTP, 443 HTTPS) then ICMP isReachable
        for (port in listOf(53, 80, 443)) {
            val attempt = tryTcpConnect(gatewayIp, port, timeoutMs / 2, sequence = 1)
            if (attempt.reachable) return attempt
        }
        return tryIcmpReachable(gatewayIp, timeoutMs, sequence = 1)
    }

    private fun measureSingleProbe(
        host: String,
        port: Int,
        timeoutMs: Int,
        sequence: Int = 1
    ): ProbeSample {
        val tcpResult = tryTcpConnect(host, port, timeoutMs, sequence)
        if (tcpResult.reachable) return tcpResult

        // Fallback to InetAddress.isReachable (ICMP echo / TCP 7) if requested port is filtered
        val icmpResult = tryIcmpReachable(host, timeoutMs / 2, sequence)
        return if (icmpResult.reachable) icmpResult else tcpResult
    }

    private fun tryTcpConnect(
        host: String,
        port: Int,
        timeoutMs: Int,
        sequence: Int
    ): ProbeSample {
        val startNs = System.nanoTime()
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
            }
            val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
            ProbeSample(
                sequence = sequence,
                reachable = true,
                latencyMs = elapsedMs,
                method = "TCP:$port"
            )
        } catch (e: Exception) {
            ProbeSample(
                sequence = sequence,
                reachable = false,
                latencyMs = null,
                method = "TCP:$port",
                errorDetail = e.javaClass.simpleName
            )
        }
    }

    private fun tryIcmpReachable(
        host: String,
        timeoutMs: Int,
        sequence: Int
    ): ProbeSample {
        val startNs = System.nanoTime()
        return try {
            val reachable = InetAddress.getByName(host).isReachable(timeoutMs)
            val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
            ProbeSample(
                sequence = sequence,
                reachable = reachable,
                latencyMs = if (reachable) elapsedMs else null,
                method = "ICMP",
                errorDetail = if (reachable) null else "Timeout"
            )
        } catch (e: Exception) {
            ProbeSample(
                sequence = sequence,
                reachable = false,
                latencyMs = null,
                method = "ICMP",
                errorDetail = e.javaClass.simpleName
            )
        }
    }
}
