package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppContainer
import com.example.domain.NetworkSortOption
import com.example.domain.SecurityFilter
import com.example.domain.SecurityReportRecord
import com.example.domain.WifiBand
import com.example.domain.WifiNetwork
import com.example.utils.AppLogger
import com.example.utils.WifiChannelUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WifiToolsViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val _uiState = MutableStateFlow(WifiToolsUiState())
    val uiState: StateFlow<WifiToolsUiState> = _uiState.asStateFlow()

    init {
        refreshPermissionsAndConnection()
        observeConnection()
        observeScanBroadcasts()
        observeLocalHistory()
        evaluatePassword("")
    }

    fun refreshPermissionsAndConnection() {
        val perms = container.permissionManager.getPermissionSnapshot()
        val conn = container.activeConnectionMonitor.readCurrentConnectionSnapshot()
        val cached = if (perms.hasFineLocation) container.wifiScannerService.readCachedScanResults() else emptyList()

        _uiState.update { state ->
            val updatedNetworks = if (cached.isNotEmpty()) cached else state.discoveredNetworks
            val congestion = WifiChannelUtils.analyzeChannelCongestion(updatedNetworks, state.selectedCongestionBand)
            val bestCh = WifiChannelUtils.recommendBestChannel(congestion, state.selectedCongestionBand)
            val suggestedTarget = if (state.targetHostInput == "1.1.1.1" && !conn.gatewayIp.isNullOrBlank()) {
                conn.gatewayIp
            } else {
                state.targetHostInput
            }
            state.copy(
                permissions = perms,
                activeConnection = conn,
                discoveredNetworks = updatedNetworks,
                channelCongestion = congestion,
                recommendedChannel = bestCh,
                targetHostInput = suggestedTarget
            )
        }
    }

    private fun observeConnection() {
        viewModelScope.launch {
            container.activeConnectionMonitor.observeActiveConnection()
                .catch { e -> AppLogger.e("ViewModel", "Connection observer error", e) }
                .collect { conn ->
                    _uiState.update { state ->
                        val updatedHost = if ((state.targetHostInput.isBlank() || state.targetHostInput == "1.1.1.1") && !conn.gatewayIp.isNullOrBlank()) {
                            conn.gatewayIp
                        } else {
                            state.targetHostInput
                        }
                        state.copy(
                            activeConnection = conn,
                            permissions = container.permissionManager.getPermissionSnapshot(),
                            targetHostInput = updatedHost
                        )
                    }
                }
        }
    }

    private fun observeScanBroadcasts() {
        viewModelScope.launch {
            container.wifiScannerService.observeScanBroadcasts()
                .catch { e -> AppLogger.e("ViewModel", "Scan broadcast observer error", e) }
                .collect { event ->
                    _uiState.update { state ->
                        val congestion = WifiChannelUtils.analyzeChannelCongestion(
                            event.networks,
                            state.selectedCongestionBand
                        )
                        val bestCh = WifiChannelUtils.recommendBestChannel(
                            congestion,
                            state.selectedCongestionBand
                        )
                        state.copy(
                            discoveredNetworks = event.networks,
                            isScanning = false,
                            lastScanTimestampMs = event.timestampMs,
                            scanStatusMessage = event.statusNote,
                            channelCongestion = congestion,
                            recommendedChannel = bestCh
                        )
                    }
                }
        }
    }

    private fun observeLocalHistory() {
        viewModelScope.launch {
            container.historyRepository.scanHistory
                .catch { e -> AppLogger.e("ViewModel", "Scan history error", e) }
                .collect { history ->
                    _uiState.update { it.copy(scanHistory = history) }
                }
        }
        viewModelScope.launch {
            container.historyRepository.securityReports
                .catch { e -> AppLogger.e("ViewModel", "Security reports error", e) }
                .collect { reports ->
                    _uiState.update { it.copy(securityReports = reports) }
                }
        }
    }

    fun startWifiScan() {
        refreshPermissionsAndConnection()
        val (started, note) = container.wifiScannerService.triggerActiveScan()
        val cached = container.wifiScannerService.readCachedScanResults()
        _uiState.update { state ->
            val nets = if (cached.isNotEmpty()) cached else state.discoveredNetworks
            val congestion = WifiChannelUtils.analyzeChannelCongestion(nets, state.selectedCongestionBand)
            val bestCh = WifiChannelUtils.recommendBestChannel(congestion, state.selectedCongestionBand)
            state.copy(
                isScanning = started,
                discoveredNetworks = nets,
                lastScanTimestampMs = if (nets.isNotEmpty()) System.currentTimeMillis() else state.lastScanTimestampMs,
                scanStatusMessage = note,
                channelCongestion = congestion,
                recommendedChannel = bestCh
            )
        }

        if (started) {
            // Safeguard timeout in case OS doesn't fire broadcast within 6s
            viewModelScope.launch {
                delay(6000)
                if (_uiState.value.isScanning) {
                    val refreshed = container.wifiScannerService.readCachedScanResults()
                    _uiState.update { state ->
                        val nets = if (refreshed.isNotEmpty()) refreshed else state.discoveredNetworks
                        val congestion = WifiChannelUtils.analyzeChannelCongestion(nets, state.selectedCongestionBand)
                        state.copy(
                            isScanning = false,
                            discoveredNetworks = nets,
                            channelCongestion = congestion,
                            recommendedChannel = WifiChannelUtils.recommendBestChannel(congestion, state.selectedCongestionBand),
                            scanStatusMessage = "Scan cycle complete (${nets.size} APs reported by Android)."
                        )
                    }
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectBandFilter(band: WifiBand?) {
        _uiState.update { it.copy(selectedBandFilter = band) }
    }

    fun selectSecurityFilter(filter: SecurityFilter) {
        _uiState.update { it.copy(selectedSecurityFilter = filter) }
    }

    fun selectSortOption(option: NetworkSortOption) {
        _uiState.update { it.copy(selectedSortOption = option) }
    }

    fun inspectNetwork(network: WifiNetwork?) {
        val assessment = network?.let { container.securityAnalyzer.assessNetwork(it) }
        _uiState.update {
            it.copy(
                selectedNetworkForDetail = network,
                selectedNetworkAssessment = assessment
            )
        }
    }

    fun selectCongestionBand(band: WifiBand) {
        _uiState.update { state ->
            val congestion = WifiChannelUtils.analyzeChannelCongestion(state.discoveredNetworks, band)
            val bestCh = WifiChannelUtils.recommendBestChannel(congestion, band)
            state.copy(
                selectedCongestionBand = band,
                channelCongestion = congestion,
                recommendedChannel = bestCh
            )
        }
    }

    fun runConnectivityDiagnostics() {
        if (_uiState.value.isRunningConnectivityCheck) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRunningConnectivityCheck = true) }
            val result = container.routerDiagnosticsEngine.runConnectivityDiagnostics(
                _uiState.value.activeConnection
            )
            _uiState.update {
                it.copy(
                    isRunningConnectivityCheck = false,
                    connectivityCheckResult = result
                )
            }
        }
    }

    fun setAuthorizationConfirmed(confirmed: Boolean) {
        _uiState.update { it.copy(isAuthorizationConfirmed = confirmed) }
    }

    fun runAuthorizedConfigurationAudit() {
        val state = _uiState.value
        if (!state.isAuthorizationConfirmed) {
            showToast("Confirm network ownership / authorization before running active audits.")
            return
        }
        if (state.isRunningAuthorizedAudit) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRunningAuthorizedAudit = true) }
            val conn = container.activeConnectionMonitor.readCurrentConnectionSnapshot()
            val matched = state.discoveredNetworks.firstOrNull { net ->
                (conn.bssid != "N/A" && net.bssid.equals(conn.bssid, ignoreCase = true)) ||
                    (conn.isConnectedToWifi && net.ssid == conn.ssid)
            }
            val audit = container.authorizedAuditEngine.runAuthorizedConfigurationAudit(conn, matched)
            _uiState.update {
                it.copy(
                    activeConnection = conn,
                    isRunningAuthorizedAudit = false,
                    authorizedAuditReport = audit,
                    toastMessage = "Authorized configuration audit complete (Score: ${audit.overallScore}/100)."
                )
            }
        }
    }

    fun toggleHardeningChecklistItem(id: String) {
        _uiState.update { state ->
            val next = if (id in state.completedHardeningIds) {
                state.completedHardeningIds - id
            } else {
                state.completedHardeningIds + id
            }
            state.copy(completedHardeningIds = next)
        }
    }

    fun evaluatePassword(password: String) {
        val analysis = container.passwordValidator.analyze(password)
        _uiState.update {
            it.copy(
                passwordInput = password,
                passwordAnalysis = analysis
            )
        }
    }

    fun generateLocalPassphrase() {
        val generated = container.passwordValidator.generateSecurePassphrase(20)
        evaluatePassword(generated)
        showToast("Generated 20-character high-entropy passphrase locally.")
    }

    fun updateTargetHost(host: String) {
        _uiState.update { it.copy(targetHostInput = host) }
    }

    fun updateTargetPort(port: String) {
        val filtered = port.filter { it.isDigit() }.take(5)
        _uiState.update { it.copy(targetPortInput = filtered) }
    }

    fun updateProbeCount(count: Int) {
        _uiState.update { it.copy(probeCountInput = count.coerceIn(2, 12)) }
    }

    fun runRouterLatencyTest() {
        val state = _uiState.value
        if (state.isRunningLatencyTest) return
        val port = state.targetPortInput.toIntOrNull() ?: 443

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRunningLatencyTest = true,
                    liveProbeSamples = emptyList()
                )
            }
            val result = container.routerDiagnosticsEngine.runLatencyAndLossTest(
                targetHost = state.targetHostInput,
                port = port,
                probeCount = state.probeCountInput,
                onProgress = { sample ->
                    _uiState.update { curr ->
                        curr.copy(liveProbeSamples = curr.liveProbeSamples + sample)
                    }
                }
            )
            _uiState.update {
                it.copy(
                    isRunningLatencyTest = false,
                    latencyDiagnosticResult = result
                )
            }
        }
    }

    fun saveScanSnapshotToHistory() {
        val state = _uiState.value
        if (state.discoveredNetworks.isEmpty()) {
            showToast("No scan results available yet. Run a Wi-Fi scan first.")
            return
        }
        viewModelScope.launch {
            container.historyRepository.saveScanSnapshot(
                networks = state.discoveredNetworks,
                connectedSsid = state.activeConnection.takeIf { it.isConnectedToWifi }?.ssid
            )
            showToast("Saved ${state.discoveredNetworks.size} networks to local scan history.")
        }
    }

    fun generateAndSaveAssessmentReport() {
        val state = _uiState.value
        viewModelScope.launch {
            val report = container.reportGenerator.buildComprehensiveReport(
                connection = state.activeConnection,
                networks = state.discoveredNetworks,
                auditReport = state.authorizedAuditReport,
                latencyResult = state.latencyDiagnosticResult
            )
            val insertedId = container.historyRepository.saveSecurityReport(report)
            val savedReport = report.copy(id = insertedId)
            _uiState.update {
                it.copy(
                    selectedReportForPreview = savedReport,
                    toastMessage = "Assessment report saved locally."
                )
            }
        }
    }

    fun previewReport(report: SecurityReportRecord?) {
        _uiState.update { it.copy(selectedReportForPreview = report) }
    }

    fun deleteScanHistory(id: Long) {
        viewModelScope.launch {
            container.historyRepository.deleteScanHistory(id)
            showToast("Scan snapshot deleted.")
        }
    }

    fun deleteSecurityReport(id: Long) {
        viewModelScope.launch {
            container.historyRepository.deleteSecurityReport(id)
            if (_uiState.value.selectedReportForPreview?.id == id) {
                _uiState.update { it.copy(selectedReportForPreview = null) }
            }
            showToast("Assessment report deleted.")
        }
    }

    fun purgeAllLocalData() {
        viewModelScope.launch {
            container.historyRepository.deleteAllData()
            _uiState.update {
                it.copy(
                    selectedReportForPreview = null,
                    toastMessage = "All local scan history and reports permanently deleted."
                )
            }
        }
    }

    fun openAppSettings() = container.permissionManager.openAppSettings()
    fun openLocationSettings() = container.permissionManager.openLocationSettings()
    fun openWifiSettings() = container.permissionManager.openWifiSettings()
    fun requiredRuntimePermissions(): Array<String> = container.permissionManager.requiredRuntimePermissions()

    fun consumeToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    private fun showToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WifiToolsViewModel(container) as T
                }
            }
    }
}
