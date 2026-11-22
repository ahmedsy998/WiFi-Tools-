package com.example.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.NetworkSecurityAssessment
import com.example.domain.NetworkSortOption
import com.example.domain.SecurityFilter
import com.example.domain.SecurityProtocol
import com.example.domain.WifiBand
import com.example.domain.WifiNetwork
import com.example.presentation.components.AnimatedSignalBars
import com.example.presentation.components.CyberBadge
import com.example.presentation.components.CyberKeyValueRow
import com.example.presentation.components.CyberPanel
import com.example.presentation.components.PermissionStatusBanner
import com.example.presentation.components.RiskBadge
import com.example.presentation.i18n.LocalAppStrings
import com.example.presentation.viewmodel.WifiToolsUiState
import com.example.ui.theme.CrimsonBorder
import com.example.ui.theme.CrimsonBorderBright
import com.example.ui.theme.CrimsonPanel
import com.example.ui.theme.CrimsonPanelHigh
import com.example.ui.theme.CrimsonTextMuted
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberAmberContainer
import com.example.ui.theme.CyberCritical
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanContainer
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.NeonRed
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    state: WifiToolsUiState,
    onTriggerScan: () -> Unit,
    onSearchChange: (String) -> Unit,
    onBandFilterSelect: (WifiBand?) -> Unit,
    onSecurityFilterSelect: (SecurityFilter) -> Unit,
    onSortOptionSelect: (NetworkSortOption) -> Unit,
    onInspectNetwork: (WifiNetwork?) -> Unit,
    onSaveSnapshot: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val networks = state.filteredNetworks

    Column(modifier = modifier.fillMaxSize()) {
        if (state.isScanning) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = NeonRed,
                trackColor = CrimsonPanel
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PermissionStatusBanner(
                    snapshot = state.permissions,
                    onRequestPermissions = onRequestPermissions,
                    onOpenLocationSettings = onOpenLocationSettings,
                    onOpenWifiSettings = onOpenWifiSettings,
                    onOpenAppSettings = onOpenAppSettings
                )
            }

            // Control Bar: Scan + Save + Sort
            item {
                CyberPanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${networks.size} ${strings.scannerCountFormat} ${state.discoveredNetworks.size}",
                                style = MaterialTheme.typography.titleSmall,
                                color = PureWhite
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = state.scanStatusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = CrimsonTextMuted
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onTriggerScan,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                            modifier = Modifier.testTag("scanner_refresh_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = strings.btnScan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.btnScan)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text(strings.searchPlaceholder, color = CrimsonTextMuted) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = strings.searchPlaceholder, tint = NeonRed) },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = CrimsonTextMuted)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonRed,
                            unfocusedBorderColor = CrimsonBorder,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            cursorColor = NeonRed
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scanner_search_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Band Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CyberFilterChip(
                            selected = state.selectedBandFilter == null,
                            label = strings.filterAllBands,
                            onClick = { onBandFilterSelect(null) }
                        )
                        listOf(WifiBand.BAND_2_4_GHZ, WifiBand.BAND_5_GHZ, WifiBand.BAND_6_GHZ).forEach { band ->
                            CyberFilterChip(
                                selected = state.selectedBandFilter == band,
                                label = band.label,
                                onClick = { onBandFilterSelect(if (state.selectedBandFilter == band) null else band) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Security Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SecurityFilter.entries.forEach { secFilter ->
                            CyberFilterChip(
                                selected = state.selectedSecurityFilter == secFilter,
                                label = strings.securityFilterLabel(secFilter),
                                onClick = { onSecurityFilterSelect(secFilter) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.layout.Box {
                            OutlinedButton(
                                onClick = { sortMenuExpanded = true },
                                border = BorderStroke(1.dp, CrimsonBorderBright),
                                modifier = Modifier.testTag("scanner_sort_button")
                            ) {
                                Icon(Icons.Rounded.Sort, contentDescription = strings.sortPrefix, tint = NeonRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${strings.sortPrefix} ${strings.sortOptionLabel(state.selectedSortOption)}", color = PureWhite)
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                NetworkSortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(strings.sortOptionLabel(option)) },
                                        onClick = {
                                            onSortOptionSelect(option)
                                            sortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = onSaveSnapshot,
                            border = BorderStroke(1.dp, CrimsonBorderBright),
                            modifier = Modifier.testTag("scanner_save_history_button")
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = strings.btnLogScan, tint = PureWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.btnLogScan, color = PureWhite)
                        }
                    }
                }
            }

            if (networks.isEmpty()) {
                item {
                    CyberPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Radar,
                                contentDescription = strings.emptyNoApsTitle,
                                tint = CrimsonTextMuted,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (state.discoveredNetworks.isEmpty()) strings.emptyNoApsTitle else strings.emptyNoMatchTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = PureWhite
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (state.discoveredNetworks.isEmpty()) strings.emptyNoApsBody else strings.emptyNoMatchBody,
                                style = MaterialTheme.typography.bodySmall,
                                color = CrimsonTextMuted
                            )
                        }
                    }
                }
            } else {
                items(networks, key = { it.bssid }) { network ->
                    WifiNetworkCard(
                        network = network,
                        onClick = { onInspectNetwork(network) }
                    )
                }
            }
        }
    }

    val inspected = state.selectedNetworkForDetail
    val assessment = state.selectedNetworkAssessment
    if (inspected != null && assessment != null) {
        ModalBottomSheet(
            onDismissRequest = { onInspectNetwork(null) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = PureBlack,
            contentColor = PureWhite
        ) {
            NetworkDetailSheetContent(
                network = inspected,
                assessment = assessment,
                onDismiss = { onInspectNetwork(null) }
            )
        }
    }
}

@Composable
private fun CyberFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = NeonRed,
            selectedLabelColor = PureWhite,
            containerColor = CrimsonPanelHigh,
            labelColor = CrimsonTextMuted
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = CrimsonBorder,
            selectedBorderColor = NeonRed
        )
    )
}

@Composable
private fun WifiNetworkCard(
    network: WifiNetwork,
    onClick: () -> Unit
) {
    val isInsecure = network.securityProtocol == SecurityProtocol.OPEN ||
        network.securityProtocol == SecurityProtocol.WEP_LEGACY ||
        network.securityProtocol == SecurityProtocol.WPA_LEGACY

    CyberPanel(
        highlighted = isInsecure,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("network_card_${network.bssid}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (network.securityProtocol == SecurityProtocol.OPEN) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                    contentDescription = network.securityProtocol.label,
                    tint = if (isInsecure) CyberCritical else CyberGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = network.ssid,
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = network.bssid,
                        style = MonoCodeStyle.copy(textDirection = TextDirection.Ltr),
                        color = CrimsonTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${network.rssi} dBm",
                        style = MonoMetricMedium.copy(textDirection = TextDirection.Ltr),
                        color = PureWhite
                    )
                    Text(
                        text = "${network.signalPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrimsonTextMuted
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                AnimatedSignalBars(rssiDbm = network.rssi)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RiskBadge(riskLevel = network.riskLevel)
            CyberBadge(
                text = network.securityProtocol.label,
                color = PureWhite,
                backgroundColor = CrimsonPanelHigh
            )
            CyberBadge(
                text = "${network.band.label} • CH ${network.channel}",
                color = CyberCyan,
                backgroundColor = CyberCyanContainer
            )
            if (network.hasWps) {
                CyberBadge(
                    text = "WPS",
                    color = CyberAmber,
                    backgroundColor = CyberAmberContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${network.frequencyMhz} MHz • ${network.channelWidth} • ${network.wifiStandard} • ${network.securityScore}/100",
            style = MaterialTheme.typography.labelSmall.copy(textDirection = TextDirection.Ltr),
            color = CrimsonTextMuted
        )
    }
}

@Composable
private fun NetworkDetailSheetContent(
    network: WifiNetwork,
    assessment: NetworkSecurityAssessment,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = network.ssid,
                        style = MaterialTheme.typography.headlineMedium,
                        color = PureWhite
                    )
                    Text(
                        text = "BSSID ${network.bssid}",
                        style = MonoCodeStyle.copy(textDirection = TextDirection.Ltr),
                        color = CrimsonTextMuted
                    )
                }
                RiskBadge(riskLevel = assessment.riskLevel)
            }
        }

        item {
            CyberPanel {
                CyberKeyValueRow(strings.detailSecurityScore, "${assessment.score} / 100", forceLtrValue = true)
                CyberKeyValueRow(strings.detailProtocol, network.securityProtocol.label, forceLtrValue = true)
                CyberKeyValueRow(strings.detailFreqBand, "${network.frequencyMhz} MHz (${network.band.label})", forceLtrValue = true)
                CyberKeyValueRow(strings.detailChannelWidth, "Ch ${network.channel} • ${network.channelWidth}", forceLtrValue = true)
                CyberKeyValueRow(strings.detailWifiStandard, network.wifiStandard, forceLtrValue = true)
                CyberKeyValueRow(strings.detailSignalLevel, "${network.rssi} dBm (${network.signalPercent}%)", forceLtrValue = true)
                CyberKeyValueRow(
                    strings.detailWpsAdvertised,
                    if (network.hasWps) strings.wpsYesRisk else strings.wpsNo,
                    if (network.hasWps) CyberCritical else CyberGreen
                )
                CyberKeyValueRow(strings.detailRawCaps, network.capabilities.ifBlank { strings.valueNone }, forceLtrValue = true)
            }
        }

        item {
            Text(
                text = strings.sectionSecurityFindings,
                style = MaterialTheme.typography.titleSmall,
                color = NeonRed
            )
        }

        items(assessment.findings) { finding ->
            CyberPanel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = finding.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite,
                        modifier = Modifier.weight(1f)
                    )
                    RiskBadge(riskLevel = finding.severity)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(finding.description, style = MaterialTheme.typography.bodySmall, color = CrimsonTextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                Text("${strings.labelRecommendation} ${finding.recommendation}", style = MaterialTheme.typography.bodySmall, color = PureWhite)
            }
        }

        item {
            HorizontalDivider(color = CrimsonBorder)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(strings.btnCloseInspection)
            }
        }
    }
}
