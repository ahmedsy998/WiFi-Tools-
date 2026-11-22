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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.example.presentation.components.CyberKeyValueRow
import com.example.presentation.components.CyberMetricTile
import com.example.presentation.components.CyberPanel
import com.example.presentation.components.CyberSectionHeader
import com.example.presentation.components.LatencySparklineChart
import com.example.presentation.i18n.LocalAppStrings
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
    val strings = LocalAppStrings.current
    val conn = state.activeConnection
    val result = state.latencyDiagnosticResult
    val samplesToPlot = if (state.isRunningLatencyTest) state.liveProbeSamples else result?.samples.orEmpty()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CyberSectionHeader(
                title = strings.sectionGatewayDns,
                subtitle = strings.subtitleGatewayDns
            )
            Spacer(modifier = Modifier.height(8.dp))
            CyberPanel {
                CyberKeyValueRow(
                    strings.labelDefaultGateway,
                    conn.gatewayIp ?: strings.valueUnavailable,
                    forceLtrValue = conn.gatewayIp != null
                )
                CyberKeyValueRow(
                    strings.labelAssignedIps,
                    conn.localIpAddresses.joinToString("\n").ifBlank { strings.valueUnavailable },
                    forceLtrValue = conn.localIpAddresses.isNotEmpty()
                )
                CyberKeyValueRow(
                    strings.labelSubnetPrefix,
                    conn.subnetMaskOrPrefix ?: "N/A",
                    forceLtrValue = true
                )
                CyberKeyValueRow(
                    strings.labelPrimaryDns,
                    conn.dnsServers.getOrNull(0) ?: strings.valueUnavailable,
                    forceLtrValue = conn.dnsServers.isNotEmpty()
                )
                CyberKeyValueRow(
                    strings.labelSecondaryDns,
                    conn.dnsServers.getOrNull(1) ?: strings.valueNone,
                    forceLtrValue = conn.dnsServers.size > 1
                )
                CyberKeyValueRow(
                    strings.labelPrivateDns,
                    if (conn.isPrivateDnsActive) conn.privateDnsServerName ?: strings.valueEnabledDot else strings.valueNone,
                    valueColor = if (conn.isPrivateDnsActive) CyberGreen else CyberAmber
                )
                CyberKeyValueRow(
                    strings.labelInterfaceMtu,
                    "${conn.interfaceName ?: "wlan0"} • ${if (conn.mtu > 0) "${conn.mtu} B" else "Auto"}",
                    forceLtrValue = true
                )
            }
        }

        item {
            CyberSectionHeader(
                title = strings.sectionLatencyTest,
                subtitle = strings.subtitleLatencyTest
            )
            Spacer(modifier = Modifier.height(8.dp))
            CyberPanel(highlighted = state.isRunningLatencyTest) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!conn.gatewayIp.isNullOrBlank()) {
                        PresetHostChip("${strings.labelGateway} (${conn.gatewayIp})") {
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
                        label = { Text(strings.labelAuthorizedHost, color = CrimsonTextMuted) },
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
                        label = { Text(strings.labelPort, color = CrimsonTextMuted) },
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
                    Text(strings.labelProbeCount, style = MaterialTheme.typography.bodySmall, color = CrimsonTextMuted)
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
                            Text("${strings.btnProbing} (${state.liveProbeSamples.size}/${state.probeCountInput})…")
                        } else {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = strings.btnRunProbeTest, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.btnRunProbeTest)
                        }
                    }

                    OutlinedButton(
                        onClick = onSaveDiagnosticReport,
                        border = BorderStroke(1.dp, CrimsonBorderBright),
                        modifier = Modifier.testTag("router_save_report_button")
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = strings.btnSaveReport, tint = PureWhite, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (samplesToPlot.isNotEmpty()) {
            item {
                CyberPanel {
                    Text(
                        text = strings.labelRttSparkline,
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
                            style = MonoCodeStyle.copy(textDirection = TextDirection.Ltr),
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
                        label = strings.metricAvgRtt,
                        value = result.avgLatencyMs?.let { "%.1f".format(it) } ?: "--",
                        unit = "ms",
                        accentColor = CyberCyan,
                        icon = Icons.Rounded.Router,
                        modifier = Modifier.weight(1f)
                    )
                    CyberMetricTile(
                        label = strings.metricPacketLoss,
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
                    CyberKeyValueRow(strings.labelTarget, "${result.targetHost}:${result.targetPort}", forceLtrValue = true)
                    CyberKeyValueRow(strings.labelResolvedIp, result.resolvedIp ?: strings.valueDnsUnresolved, forceLtrValue = result.resolvedIp != null)
                    CyberKeyValueRow(
                        strings.labelMinMaxRtt,
                        "${result.minLatencyMs ?: "--"} ms / ${result.maxLatencyMs ?: "--"} ms",
                        forceLtrValue = true
                    )
                    CyberKeyValueRow(
                        strings.labelJitter,
                        result.jitterMs?.let { "%.1f ms".format(it) } ?: "N/A",
                        forceLtrValue = true
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
