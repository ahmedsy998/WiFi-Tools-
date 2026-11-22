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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
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
                                text = if (state.isScanning) "ACTIVE RF SWEEP" else "RF COMMAND CONSOLE",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonRed
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (state.activeConnection.isConnectedToWifi) state.activeConnection.ssid else "Standby Mode",
                            style = MaterialTheme.typography.headlineMedium,
                            color = PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (state.activeConnection.isConnectedToWifi) {
                                "${state.activeConnection.band.label} • Ch ${state.activeConnection.channel} • ${state.activeConnection.rssiDbm} dBm (${SignalQualityUtils.rssiQualityLabel(state.activeConnection.rssiDbm)})"
                            } else {
                                state.activeConnection.limitationNote ?: "Connect to Wi-Fi or run a spectrum scan."
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
                                    contentDescription = "Scan Wi-Fi",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (state.isScanning) "Scanning…" else "Scan RF")
                            }
                            OutlinedButton(
                                onClick = onSaveSnapshot,
                                border = BorderStroke(1.dp, CrimsonBorderBright),
                                modifier = Modifier.testTag("dashboard_save_snapshot_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Save,
                                    contentDescription = "Save Snapshot",
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
                    label = "Discovered APs",
                    value = state.discoveredNetworks.size.toString(),
                    unit = "nodes",
                    accentColor = NeonRed,
                    icon = Icons.Rounded.Radar,
                    modifier = Modifier.weight(1f)
                )
                CyberMetricTile(
                    label = "Weak / WPS",
                    value = weakCount.toString(),
                    unit = "alerts",
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
                    label = "WPA3 Ready",
                    value = wpa3Count.toString(),
                    unit = "APs",
                    accentColor = CyberGreen,
                    icon = Icons.Rounded.Wifi,
                    modifier = Modifier.weight(1f)
                )
                CyberMetricTile(
                    label = "Link Speed",
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
                title = "Active Interface Snapshot",
                subtitle = state.lastScanTimestampMs?.let { "Last RF update: ${SignalQualityUtils.formatShortTime(it)}" }
                    ?: "Live Android ConnectivityManager telemetry"
            )
            Spacer(modifier = Modifier.height(8.dp))
            CyberPanel {
                CyberKeyValueRow("Gateway", state.activeConnection.gatewayIp ?: "Unavailable")
                CyberKeyValueRow(
                    "Local IP",
                    state.activeConnection.localIpAddresses.firstOrNull() ?: "Unavailable"
                )
                CyberKeyValueRow(
                    "DNS Servers",
                    state.activeConnection.dnsServers.joinToString(", ").ifBlank { "Unavailable" }
                )
                CyberKeyValueRow(
                    "Private DNS",
                    if (state.activeConnection.isPrivateDnsActive) {
                        state.activeConnection.privateDnsServerName ?: "Enabled (DoT)"
                    } else "Plaintext UDP/53",
                    valueColor = if (state.activeConnection.isPrivateDnsActive) CyberGreen else CyberAmber
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CyberBadge(
                        text = if (state.activeConnection.hasValidatedInternet) "INTERNET VALIDATED" else "NO VALIDATED WAN",
                        color = if (state.activeConnection.hasValidatedInternet) CyberGreen else CyberAmber,
                        backgroundColor = if (state.activeConnection.hasValidatedInternet) CyberGreenContainer else CyberAmberContainer
                    )
                    if (state.activeConnection.isVpnActive) {
                        CyberBadge(
                            text = "VPN TUNNEL",
                            color = CyberCyan,
                            backgroundColor = CyberGreenContainer
                        )
                    }
                    if (state.activeConnection.isCaptivePortal) {
                        CyberBadge(
                            text = "CAPTIVE PORTAL",
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
                title = "Security & Diagnostic Modules",
                subtitle = "Native Android APIs • Zero simulated data"
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickModuleCard(
                        title = "Network Scanner",
                        subtitle = "SSID, BSSID, RSSI & Filters",
                        icon = Icons.Rounded.Radar,
                        tag = "quick_module_scanner",
                        onClick = { onNavigateToTab("scanner") },
                        modifier = Modifier.weight(1f)
                    )
                    QuickModuleCard(
                        title = "Band Analyzer",
                        subtitle = "Channel Congestion & Link",
                        icon = Icons.Rounded.Speed,
                        tag = "quick_module_analyzer",
                        onClick = { onNavigateToTab("analyzer") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickModuleCard(
                        title = "Security & Audit",
                        subtitle = "WPA3, Hardening & Passwords",
                        icon = Icons.Rounded.Security,
                        tag = "quick_module_security",
                        onClick = { onNavigateToTab("security") },
                        modifier = Modifier.weight(1f)
                    )
                    QuickModuleCard(
                        title = "Router & Reports",
                        subtitle = "Latency, Loss & Export",
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
