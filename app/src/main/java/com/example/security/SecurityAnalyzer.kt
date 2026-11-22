package com.example.security

import com.example.domain.NetworkSecurityAssessment
import com.example.domain.RiskLevel
import com.example.domain.SecurityFinding
import com.example.domain.SecurityProtocol
import com.example.domain.WifiNetwork

class SecurityAnalyzer {

    fun parseSecurityProtocol(capabilities: String): SecurityProtocol {
        val caps = capabilities.uppercase()
        val hasSae = caps.contains("SAE") || caps.contains("WPA3")
        val hasWpa2 = caps.contains("WPA2") || caps.contains("RSN")
        val hasWpa1 = caps.contains("[WPA-") || caps.contains("WPA-PSK") || caps.contains("WPA-EAP")
        val hasEap = caps.contains("EAP")
        val hasWep = caps.contains("WEP")
        val hasOwe = caps.contains("OWE")

        return when {
            hasSae && hasEap -> SecurityProtocol.WPA3_ENTERPRISE
            hasSae && hasWpa2 -> SecurityProtocol.WPA2_WPA3_TRANSITION
            hasSae -> SecurityProtocol.WPA3_SAE
            hasWpa2 && hasEap -> SecurityProtocol.WPA2_ENTERPRISE
            hasWpa2 -> SecurityProtocol.WPA2_PSK
            hasOwe -> SecurityProtocol.OWE
            hasWpa1 -> SecurityProtocol.WPA_LEGACY
            hasWep -> SecurityProtocol.WEP_LEGACY
            caps.isBlank() || (!hasSae && !hasWpa2 && !hasWpa1 && !hasWep && !hasOwe) -> SecurityProtocol.OPEN
            else -> SecurityProtocol.UNKNOWN
        }
    }

    fun hasWps(capabilities: String): Boolean = capabilities.uppercase().contains("WPS")

    fun hasTkip(capabilities: String): Boolean = capabilities.uppercase().contains("TKIP")

    fun calculateScore(
        protocol: SecurityProtocol,
        hasWps: Boolean,
        hasTkip: Boolean,
        isHidden: Boolean
    ): Int {
        var score = protocol.baseScore
        if (hasWps) score -= 18
        if (hasTkip && protocol != SecurityProtocol.WPA_LEGACY) score -= 14
        if (isHidden) score -= 3 // Hidden SSID offers negligible privacy & causes active probe leakage
        return score.coerceIn(0, 100)
    }

    fun scoreToRiskLevel(score: Int): RiskLevel = when {
        score >= 88 -> RiskLevel.SECURE
        score >= 74 -> RiskLevel.LOW
        score >= 55 -> RiskLevel.MODERATE
        score >= 30 -> RiskLevel.HIGH
        else -> RiskLevel.CRITICAL
    }

    fun assessNetwork(network: WifiNetwork): NetworkSecurityAssessment {
        val findings = mutableListOf<SecurityFinding>()
        val hardeningSteps = mutableListOf<String>()

        when (network.securityProtocol) {
            SecurityProtocol.OPEN -> {
                findings += SecurityFinding(
                    title = "Unencrypted Open Network",
                    severity = RiskLevel.CRITICAL,
                    description = "All 802.11 wireless frames are transmitted without link-layer encryption, allowing passive eavesdropping on non-TLS traffic.",
                    recommendation = "Configure WPA3-Personal (SAE) or WPA2-AES immediately; use a VPN if you must connect to public open Wi-Fi."
                )
                hardeningSteps += "Enable WPA3-SAE or WPA2-AES (CCMP) encryption in the router's Wireless Security settings."
            }
            SecurityProtocol.WEP_LEGACY -> {
                findings += SecurityFinding(
                    title = "Deprecated WEP Encryption",
                    severity = RiskLevel.CRITICAL,
                    description = "WEP uses broken RC4 initialization vectors and is trivially compromised with passive packet analysis.",
                    recommendation = "Disable WEP immediately and migrate to WPA2-AES or WPA3-SAE."
                )
                hardeningSteps += "Replace legacy WEP equipment or upgrade security mode to WPA2/WPA3."
            }
            SecurityProtocol.WPA_LEGACY -> {
                findings += SecurityFinding(
                    title = "Legacy WPA1 / TKIP Protocol",
                    severity = RiskLevel.HIGH,
                    description = "Original WPA relies on TKIP, which has known cryptographic weaknesses and is deprecated by the Wi-Fi Alliance.",
                    recommendation = "Switch to WPA2-AES (CCMP) minimum or WPA3-Personal."
                )
                hardeningSteps += "Disable legacy WPA1/TKIP compatibility mode on the access point."
            }
            SecurityProtocol.OWE -> {
                findings += SecurityFinding(
                    title = "Opportunistic Wireless Encryption (OWE)",
                    severity = RiskLevel.MODERATE,
                    description = "Encrypts over-the-air traffic without a shared password, preventing passive sniffing, though it does not authenticate the access point identity.",
                    recommendation = "Appropriate for public guest hotspots; use WPA3-SAE for private home or corporate networks."
                )
            }
            SecurityProtocol.WPA2_PSK -> {
                findings += SecurityFinding(
                    title = "WPA2-Personal Active",
                    severity = RiskLevel.LOW,
                    description = "Strong AES-CCMP encryption when paired with a high-entropy passphrase, though vulnerable to offline dictionary attacks if the passphrase is weak.",
                    recommendation = "Upgrade to WPA3-SAE or WPA2/WPA3 Transition mode and ensure a 16+ character random passphrase."
                )
                hardeningSteps += "Enable Protected Management Frames (802.11w / PMF) and transition to WPA3-SAE if supported."
            }
            SecurityProtocol.WPA2_ENTERPRISE -> {
                findings += SecurityFinding(
                    title = "WPA2-Enterprise (802.1X)",
                    severity = RiskLevel.LOW,
                    description = "Per-user RADIUS/802.1X authentication isolates credentials across clients.",
                    recommendation = "Verify server TLS certificate validation on client devices and adopt WPA3-Enterprise when possible."
                )
            }
            SecurityProtocol.WPA2_WPA3_TRANSITION -> {
                findings += SecurityFinding(
                    title = "WPA2/WPA3 Transition Mode",
                    severity = RiskLevel.SECURE,
                    description = "Supports modern WPA3-SAE clients while retaining WPA2 compatibility. Legacy downgrade paths remain possible if WPA2 passphrases are weak.",
                    recommendation = "Use a strong 16+ character passphrase and move to WPA3-Only once legacy IoT clients are retired."
                )
            }
            SecurityProtocol.WPA3_SAE, SecurityProtocol.WPA3_ENTERPRISE -> {
                findings += SecurityFinding(
                    title = "Modern WPA3 Protection",
                    severity = RiskLevel.SECURE,
                    description = "Uses Simultaneous Authentication of Equals (Dragonfly handshake), mandatory Protected Management Frames (PMF), and forward secrecy.",
                    recommendation = "Maintain firmware updates and keep WPS disabled."
                )
            }
            SecurityProtocol.UNKNOWN -> {
                findings += SecurityFinding(
                    title = "Unclassified Capability String",
                    severity = RiskLevel.MODERATE,
                    description = "Android reported capabilities '${network.capabilities}' that could not be matched to a standard cipher suite.",
                    recommendation = "Verify router wireless security settings directly from the administration panel."
                )
            }
        }

        if (network.hasWps) {
            findings += SecurityFinding(
                title = "WPS (Wi-Fi Protected Setup) Advertised",
                severity = RiskLevel.HIGH,
                description = "WPS PIN validation splits the 8-digit PIN into two halves, drastically reducing brute-force search space on routers without strict lockout.",
                recommendation = "Disable WPS completely in router wireless settings."
            )
            hardeningSteps += "Turn off WPS (both PIN and Push-Button methods) in the router admin interface."
        }

        if (network.hasTkip) {
            findings += SecurityFinding(
                title = "Legacy TKIP Cipher Enabled",
                severity = RiskLevel.HIGH,
                description = "TKIP reduces wireless throughput and introduces known replay and MIC weaknesses compared to AES-CCMP.",
                recommendation = "Set cipher mode strictly to AES-CCMP (or GCMP-256 for WPA3)."
            )
            hardeningSteps += "Change encryption cipher from TKIP+AES mixed mode to AES-only (CCMP)."
        }

        if (network.isHidden) {
            findings += SecurityFinding(
                title = "Hidden SSID Broadcast",
                severity = RiskLevel.LOW,
                description = "Hiding the SSID does not add cryptographic security and causes client devices to actively broadcast probe requests for the network name.",
                recommendation = "Broadcast a neutral SSID that reveals no personal or router model details and rely on WPA3/WPA2-AES."
            )
        }

        if (hardeningSteps.isEmpty()) {
            hardeningSteps += "Keep router firmware patched and segment IoT devices onto a separate Guest VLAN."
            hardeningSteps += "Use unique administrator credentials and disable remote WAN management."
        }

        return NetworkSecurityAssessment(
            ssid = network.ssid,
            bssid = network.bssid,
            protocol = network.securityProtocol,
            score = network.securityScore,
            riskLevel = network.riskLevel,
            findings = findings,
            hardeningSteps = hardeningSteps.distinct()
        )
    }
}
