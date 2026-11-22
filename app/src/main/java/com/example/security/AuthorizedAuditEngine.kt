package com.example.security

import com.example.domain.ActiveConnectionInfo
import com.example.domain.AuditCheckStatus
import com.example.domain.AuthorizedAuditCheck
import com.example.domain.AuthorizedAuditReport
import com.example.domain.SecurityProtocol
import com.example.domain.WifiNetwork
import com.example.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Performs non-intrusive, strictly authorized configuration audits on the user's currently
 * connected Wi-Fi network and explicitly authorized local gateway.
 * Never attempts credential guessing, packet injection, or unauthorized access.
 */
class AuthorizedAuditEngine {

    suspend fun runAuthorizedConfigurationAudit(
        connection: ActiveConnectionInfo,
        matchedScanNetwork: WifiNetwork?
    ): AuthorizedAuditReport = withContext(Dispatchers.IO) {
        val checks = mutableListOf<AuthorizedAuditCheck>()

        if (!connection.isConnectedToWifi) {
            checks += AuthorizedAuditCheck(
                name = "Active Wi-Fi Link",
                status = AuditCheckStatus.FAIL,
                detail = "Device is not currently connected to a Wi-Fi network.",
                remediation = "Connect to your authorized Wi-Fi network and re-run the configuration audit."
            )
            return@withContext AuthorizedAuditReport(
                targetSsid = "Disconnected",
                targetGateway = null,
                timestampMs = System.currentTimeMillis(),
                overallScore = 0,
                checks = checks
            )
        }

        // 1. Encryption Protocol Check
        val protocol = matchedScanNetwork?.securityProtocol ?: connection.securityProtocol
        when (protocol) {
            SecurityProtocol.WPA3_SAE, SecurityProtocol.WPA3_ENTERPRISE -> {
                checks += AuthorizedAuditCheck(
                    name = "Wireless Encryption Standard",
                    status = AuditCheckStatus.PASS,
                    detail = "${protocol.label} detected with modern authentication and PMF.",
                    remediation = "Keep firmware updated."
                )
            }
            SecurityProtocol.WPA2_WPA3_TRANSITION, SecurityProtocol.WPA2_PSK, SecurityProtocol.WPA2_ENTERPRISE -> {
                checks += AuthorizedAuditCheck(
                    name = "Wireless Encryption Standard",
                    status = AuditCheckStatus.WARN,
                    detail = "${protocol.label} active. Secure with strong passphrase, but WPA3-Only offers stronger handshake defense.",
                    remediation = "Ensure AES-CCMP only and plan migration to WPA3-SAE."
                )
            }
            SecurityProtocol.OPEN, SecurityProtocol.WEP_LEGACY, SecurityProtocol.WPA_LEGACY -> {
                checks += AuthorizedAuditCheck(
                    name = "Wireless Encryption Standard",
                    status = AuditCheckStatus.FAIL,
                    detail = "Insecure or deprecated security detected: ${protocol.label}.",
                    remediation = "Immediately reconfigure router to WPA2-AES or WPA3-SAE."
                )
            }
            else -> {
                checks += AuthorizedAuditCheck(
                    name = "Wireless Encryption Standard",
                    status = AuditCheckStatus.INFO,
                    detail = "Protocol could not be resolved from Android scan cache (${protocol.label}). Run a fresh Wi-Fi scan with Location enabled.",
                    remediation = "Refresh the Network Scanner tab to match BSSID capabilities."
                )
            }
        }

        // 2. WPS Exposure Check
        if (matchedScanNetwork != null) {
            if (matchedScanNetwork.hasWps) {
                checks += AuthorizedAuditCheck(
                    name = "WPS Exposure",
                    status = AuditCheckStatus.FAIL,
                    detail = "Beacon capabilities advertise WPS enabled on ${matchedScanNetwork.bssid}.",
                    remediation = "Disable WPS in router administration settings."
                )
            } else {
                checks += AuthorizedAuditCheck(
                    name = "WPS Exposure",
                    status = AuditCheckStatus.PASS,
                    detail = "WPS is not advertised in access point beacon capabilities.",
                    remediation = "No action required."
                )
            }
        }

        // 3. Encrypted DNS (Private DNS / DoT)
        if (connection.isPrivateDnsActive) {
            checks += AuthorizedAuditCheck(
                name = "Encrypted DNS (DoT)",
                status = AuditCheckStatus.PASS,
                detail = "Android Private DNS is active (${connection.privateDnsServerName ?: "Opportunistic TLS"}).",
                remediation = "DNS queries are protected against local LAN eavesdropping."
            )
        } else {
            checks += AuthorizedAuditCheck(
                name = "Encrypted DNS (DoT)",
                status = AuditCheckStatus.WARN,
                detail = "Private DNS is off. Queries use plaintext UDP/53 to ${connection.dnsServers.joinToString().ifBlank { "router DNS" }}.",
                remediation = "Enable Private DNS in Android Settings -> Network & Internet (e.g. one.one.one.one or dns.google)."
            )
        }

        // 4. Captive Portal / Validated Internet Check
        if (connection.isCaptivePortal) {
            checks += AuthorizedAuditCheck(
                name = "Captive Portal Detection",
                status = AuditCheckStatus.WARN,
                detail = "Network intercepts HTTP traffic via a captive portal.",
                remediation = "Treat network as untrusted; avoid sensitive logins without VPN."
            )
        } else {
            checks += AuthorizedAuditCheck(
                name = "Captive Portal Detection",
                status = AuditCheckStatus.PASS,
                detail = if (connection.hasValidatedInternet) "Clean internet path validated without captive interception." else "No captive portal detected (internet validation pending or offline LAN).",
                remediation = "No action required."
            )
        }

        // 5. Authorized Local Gateway Admin Interface Check (non-intrusive TCP handshake on 80 / 443 / 23)
        val gateway = connection.gatewayIp
        if (!gateway.isNullOrBlank()) {
            val telnetOpen = probeTcpPort(gateway, 23, 600)
            val httpOpen = probeTcpPort(gateway, 80, 600)
            val httpsOpen = probeTcpPort(gateway, 443, 600)

            if (telnetOpen) {
                checks += AuthorizedAuditCheck(
                    name = "Gateway Legacy Telnet (Port 23)",
                    status = AuditCheckStatus.FAIL,
                    detail = "Gateway $gateway accepted a TCP connection on plaintext Telnet port 23.",
                    remediation = "Disable Telnet administration immediately; use HTTPS or SSH with key auth."
                )
            } else {
                checks += AuthorizedAuditCheck(
                    name = "Gateway Legacy Telnet (Port 23)",
                    status = AuditCheckStatus.PASS,
                    detail = "Gateway $gateway did not expose plaintext Telnet port 23.",
                    remediation = "Good perimeter hygiene."
                )
            }

            when {
                httpsOpen && !httpOpen -> checks += AuthorizedAuditCheck(
                    name = "Gateway Admin Transport (HTTP/HTTPS)",
                    status = AuditCheckStatus.PASS,
                    detail = "Gateway $gateway exposes HTTPS (443) without plaintext HTTP (80).",
                    remediation = "Ensure strong admin password and disable WAN admin access."
                )
                httpOpen && httpsOpen -> checks += AuthorizedAuditCheck(
                    name = "Gateway Admin Transport (HTTP/HTTPS)",
                    status = AuditCheckStatus.WARN,
                    detail = "Gateway $gateway responds on both HTTP (80) and HTTPS (443).",
                    remediation = "Disable plaintext HTTP admin login and enforce HTTPS-only management."
                )
                httpOpen -> checks += AuthorizedAuditCheck(
                    name = "Gateway Admin Transport (HTTP/HTTPS)",
                    status = AuditCheckStatus.WARN,
                    detail = "Gateway $gateway serves admin panel over unencrypted HTTP (80) only.",
                    remediation = "Enable HTTPS admin access in router settings if supported."
                )
                else -> checks += AuthorizedAuditCheck(
                    name = "Gateway Admin Transport (HTTP/HTTPS)",
                    status = AuditCheckStatus.INFO,
                    detail = "Standard HTTP/HTTPS admin ports (80/443) are closed or filtered on $gateway.",
                    remediation = "Router admin interface may be restricted to wired Ethernet or a custom port."
                )
            }
        } else {
            checks += AuthorizedAuditCheck(
                name = "Gateway Discovery",
                status = AuditCheckStatus.INFO,
                detail = "Default gateway IP could not be resolved from LinkProperties.",
                remediation = "Ensure IPv4/IPv6 DHCP lease is active."
            )
        }

        // 6. Signal Margin & Band Hygiene
        checks += AuthorizedAuditCheck(
            name = "RF Signal Margin & Band",
            status = if (connection.rssiDbm >= -72) AuditCheckStatus.PASS else AuditCheckStatus.WARN,
            detail = "Connected at ${connection.rssiDbm} dBm on ${connection.band.label} (${connection.frequencyMhz} MHz, ${connection.linkSpeedMbps} Mbps).",
            remediation = if (connection.rssiDbm < -72) "Weak signal increases retransmissions; relocate AP or use a wired mesh backhaul." else "Healthy RF link margin."
        )

        val totalWeight = checks.count { it.status != AuditCheckStatus.INFO }.coerceAtLeast(1)
        val earned = checks.sumOf {
            when (it.status) {
                AuditCheckStatus.PASS -> 100
                AuditCheckStatus.WARN -> 65
                AuditCheckStatus.FAIL -> 15
                AuditCheckStatus.INFO -> 0
            }
        }
        val overallScore = (earned / totalWeight).coerceIn(0, 100)

        AuthorizedAuditReport(
            targetSsid = connection.ssid,
            targetGateway = gateway,
            timestampMs = System.currentTimeMillis(),
            overallScore = overallScore,
            checks = checks
        )
    }

    private fun probeTcpPort(host: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (e: Exception) {
            AppLogger.d("AuthorizedAudit", "Port $port on $host closed/filtered: ${e.javaClass.simpleName}")
            false
        }
    }
}
