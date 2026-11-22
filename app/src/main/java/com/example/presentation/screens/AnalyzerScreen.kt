package com.example.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiFind
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.WifiBand
import com.example.presentation.components.ChannelCongestionChart
import com.example.presentation.components.CyberBadge
import com.example.presentation.components.CyberKeyValueRow
import com.example.presentation.components.CyberPanel
import com.example.presentation.components.CyberSectionHeader
import com.example.presentation.components.SignalArcGauge
import com.example.presentation.viewmodel.WifiToolsUiState
import com.example.ui.theme.CrimsonBorder
import com.example.ui.theme.CrimsonPanelHigh
import com.example.ui.theme.CrimsonTextMuted
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCritical
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanContainer
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberGreenContainer
import com.example.ui.theme.NeonRed
import com.example.ui.theme.PureWhite
import com.example.utils.SignalQualityUtils

@Composable
fun AnalyzerScreen(
    state: WifiToolsUiState,
    onSelectCongestionBand: (WifiBand) -> Unit,
    onRunConnectivityCheck: () -> Unit,
    onRefreshConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val conn = state.activeConnection

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Live Signal Quality & Active Link Telemetry
        item {
            CyberSectionHeader(
                title = "Live Signal & Link Quality",
                subtitle = "ConnectivityManager & WifiInfo telemetry",
                trailing = {
                    Button(
                        onClick = onRefreshConnection,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPanelHigh),
                        modifier = Modifier.testTag("analyzer_refresh_button")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh telemetry", tint = NeonRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync", color = PureWhite)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            CyberPanel(highlighted = conn.isConnectedToWifi) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SignalArcGauge(
                        rssiDbm = conn.rssiDbm,
                        signalPercent = conn.signalPercent,
                        subtitle = if (conn.isConnectedToWifi) SignalQualityUtils.rssiQualityLabel(conn.rssiDbm) else "OFFLINE"
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = conn.ssid,
                            style = MaterialTheme.typography.titleLarge,
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (conn.isConnectedToWifi) {
                                "${conn.band.label} • Ch ${conn.channel} (${conn.frequencyMhz} MHz)"
                            } else {
                                "No active Wi-Fi association"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberCyan
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Link: ${conn.linkSpeedMbps} Mbps",
                            style = MaterialTheme.typography.labelMedium,
                            color = PureWhite
                        )
                        if (conn.txLinkSpeedMbps != null || conn.rxLinkSpeedMbps != null) {
                            Text(
                                text = "TX ${conn.txLinkSpeedMbps ?: "-"} / RX ${conn.rxLinkSpeedMbps ?: "-"} Mbps",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrimsonTextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = conn.wifiStandard,
                            style = MaterialTheme.typography.labelSmall,
                            color = CrimsonTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                CyberKeyValueRow("BSSID", conn.bssid)
                CyberKeyValueRow("Interface / MTU", "${conn.interfaceName ?: "wlan"} • MTU ${if (conn.mtu > 0) conn.mtu else "Default"}")
                CyberKeyValueRow("Local IP / Prefix", "${conn.localIpAddresses.firstOrNull() ?: "Unavailable"} ${conn.subnetMaskOrPrefix ?: ""}")
                CyberKeyValueRow("Metered Network", if (conn.isMetered) "Yes (Metered)" else "No (Unmetered)")
                if (!conn.limitationNote.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = conn.limitationNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberAmber
                    )
                }
            }
        }

        // Frequency Band Breakdown
        item {
            val count24 = state.discoveredNetworks.count { it.band == WifiBand.BAND_2_4_GHZ }
            val count5 = state.discoveredNetworks.count { it.band == WifiBand.BAND_5_GHZ }
            val count6 = state.discoveredNetworks.count { it.band == WifiBand.BAND_6_GHZ }

            CyberSectionHeader(
                title = "Channel Congestion Spectrum",
                subtitle = "2.4 GHz ($count24 APs) • 5 GHz ($count5 APs) • 6 GHz ($count6 APs)"
            )
            Spacer(modifier = Modifier.height(8.dp))
            CyberPanel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(WifiBand.BAND_2_4_GHZ, WifiBand.BAND_5_GHZ, WifiBand.BAND_6_GHZ).forEach { band ->
                        FilterChip(
                            selected = state.selectedCongestionBand == band,
                            onClick = { onSelectCongestionBand(band) },
                            label = { Text(band.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonRed,
                                selectedLabelColor = PureWhite,
                                containerColor = CrimsonPanelHigh,
                                labelColor = CrimsonTextMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = state.selectedCongestionBand == band,
                                borderColor = CrimsonBorder,
                                selectedBorderColor = NeonRed
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (state.discoveredNetworks.none { it.band == state.selectedCongestionBand }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WifiFind, contentDescription = "No band data", tint = CrimsonTextMuted, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No access points detected on ${state.selectedCongestionBand.label} in the latest scan. Run a Wi-Fi scan to plot real channel load.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonTextMuted
                        )
                    }
                } else {
                    ChannelCongestionChart(channels = state.channelCongestion)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.selectedCongestionBand == WifiBand.BAND_2_4_GHZ) {
                                "Non-overlapping optimal channels: 1, 6, 11"
                            } else {
                                "Channels scored by AP density & RSSI overlap"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonTextMuted
                        )
                        state.recommendedChannel?.let { best ->
                            CyberBadge(
                                text = "BEST CH $best",
                                color = CyberGreen,
                                backgroundColor = CyberGreenContainer
                            )
                        }
                    }
                }
            }
        }

        // Connectivity Diagnostics
        item {
            CyberSectionHeader(
                title = "Connectivity Diagnostics",
                subtitle = "Live DNS resolution, gateway reachability & upstream TCP probe"
            )
            Spacer(modifier = Modifier.height(8.dp))
            CyberPanel {
                Button(
                    onClick = onRunConnectivityCheck,
                    enabled = !state.isRunningConnectivityCheck,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("run_connectivity_diagnostics_button")
                ) {
                    if (state.isRunningConnectivityCheck) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = PureWhite,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Running Live Probes…")
                    } else {
                        Icon(Icons.Rounded.NetworkCheck, contentDescription = "Run Diagnostics", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Connectivity Diagnostics")
                    }
                }

                val check = state.connectivityCheckResult
                if (check != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CyberBadge(
                        text = check.statusMessage,
                        color = if (check.tcpInternetReachable) CyberGreen else CyberAmber,
                        backgroundColor = if (check.tcpInternetReachable) CyberGreenContainer else CyberCyanContainer
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CyberKeyValueRow(
                        "DNS Resolution (dns.google)",
                        check.dnsResolutionMs?.let { "$it ms (${check.resolvedTestHost})" } ?: "Failed",
                        valueColor = if (check.dnsResolutionMs != null) CyberGreen else CyberCritical
                    )
                    CyberKeyValueRow(
                        "Gateway Reachability",
                        if (check.gatewayReachable) "Reachable (${check.gatewayLatencyMs} ms)" else "No Reply / Unavailable",
                        valueColor = if (check.gatewayReachable) CyberGreen else CyberAmber
                    )
                    CyberKeyValueRow(
                        "Upstream TCP (1.1.1.1:443)",
                        if (check.tcpInternetReachable) "Connected (${check.tcpInternetLatencyMs} ms)" else "Unreachable",
                        valueColor = if (check.tcpInternetReachable) CyberGreen else CyberCritical
                    )
                    CyberKeyValueRow(
                        "Captive Portal",
                        if (check.isCaptivePortal) "Detected" else "None",
                        valueColor = if (check.isCaptivePortal) CyberCritical else CyberGreen
                    )
                }
            }
        }
    }
}
