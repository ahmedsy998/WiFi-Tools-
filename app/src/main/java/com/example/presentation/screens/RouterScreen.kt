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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.presentation.components.CyberKeyValueRow
import com.example.presentation.components.CyberMetricTile
import com.example.presentation.components.CyberPanel
import com.example.presentation.components.CyberSectionHeader
import com.example.presentation.components.LatencySparklineChart
import com.example.presentation.viewmodel.WifiToolsUiState
import com.example.ui.theme.CrimsonBorder
import com.example.ui.theme.CrimsonBorderBright
import com.example.ui.theme.CrimsonPanelHigh
import com.example.ui.theme.CrimsonTextMuted
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCritical
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.NeonRed
import com.example.ui.theme.PureWhite

@Composable
fun RouterScreen(
    state: WifiToolsUiState,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onProbeCountChange: (Int) -> Unit,
    onRunLatencyTest: () -> Unit,
    onSaveDiagnosticReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val conn = state.activeConnection
    val result = state.latencyDiagnosticResult
    val samplesToPlot = if (state.isRunningLatencyTest) state.liveProbeSamples else result?.samples.orEmpty()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Gateway & DNS Routing Table
        item {
            CyberSectionHeader(
                title = "Gateway & DNS Configuration",
                subtitle = "Extracted from Android LinkProperties & active Wi-Fi interface"
            )
            Spacer(modifier = Modifier.height(8.dp))
            CyberPanel {
                CyberKeyValueRow("Default Gateway", conn.gatewayIp ?: "Not Available")
                CyberKeyValueRow("Assigned IP(s)", conn.localIpAddresses.joinToString("\n").ifBlank { "Not Available" })
                CyberKeyValueRow("Subnet Prefix", conn.subnetMaskOrPrefix ?: "N/A")
                CyberKeyValueRow("Primary DNS", conn.dnsServers.getOrNull(0) ?: "Not Available")
                CyberKeyValueRow("Secondary DNS", conn.dnsServers.getOrNull(1) ?: "None")
                CyberKeyValueRow(
                    "Private DNS (DoT)",
                    if (conn.isPrivateDnsActive) conn.privateDnsServerName ?: "Active (Opportunistic)" else "Off",
                    valueColor = if (conn.isPrivateDnsActive) CyberGreen else CyberAmber
                )
                CyberKeyValueRow("Interface / MTU", "${conn.interfaceName ?: "wlan0"} • ${if (conn.mtu > 0) "${conn.mtu} B" else "Auto"}")
            }
        }

        // Latency & Packet Loss Test Console
        item {
            CyberSectionHeader(
                title = "Authorized Latency & Loss Test",
                subtitle = "Genuine TCP/ICMP multi-probe RTT, jitter & packet-loss analyzer"
            )
            Spacer(modifier = Modifier.height(8.dp))
            CyberPanel(highlighted = state.isRunningLatencyTest) {
                // Quick Authorized Target Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!conn.gatewayIp.isNullOrBlank()) {
                        PresetHostChip("Gateway (${conn.gatewayIp})") {
                            onHostChange(conn.gatewayIp)
                            onPortChange("80")
                        }
                    }
                    if (conn.dnsServers.isNotEmpty()) {
                        val dns1 = conn.dnsServers.first()
                        PresetHostChip("DNS ($dns1)") {
                            onHostChange(dns1)
                            onPortChange("53")
                        }
                    }
                    PresetHostChip("Cloudflare 1.1.1.1") {
                        onHostChange("1.1.1.1")
                        onPortChange("443")
                    }
                    PresetHostChip("Google 8.8.8.8") {
                        onHostChange("8.8.8.8")
                        onPortChange("53")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.targetHostInput,
                        onValueChange = onHostChange,
                        label = { Text("Authorized Host / IP", color = CrimsonTextMuted) },
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
                            .weight(0.68f)
                            .testTag("router_target_host_input")
                    )

                    OutlinedTextField(
                        value = state.targetPortInput,
                        onValueChange = onPortChange,
                        label = { Text("Port", color = CrimsonTextMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonRed,
                            unfocusedBorderColor = CrimsonBorder,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            cursorColor = NeonRed
                        ),
                        modifier = Modifier
                            .weight(0.32f)
                            .testTag("router_target_port_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Probe Count:", style = MaterialTheme.typography.bodySmall, color = CrimsonTextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(4, 6, 10).forEach { count ->
                            AssistChip(
                                onClick = { onProbeCountChange(count) },
                                label = { Text("$count") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (state.probeCountInput == count) NeonRed else CrimsonPanelHigh,
                                    labelColor = PureWhite
                                ),
                                border = BorderStroke(1.dp, if (state.probeCountInput == count) NeonRed else CrimsonBorder)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onRunLatencyTest,
                        enabled = !state.isRunningLatencyTest,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("router_run_latency_button")
                    ) {
                        if (state.isRunningLatencyTest) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = PureWhite,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Probing (${state.liveProbeSamples.size}/${state.probeCountInput})…")
                        } else {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Start Test", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Probe Test")
                        }
                    }

                    OutlinedButton(
                        onClick = onSaveDiagnosticReport,
                        border = BorderStroke(1.dp, CrimsonBorderBright),
                        modifier = Modifier.testTag("router_save_report_button")
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = "Save Report", tint = PureWhite, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Live / Completed Latency Metrics & Sparkline
        if (samplesToPlot.isNotEmpty()) {
            item {
                CyberPanel {
                    Text(
                        text = "RTT TELEMETRY SPARKLINE",
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LatencySparklineChart(samples = samplesToPlot)
                    Spacer(modifier = Modifier.height(10.dp))
                    samplesToPlot.forEach { sample ->
                        Text(
                            text = if (sample.reachable) {
                                "#${sample.sequence}  ${sample.method}  reply=${sample.latencyMs} ms"
                            } else {
                                "#${sample.sequence}  ${sample.method}  TIMEOUT (${sample.errorDetail ?: "Unreachable"})"
                            },
                            style = MonoCodeStyle,
                            color = if (sample.reachable) CyberGreen else CyberCritical
                        )
                    }
                }
            }
        }

        if (result != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CyberMetricTile(
                        label = "Avg RTT",
                        value = result.avgLatencyMs?.let { "%.1f".format(it) } ?: "--",
                        unit = "ms",
                        accentColor = CyberCyan,
                        icon = Icons.Rounded.Router,
                        modifier = Modifier.weight(1f)
                    )
                    CyberMetricTile(
                        label = "Packet Loss",
                        value = "%.0f".format(result.packetLossPercent),
                        unit = "%",
                        accentColor = if (result.packetLossPercent == 0.0) CyberGreen else CyberCritical,
                        icon = Icons.Rounded.Dns,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                CyberPanel {
                    CyberKeyValueRow("Target", "${result.targetHost}:${result.targetPort}")
                    CyberKeyValueRow("Resolved IP", result.resolvedIp ?: "DNS Unresolved")
                    CyberKeyValueRow(
                        "Min / Max RTT",
                        "${result.minLatencyMs ?: "--"} ms / ${result.maxLatencyMs ?: "--"} ms"
                    )
                    CyberKeyValueRow(
                        "Jitter",
                        result.jitterMs?.let { "%.1f ms".format(it) } ?: "N/A"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = result.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = PureWhite
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetHostChip(label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = CrimsonPanelHigh,
            labelColor = PureWhite
        ),
        border = BorderStroke(1.dp, CrimsonBorder)
    )
}
