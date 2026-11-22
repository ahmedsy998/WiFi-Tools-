package com.example.presentation.viewmodel

import com.example.domain.ActiveConnectionInfo
import com.example.domain.AuthorizedAuditReport
import com.example.domain.ChannelCongestion
import com.example.domain.ConnectivityCheckResult
import com.example.domain.LatencyDiagnosticResult
import com.example.domain.NetworkSecurityAssessment
import com.example.domain.NetworkSortOption
import com.example.domain.PasswordAnalysisResult
import com.example.domain.ProbeSample
import com.example.domain.ScanHistoryRecord
import com.example.domain.SecurityFilter
import com.example.domain.SecurityReportRecord
import com.example.domain.WifiBand
import com.example.domain.WifiNetwork
import com.example.permissions.WifiPermissionSnapshot
import com.example.presentation.i18n.AppLanguage

data class WifiToolsUiState(
    val language: AppLanguage = AppLanguage.ARABIC,
    val permissions: WifiPermissionSnapshot = WifiPermissionSnapshot(
        hasFineLocation = false,
        hasCoarseLocation = false,
        hasNearbyWifiDevices = false,
        isLocationServiceEnabled = false,
        isWifiEnabled = false
    ),
    val activeConnection: ActiveConnectionInfo = ActiveConnectionInfo(),
    val discoveredNetworks: List<WifiNetwork> = emptyList(),
    val isScanning: Boolean = false,
    val lastScanTimestampMs: Long? = null,
    val scanStatusMessage: String = "Tap Scan to discover nearby Wi-Fi access points.",

    // Scanner Filters & Sorting
    val searchQuery: String = "",
    val selectedBandFilter: WifiBand? = null,
    val selectedSecurityFilter: SecurityFilter = SecurityFilter.ALL,
    val selectedSortOption: NetworkSortOption = NetworkSortOption.SIGNAL_DESC,
    val selectedNetworkForDetail: WifiNetwork? = null,
    val selectedNetworkAssessment: NetworkSecurityAssessment? = null,

    // Analyzer
    val selectedCongestionBand: WifiBand = WifiBand.BAND_2_4_GHZ,
    val channelCongestion: List<ChannelCongestion> = emptyList(),
    val recommendedChannel: Int? = null,
    val isRunningConnectivityCheck: Boolean = false,
    val connectivityCheckResult: ConnectivityCheckResult? = null,

    // Security & Authorized Testing
    val isAuthorizationConfirmed: Boolean = false,
    val isRunningAuthorizedAudit: Boolean = false,
    val authorizedAuditReport: AuthorizedAuditReport? = null,
    val completedHardeningIds: Set<String> = emptySet(),
    val passwordInput: String = "",
    val passwordAnalysis: PasswordAnalysisResult? = null,

    // Router Diagnostics
    val targetHostInput: String = "1.1.1.1",
    val targetPortInput: String = "443",
    val probeCountInput: Int = 6,
    val isRunningLatencyTest: Boolean = false,
    val liveProbeSamples: List<ProbeSample> = emptyList(),
    val latencyDiagnosticResult: LatencyDiagnosticResult? = null,

    // Reports & Local History
    val scanHistory: List<ScanHistoryRecord> = emptyList(),
    val securityReports: List<SecurityReportRecord> = emptyList(),
    val selectedReportForPreview: SecurityReportRecord? = null,

    // Transient UI Feedback
    val toastMessage: String? = null
) {
    val filteredNetworks: List<WifiNetwork>
        get() {
            val query = searchQuery.trim().lowercase()
            val filtered = discoveredNetworks.filter { net ->
                val matchesQuery = query.isEmpty() ||
                    net.ssid.lowercase().contains(query) ||
                    net.bssid.lowercase().contains(query) ||
                    net.securityProtocol.label.lowercase().contains(query)
                val matchesBand = selectedBandFilter == null || net.band == selectedBandFilter
                val matchesSecurity = when (selectedSecurityFilter) {
                    SecurityFilter.ALL -> true
                    SecurityFilter.WEAK_OR_OPEN -> net.securityScore < 65 || net.hasWps
                    SecurityFilter.WPA2 -> net.securityProtocol.name.contains("WPA2")
                    SecurityFilter.WPA3 -> net.securityProtocol.name.contains("WPA3")
                }
                matchesQuery && matchesBand && matchesSecurity
            }
            return when (selectedSortOption) {
                NetworkSortOption.SIGNAL_DESC -> filtered.sortedByDescending { it.rssi }
                NetworkSortOption.SECURITY_RISK -> filtered.sortedBy { it.securityScore }
                NetworkSortOption.CHANNEL_ASC -> filtered.sortedWith(compareBy<WifiNetwork> { it.channel }.thenByDescending { it.rssi })
                NetworkSortOption.SSID_ASC -> filtered.sortedBy { it.ssid.lowercase() }
            }
        }
}
