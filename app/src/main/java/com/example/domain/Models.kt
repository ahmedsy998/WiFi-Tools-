package com.example.domain

enum class WifiBand(val label: String) {
    BAND_2_4_GHZ("2.4 GHz"),
    BAND_5_GHZ("5 GHz"),
    BAND_6_GHZ("6 GHz"),
    UNKNOWN("Unknown")
}

enum class SecurityProtocol(val label: String, val baseScore: Int) {
    WPA3_ENTERPRISE("WPA3-Enterprise", 100),
    WPA3_SAE("WPA3-Personal (SAE)", 98),
    WPA2_WPA3_TRANSITION("WPA2/WPA3 Transition", 88),
    WPA2_ENTERPRISE("WPA2-Enterprise", 86),
    WPA2_PSK("WPA2-PSK (AES)", 80),
    OWE("OWE (Enhanced Open)", 68),
    WPA_LEGACY("WPA (Legacy TKIP)", 40),
    WEP_LEGACY("WEP (Deprecated)", 15),
    OPEN("Open (Unencrypted)", 5),
    UNKNOWN("Unknown", 35)
}

enum class RiskLevel(val label: String) {
    SECURE("SECURE"),
    LOW("LOW RISK"),
    MODERATE("MODERATE"),
    HIGH("HIGH RISK"),
    CRITICAL("CRITICAL")
}

enum class NetworkSortOption(val label: String) {
    SIGNAL_DESC("Signal Strongest"),
    SECURITY_RISK("Highest Risk"),
    CHANNEL_ASC("Channel Asc"),
    SSID_ASC("SSID A-Z")
}

enum class SecurityFilter(val label: String) {
    ALL("All"),
    WEAK_OR_OPEN("Weak / Open"),
    WPA2("WPA2"),
    WPA3("WPA3")
}

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val frequencyMhz: Int,
    val channel: Int,
    val band: WifiBand,
    val rssi: Int,
    val signalPercent: Int,
    val capabilities: String,
    val securityProtocol: SecurityProtocol,
    val channelWidth: String,
    val wifiStandard: String,
    val hasWps: Boolean,
    val hasTkip: Boolean,
    val isHidden: Boolean,
    val securityScore: Int,
    val riskLevel: RiskLevel,
    val timestampMs: Long
)

data class ActiveConnectionInfo(
    val isConnectedToWifi: Boolean = false,
    val isWifiEnabled: Boolean = false,
    val ssid: String = "Not Connected",
    val bssid: String = "N/A",
    val rssiDbm: Int = -100,
    val signalPercent: Int = 0,
    val frequencyMhz: Int = 0,
    val channel: Int = -1,
    val band: WifiBand = WifiBand.UNKNOWN,
    val linkSpeedMbps: Int = 0,
    val txLinkSpeedMbps: Int? = null,
    val rxLinkSpeedMbps: Int? = null,
    val wifiStandard: String = "Unknown",
    val localIpAddresses: List<String> = emptyList(),
    val gatewayIp: String? = null,
    val dnsServers: List<String> = emptyList(),
    val subnetMaskOrPrefix: String? = null,
    val interfaceName: String? = null,
    val isPrivateDnsActive: Boolean = false,
    val privateDnsServerName: String? = null,
    val hasValidatedInternet: Boolean = false,
    val isCaptivePortal: Boolean = false,
    val isMetered: Boolean = false,
    val isVpnActive: Boolean = false,
    val mtu: Int = 0,
    val securityProtocol: SecurityProtocol = SecurityProtocol.UNKNOWN,
    val limitationNote: String? = null
)

data class ChannelCongestion(
    val channel: Int,
    val band: WifiBand,
    val networkCount: Int,
    val overlappingCount: Int,
    val congestionScore: Int,
    val strongestRssi: Int?,
    val isNonOverlappingStandard: Boolean,
    val isRecommended: Boolean
)

data class SecurityFinding(
    val title: String,
    val severity: RiskLevel,
    val description: String,
    val recommendation: String
)

data class NetworkSecurityAssessment(
    val ssid: String,
    val bssid: String,
    val protocol: SecurityProtocol,
    val score: Int,
    val riskLevel: RiskLevel,
    val findings: List<SecurityFinding>,
    val hardeningSteps: List<String>
)

data class EncryptionStandardInfo(
    val name: String,
    val statusBadge: String,
    val riskLevel: RiskLevel,
    val cipherSuite: String,
    val summary: String,
    val vulnerabilities: String,
    val guidance: String
)

data class HardeningItem(
    val id: String,
    val title: String,
    val category: String,
    val detail: String,
    val priority: RiskLevel
)

data class PasswordAnalysisResult(
    val length: Int,
    val entropyBits: Double,
    val wpaLengthCompliant: Boolean,
    val score: Int,
    val strengthLabel: String,
    val riskLevel: RiskLevel,
    val hasLowercase: Boolean,
    val hasUppercase: Boolean,
    val hasDigits: Boolean,
    val hasSymbols: Boolean,
    val estimatedCrackResistance: String,
    val warnings: List<String>,
    val recommendations: List<String>
)

data class AuthorizedAuditCheck(
    val name: String,
    val status: AuditCheckStatus,
    val detail: String,
    val remediation: String
)

enum class AuditCheckStatus(val label: String) {
    PASS("PASS"),
    WARN("WARN"),
    FAIL("FAIL"),
    INFO("INFO")
}

data class AuthorizedAuditReport(
    val targetSsid: String,
    val targetGateway: String?,
    val timestampMs: Long,
    val overallScore: Int,
    val checks: List<AuthorizedAuditCheck>
)

data class ProbeSample(
    val sequence: Int,
    val reachable: Boolean,
    val latencyMs: Long?,
    val method: String,
    val errorDetail: String? = null
)

data class LatencyDiagnosticResult(
    val targetHost: String,
    val resolvedIp: String?,
    val targetPort: Int,
    val samples: List<ProbeSample>,
    val packetsSent: Int,
    val packetsReceived: Int,
    val packetLossPercent: Double,
    val minLatencyMs: Long?,
    val avgLatencyMs: Double?,
    val maxLatencyMs: Long?,
    val jitterMs: Double?,
    val timestampMs: Long,
    val summary: String
)

data class ConnectivityCheckResult(
    val dnsResolutionMs: Long?,
    val resolvedTestHost: String?,
    val tcpInternetReachable: Boolean,
    val tcpInternetLatencyMs: Long?,
    val gatewayReachable: Boolean,
    val gatewayLatencyMs: Long?,
    val isCaptivePortal: Boolean,
    val timestampMs: Long,
    val statusMessage: String
)

data class ScanHistoryRecord(
    val id: Long = 0,
    val timestampMs: Long,
    val totalNetworks: Int,
    val openOrWeakCount: Int,
    val wpa3Count: Int,
    val strongestSsid: String,
    val strongestRssi: Int,
    val connectedSsid: String?,
    val detailsJson: String
)

data class SecurityReportRecord(
    val id: Long = 0,
    val timestampMs: Long,
    val title: String,
    val reportType: String,
    val targetName: String,
    val overallScore: Int,
    val riskLevel: RiskLevel,
    val summary: String,
    val fullMarkdown: String,
    val fullJson: String
)
