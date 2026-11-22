package com.example.reports

import com.example.domain.ActiveConnectionInfo
import com.example.domain.AuthorizedAuditReport
import com.example.domain.LatencyDiagnosticResult
import com.example.domain.RiskLevel
import com.example.domain.SecurityProtocol
import com.example.domain.SecurityReportRecord
import com.example.domain.WifiNetwork
import com.example.utils.SignalQualityUtils
import org.json.JSONArray
import org.json.JSONObject

class ReportGenerator {

    fun buildComprehensiveReport(
        connection: ActiveConnectionInfo,
        networks: List<WifiNetwork>,
        auditReport: AuthorizedAuditReport?,
        latencyResult: LatencyDiagnosticResult?
    ): SecurityReportRecord {
        val now = System.currentTimeMillis()
        val weakCount = networks.count {
            it.securityProtocol == SecurityProtocol.OPEN ||
                it.securityProtocol == SecurityProtocol.WEP_LEGACY ||
                it.securityProtocol == SecurityProtocol.WPA_LEGACY ||
                it.hasWps
        }
        val wpa3Count = networks.count {
            it.securityProtocol == SecurityProtocol.WPA3_SAE ||
                it.securityProtocol == SecurityProtocol.WPA3_ENTERPRISE ||
                it.securityProtocol == SecurityProtocol.WPA2_WPA3_TRANSITION
        }

        val score = auditReport?.overallScore ?: when {
            networks.isEmpty() && !connection.isConnectedToWifi -> 50
            networks.isEmpty() -> connection.securityProtocol.baseScore
            else -> networks.map { it.securityScore }.average().toInt().coerceIn(0, 100)
        }

        val risk = when {
            score >= 85 -> RiskLevel.SECURE
            score >= 70 -> RiskLevel.LOW
            score >= 50 -> RiskLevel.MODERATE
            score >= 30 -> RiskLevel.HIGH
            else -> RiskLevel.CRITICAL
        }

        val targetName = if (connection.isConnectedToWifi) connection.ssid else "RF Spectrum Survey"
        val title = "Wi-Fi Assessment — $targetName"
        val summary = buildString {
            append("APs Discovered: ${networks.size} | Weak/WPS: $weakCount | WPA3-Capable: $wpa3Count")
            if (auditReport != null) {
                append(" | Audit Score: ${auditReport.overallScore}/100")
            }
            if (latencyResult != null) {
                val avg = latencyResult.avgLatencyMs?.let { "${"%.1f".format(it)} ms" } ?: "N/A"
                append(" | RTT Avg: $avg (${"%.0f".format(latencyResult.packetLossPercent)}% loss)")
            }
        }

        val markdown = buildMarkdown(now, connection, networks, auditReport, latencyResult, score, risk)
        val json = buildJson(now, connection, networks, auditReport, latencyResult, score, risk)

        return SecurityReportRecord(
            timestampMs = now,
            title = title,
            reportType = if (auditReport != null) "AUTHORIZED_AUDIT" else "SPECTRUM_DIAGNOSTIC",
            targetName = targetName,
            overallScore = score,
            riskLevel = risk,
            summary = summary,
            fullMarkdown = markdown,
            fullJson = json
        )
    }

    fun serializeNetworksToJson(networks: List<WifiNetwork>): String {
        val arr = JSONArray()
        networks.forEach { n ->
            arr.put(
                JSONObject().apply {
                    put("ssid", n.ssid)
                    put("bssid", n.bssid)
                    put("frequencyMhz", n.frequencyMhz)
                    put("channel", n.channel)
                    put("band", n.band.label)
                    put("rssi", n.rssi)
                    put("signalPercent", n.signalPercent)
                    put("securityProtocol", n.securityProtocol.label)
                    put("hasWps", n.hasWps)
                    put("hasTkip", n.hasTkip)
                    put("securityScore", n.securityScore)
                    put("riskLevel", n.riskLevel.label)
                }
            )
        }
        return arr.toString()
    }

    private fun buildMarkdown(
        timestampMs: Long,
        connection: ActiveConnectionInfo,
        networks: List<WifiNetwork>,
        auditReport: AuthorizedAuditReport?,
        latencyResult: LatencyDiagnosticResult?,
        score: Int,
        risk: RiskLevel
    ): String = buildString {
        appendLine("# WiFi Tools — Security & Network Diagnostic Assessment")
        appendLine()
        appendLine("- **Generated:** ${SignalQualityUtils.formatTimestamp(timestampMs)}")
        appendLine("- **Overall Security Score:** $score / 100 (${risk.label})")
        appendLine("- **Scope Notice:** Generated via standard Android APIs for authorized security evaluation.")
        appendLine()

        appendLine("## 1. Active Connection Telemetry")
        if (connection.isConnectedToWifi) {
            appendLine("- **SSID:** ${connection.ssid}")
            appendLine("- **BSSID:** ${connection.bssid}")
            appendLine("- **Signal:** ${connection.rssiDbm} dBm (${connection.signalPercent}%)")
            appendLine("- **Band / Channel:** ${connection.band.label} • Ch ${connection.channel} (${connection.frequencyMhz} MHz)")
            appendLine("- **Standard / Link Speed:** ${connection.wifiStandard} • ${connection.linkSpeedMbps} Mbps")
            appendLine("- **Local IP(s):** ${connection.localIpAddresses.joinToString().ifBlank { "Unavailable" }}")
            appendLine("- **Gateway:** ${connection.gatewayIp ?: "Unavailable"}")
            appendLine("- **DNS Servers:** ${connection.dnsServers.joinToString().ifBlank { "Unavailable" }}")
            appendLine("- **Private DNS (DoT):** ${if (connection.isPrivateDnsActive) "Active (${connection.privateDnsServerName ?: "Opportunistic"})" else "Disabled"}")
        } else {
            appendLine("- Not connected to a Wi-Fi network at report generation time.")
        }
        appendLine()

        if (auditReport != null) {
            appendLine("## 2. Authorized Configuration Audit (${auditReport.overallScore}/100)")
            auditReport.checks.forEach { check ->
                appendLine("- **[${check.status.label}] ${check.name}:** ${check.detail}")
                appendLine("  - *Guidance:* ${check.remediation}")
            }
            appendLine()
        }

        if (latencyResult != null) {
            appendLine("## 3. Router & Latency Diagnostics")
            appendLine("- **Target:** ${latencyResult.targetHost}:${latencyResult.targetPort} (Resolved: ${latencyResult.resolvedIp ?: "N/A"})")
            appendLine("- **Probes:** ${latencyResult.packetsReceived}/${latencyResult.packetsSent} received (${"%.1f".format(latencyResult.packetLossPercent)}% loss)")
            appendLine("- **RTT (Min/Avg/Max):** ${latencyResult.minLatencyMs ?: "-"} / ${latencyResult.avgLatencyMs?.let { "%.1f".format(it) } ?: "-"} / ${latencyResult.maxLatencyMs ?: "-"} ms")
            appendLine("- **Jitter:** ${latencyResult.jitterMs?.let { "%.1f ms".format(it) } ?: "N/A"}")
            appendLine("- **Summary:** ${latencyResult.summary}")
            appendLine()
        }

        appendLine("## 4. Discovered Nearby Access Points (${networks.size})")
        if (networks.isEmpty()) {
            appendLine("No nearby networks recorded in current scan snapshot.")
        } else {
            appendLine("| SSID | BSSID | Band | Ch | RSSI | Security | WPS | Score |")
            appendLine("|---|---|---|---|---|---|---|---|")
            networks.take(40).forEach { n ->
                appendLine("| ${n.ssid.replace("|", "/")} | `${n.bssid}` | ${n.band.label} | ${n.channel} | ${n.rssi} dBm | ${n.securityProtocol.label} | ${if (n.hasWps) "YES" else "No"} | ${n.securityScore} |")
            }
        }
    }

    private fun buildJson(
        timestampMs: Long,
        connection: ActiveConnectionInfo,
        networks: List<WifiNetwork>,
        auditReport: AuthorizedAuditReport?,
        latencyResult: LatencyDiagnosticResult?,
        score: Int,
        risk: RiskLevel
    ): String {
        val root = JSONObject()
        root.put("generatedAt", SignalQualityUtils.formatTimestamp(timestampMs))
        root.put("timestampMs", timestampMs)
        root.put("overallScore", score)
        root.put("riskLevel", risk.label)

        root.put("activeConnection", JSONObject().apply {
            put("connected", connection.isConnectedToWifi)
            put("ssid", connection.ssid)
            put("bssid", connection.bssid)
            put("rssiDbm", connection.rssiDbm)
            put("band", connection.band.label)
            put("channel", connection.channel)
            put("frequencyMhz", connection.frequencyMhz)
            put("gatewayIp", connection.gatewayIp)
            put("dnsServers", JSONArray(connection.dnsServers))
            put("privateDnsActive", connection.isPrivateDnsActive)
        })

        if (auditReport != null) {
            val checksArr = JSONArray()
            auditReport.checks.forEach { c ->
                checksArr.put(JSONObject().apply {
                    put("name", c.name)
                    put("status", c.status.label)
                    put("detail", c.detail)
                    put("remediation", c.remediation)
                })
            }
            root.put("authorizedAudit", JSONObject().apply {
                put("targetSsid", auditReport.targetSsid)
                put("overallScore", auditReport.overallScore)
                put("checks", checksArr)
            })
        }

        if (latencyResult != null) {
            root.put("routerDiagnostics", JSONObject().apply {
                put("targetHost", latencyResult.targetHost)
                put("resolvedIp", latencyResult.resolvedIp)
                put("port", latencyResult.targetPort)
                put("packetsSent", latencyResult.packetsSent)
                put("packetsReceived", latencyResult.packetsReceived)
                put("packetLossPercent", latencyResult.packetLossPercent)
                put("minLatencyMs", latencyResult.minLatencyMs)
                put("avgLatencyMs", latencyResult.avgLatencyMs)
                put("maxLatencyMs", latencyResult.maxLatencyMs)
                put("jitterMs", latencyResult.jitterMs)
            })
        }

        root.put("discoveredNetworks", JSONArray(serializeNetworksToJson(networks)))
        return root.toString(2)
    }
}
