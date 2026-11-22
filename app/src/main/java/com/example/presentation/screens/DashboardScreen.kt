package com.example.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.SecurityProtocol
import com.example.presentation.components.AndroidLimitationDisclosureCard
import com.example.presentation.components.AnimatedRadarSweep
import com.example.presentation.components.CyberBadge
import com.example.presentation.components.CyberKeyValueRow
import com.example.presentation.components.CyberMetricTile
import com.example.presentation.components.CyberPanel
import com.example.presentation.components.CyberSectionHeader
import com.example.presentation.components.PermissionStatusBanner
import com.example.presentation.components.PulsingLiveDot
import com.example.presentation.i18n.LocalAppStrings
import com.example.presentation.viewmodel.WifiToolsUiState
import com.example.ui.theme.CrimsonBorderBright
import com.example.ui.theme.CrimsonTextMuted
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberAmberContainer
import com.example.ui.theme.CyberCritical
import com.example.ui.theme.CyberCriticalContainer
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberGreenContainer
import com.example.ui.theme.NeonRed
import com.example.ui.theme.PureWhite
import com.example.utils.SignalQualityUtils

@Composable
fun DashboardScreen(
    state: WifiToolsUiState,
    onTriggerScan: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onSaveSnapshot: () -> Unit,
    onNavigateToTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val weakCount = state.discoveredNetworks.count {
        it.securityProtocol == SecurityProtocol.OPEN ||
            it.securityProtocol == SecurityProtocol.WEP_LEGACY ||
            it.securityProtocol == SecurityProtocol.WPA_LEGACY ||
            it.hasWps
    }
    val wpa3Count = state.discoveredNetworks.count {
        it.securityProtocol == SecurityProtocol.WPA3_SAE ||
            it.securityProtocol == SecurityProtocol.WPA3_ENTERPRISE ||
            it.securityProtocol == SecurityProtocol.WPA2_WPA3_TRANSITION
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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

        // Hero Radar & Live RF Command Card
        item {
            CyberPanel(highlighted = state.isScanning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PulsingLiveDot(
                                active = state.activeConnection.isConnectedToWifi || state.isScanning,
                                color = if (state.activeConnection.isConnectedToWifi) CyberGreen else NeonRed
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (state.isScanning) strings.activeRfSweep else strings.rfCommandConsole,
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonRed
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (state.activeConnection.isConnectedToWifi) state.activeConnection.ssid else strings.standbyMode,
                            style = MaterialTheme.typography.headlineMedium,
                            color = PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (state.activeConnection.isConnectedToWifi) {
                                "${state.activeConnection.band.label} • Ch ${state.activeConnection.channel} • ${state.activeConnection.rssiDbm} dBm (${strings.signalQualityLabel(state.activeConnection.rssiDbm)})"
                            } else {
                                strings.standbyHint
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonTextMuted
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onTriggerScan,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                                modifier = Modifier.testTag("dashboard_scan_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = strings.btnScanRf,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (state.isScanning) strings.btnScanning else strings.btnScanRf)
                            }
                            OutlinedButton(
                                onClick = onSaveSnapshot,
                                border = BorderStroke(1.dp, CrimsonBorderBright),
                                modifier = Modifier.testTag("dashboard_save_snapshot_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Save,
                                    contentDescription = strings.btnLogScan,
                                    tint = PureWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier.size(116.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedRadarSweep(
                            networks = state.discoveredNetworks,
                            isScanning = state.isScanning,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Key Telemetry Metrics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CyberMetricTile(
                    label = strings.metricDiscoveredAps,
                    value = state.discoveredNetworks.size.toString(),
                    unit = strings.unitNodes,
                    accentColor = NeonRed,
                    icon = Icons.Rounded.Radar,
                    modifier = Modifier.weight(1f)
                )
                CyberMetricTile(
                    label = strings.metricWeakWps,
                    value = weakCount.toString(),
                    unit = strings.unitAlerts,
                    accentColor = if (weakCount > 0) CyberCritical else CyberGreen,
                    icon = Icons.Rounded.Security,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CyberMetricTile(
                    label = strings.metricWpa3Ready,
                    value = wpa3Count.toString(),
                    unit = strings.unitAps,
                    accentColor = CyberGreen,
                    icon = Icons.Rounded.Wifi,
                    modifier = Modifier.weight(1f)
                )
                CyberMetricTile(
                    label = strings.metricLinkSpeed,
                    value = if (state.activeConnection.isConnectedToWifi) "${state.activeConnection.linkSpeedMbps}" else "--",
                    unit = "Mbps",
                    accentColor = CyberCyan,
                    icon = Icons.Rounded.Speed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Active Interface Summary
        item {
            CyberSectionHeader(
                title = strings.sectionActiveInterface,
                subtitle = state.lastScanTimestampMs?.let { "${strings.subtitleLastRfUpdate} ${SignalQualityUtils.formatShortTime(it)}" }
                    ?: strings.subtitleLiveTelemetry
            )
            Spacer(modifier = Modifier.height(8.dp))
            CyberPanel {
                CyberKeyValueRow(
                    strings.labelGateway,
                    state.activeConnection.gatewayIp ?: strings.valueUnavailable,
                    forceLtrValue = state.activeConnection.gatewayIp != null
                )
                CyberKeyValueRow(
                    strings.labelLocalIp,
                    state.activeConnection.localIpAddresses.firstOrNull() ?: strings.valueUnavailable,
                    forceLtrValue = state.activeConnection.localIpAddresses.isNotEmpty()
                )
                CyberKeyValueRow(
                    strings.labelDnsServers,
                    state.activeConnection.dnsServers.joinToString(", ").ifBlank { strings.valueUnavailable },
                    forceLtrValue = state.activeConnection.dnsServers.isNotEmpty()
                )
                CyberKeyValueRow(
                    strings.labelPrivateDns,
                    if (state.activeConnection.isPrivateDnsActive) {
                        state.activeConnection.privateDnsServerName ?: strings.valueEnabledDot
                    } else strings.valuePlaintextDns,
                    valueColor = if (state.activeConnection.isPrivateDnsActive) CyberGreen else CyberAmber
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CyberBadge(
                        text = if (state.activeConnection.hasValidatedInternet) strings.badgeInternetValidated else strings.badgeNoValidatedWan,
                        color = if (state.activeConnection.hasValidatedInternet) CyberGreen else CyberAmber,
                        backgroundColor = if (state.activeConnection.hasValidatedInternet) CyberGreenContainer else CyberAmberContainer
                    )
                    if (state.activeConnection.isVpnActive) {
                        CyberBadge(
                            text = strings.badgeVpnTunnel,
                            color = CyberCyan,
                            backgroundColor = CyberGreenContainer
                        )
                    }
                    if (state.activeConnection.isCaptivePortal) {
                        CyberBadge(
                            text = strings.badgeCaptivePortal,
                            color = CyberCritical,
                            backgroundColor = CyberCriticalContainer
                        )
                    }
                }
            }
        }

        // Quick Module Launchers
        item {
            CyberSectionHeader(
                title = strings.sectionModules,
                subtitle = strings.subtitleModules
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickModuleCard(
                        title = strings.moduleScannerTitle,
                        subtitle = strings.moduleScannerSub,
                        icon = Icons.Rounded.Radar,
                        tag = "quick_module_scanner",
                        onClick = { onNavigateToTab("scanner") },
                        modifier = Modifier.weight(1f)
                    )
                    QuickModuleCard(
                        title = strings.moduleAnalyzerTitle,
                        subtitle = strings.moduleAnalyzerSub,
                        icon = Icons.Rounded.Speed,
                        tag = "quick_module_analyzer",
                        onClick = { onNavigateToTab("analyzer") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickModuleCard(
                        title = strings.moduleSecurityTitle,
                        subtitle = strings.moduleSecuritySub,
                        icon = Icons.Rounded.Security,
                        tag = "quick_module_security",
                        onClick = { onNavigateToTab("security") },
                        modifier = Modifier.weight(1f)
                    )
                    QuickModuleCard(
                        title = strings.moduleRouterTitle,
                        subtitle = strings.moduleRouterSub,
                        icon = Icons.Rounded.Router,
                        tag = "quick_module_router",
                        onClick = { onNavigateToTab("router") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            AndroidLimitationDisclosureCard()
        }
    }
}

@Composable
private fun QuickModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CyberPanel(
        modifier = modifier.testTag(tag),
        onClick = onClick
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = NeonRed,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = CrimsonTextMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
