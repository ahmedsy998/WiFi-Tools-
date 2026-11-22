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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.example.domain.WifiBand
import com.example.presentation.components.ChannelCongestionChart
import com.example.presentation.components.CyberBadge
import com.example.presentation.components.CyberKeyValueRow
import com.example.presentation.components.CyberPanel
import com.example.presentation.components.CyberSectionHeader
import com.example.presentation.components.SignalArcGauge
import com.example.presentation.i18n.LocalAppStrings
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

@Composable
fun AnalyzerScreen(
    state: WifiToolsUiState,
    onSelectCongestionBand: (WifiBand) -> Unit,
    onRunConnectivityCheck: () -> Unit,
    onRefreshConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val conn = state.activeConnection

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CyberSectionHeader(
                title = strings.sectionLiveSignal,
                subtitle = strings.subtitleLiveSignal,
                trailing = {
                    Button(
                        onClick = onRefreshConnection,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPanelHigh),
                        modifier = Modifier.testTag("analyzer_refresh_button")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = strings.btnSync, tint = NeonRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.btnSync, color = PureWhite)
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
                        subtitle = if (conn.isConnectedToWifi) strings.signalQualityLabel(conn.rssiDbm) else strings.statusOffline
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (conn.isConnectedToWifi) conn.ssid else strings.statusOffline,
                            style = MaterialTheme.typography.titleLarge,
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (conn.isConnectedToWifi) {
                                "${conn.band.label} • Ch ${conn.channel} (${conn.frequencyMhz} MHz)"
                            } else {
                                strings.noActiveWifi
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberCyan
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${strings.labelLink} ${conn.linkSpeedMbps} Mbps",
                            style = MaterialTheme.typography.labelMedium,
                            color = PureWhite
                        )
                        if (conn.txLinkSpeedMbps != null || conn.rxLinkSpeedMbps != null) {
                            Text(
                                text = "TX ${conn.txLinkSpeedMbps ?: "-"} / RX ${conn.rxLinkSpeedMbps ?: "-"} Mbps",
                                style = MaterialTheme.typography.labelSmall.copy(textDirection = TextDirection.Ltr),
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
                CyberKeyValueRow("BSSID", conn.bssid, forceLtrValue = true)
                CyberKeyValueRow(
                    strings.labelInterfaceMtu,
                    "${conn.interfaceName ?: "wlan"} • MTU ${if (conn.mtu > 0) conn.mtu else "Auto"}",
                    forceLtrValue = true
                )
                CyberKeyValueRow(
                    strings.labelLocalIpPrefix,
                    "${conn.localIpAddresses.firstOrNull() ?: strings.valueUnavailable} ${conn.subnetMaskOrPrefix ?: ""}".trim(),
                    forceLtrValue = conn.localIpAddresses.isNotEmpty()
                )
                CyberKeyValueRow(
                    strings.labelMetered,
                    if (conn.isMetered) strings.valueMeteredYes else strings.valueMeteredNo
                )
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
                title = strings.sectionCongestion,
                subtitle = "2.4 GHz ($count24) • 5 GHz ($count5) • 6 GHz ($count6)"
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
                        Icon(Icons.Rounded.WifiFind, contentDescription = strings.sectionCongestion, tint = CrimsonTextMuted, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.noBandDataHint,
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
                                strings.nonOverlappingHint
                            } else {
                                strings.scoredByDensityHint
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonTextMuted,
                            modifier = Modifier.weight(1f)
                        )
                        state.recommendedChannel?.let { best ->
                            Spacer(modifier = Modifier.width(8.dp))
                            CyberBadge(
                                text = "${strings.badgeBestChannel} $best",
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
                title = strings.sectionConnectivityDiag,
                subtitle = strings.subtitleConnectivityDiag
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
                        Text(strings.btnRunningProbes)
                    } else {
                        Icon(Icons.Rounded.NetworkCheck, contentDescription = strings.btnRunConnectivity, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.btnRunConnectivity)
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
                        strings.labelDnsResolution,
                        check.dnsResolutionMs?.let { "$it ms (${check.resolvedTestHost})" } ?: strings.valueFailed,
                        valueColor = if (check.dnsResolutionMs != null) CyberGreen else CyberCritical,
                        forceLtrValue = check.dnsResolutionMs != null
                    )
                    CyberKeyValueRow(
                        strings.labelGatewayReach,
                        if (check.gatewayReachable) "${strings.valueReachable} (${check.gatewayLatencyMs} ms)" else strings.valueNoReply,
                        valueColor = if (check.gatewayReachable) CyberGreen else CyberAmber
                    )
                    CyberKeyValueRow(
                        strings.labelUpstreamTcp,
                        if (check.tcpInternetReachable) "${strings.valueConnected} (${check.tcpInternetLatencyMs} ms)" else strings.valueUnreachable,
                        valueColor = if (check.tcpInternetReachable) CyberGreen else CyberCritical
                    )
                    CyberKeyValueRow(
                        strings.labelCaptivePortal,
                        if (check.isCaptivePortal) strings.valueDetected else strings.valueNone,
                        valueColor = if (check.isCaptivePortal) CyberCritical else CyberGreen
                    )
                }
            }
        }
    }
}
