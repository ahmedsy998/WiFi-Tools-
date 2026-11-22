package com.example.presentation.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.SecurityReportRecord
import com.example.presentation.components.CyberBadge
import com.example.presentation.components.CyberPanel
import com.example.presentation.components.CyberSectionHeader
import com.example.presentation.components.RiskBadge
import com.example.presentation.viewmodel.WifiToolsUiState
import com.example.ui.theme.CrimsonBorderBright
import com.example.ui.theme.CrimsonPanel
import com.example.ui.theme.CrimsonTextMuted
import com.example.ui.theme.CyberCritical
import com.example.ui.theme.CyberCriticalContainer
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanContainer
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberGreenContainer
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.NeonRed
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.utils.SignalQualityUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    state: WifiToolsUiState,
    onGenerateReport: () -> Unit,
    onPreviewReport: (SecurityReportRecord?) -> Unit,
    onShareReport: (SecurityReportRecord) -> Unit,
    onExportReportMarkdown: (SecurityReportRecord) -> Unit,
    onExportReportJson: (SecurityReportRecord) -> Unit,
    onDeleteReport: (Long) -> Unit,
    onDeleteScanHistory: (Long) -> Unit,
    onPurgeAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPurgeDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Actions Header
        item {
            CyberPanel {
                Text(
                    text = "LOCAL ASSESSMENT VAULT",
                    style = MaterialTheme.typography.titleSmall,
                    color = NeonRed
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "All scan history and security assessments are stored strictly on-device in encrypted-ready local Room SQLite storage. Export via Share or Save to Files anytime.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CrimsonTextMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onGenerateReport,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reports_generate_button")
                    ) {
                        Icon(Icons.Rounded.Assessment, contentDescription = "Generate Report", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Report")
                    }
                    OutlinedButton(
                        onClick = { showPurgeDialog = true },
                        enabled = state.scanHistory.isNotEmpty() || state.securityReports.isNotEmpty(),
                        border = BorderStroke(1.dp, CyberCritical.copy(alpha = 0.7f)),
                        modifier = Modifier.testTag("reports_purge_all_button")
                    ) {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = "Purge All", tint = CyberCritical, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Purge", color = CyberCritical)
                    }
                }
            }
        }

        // Security Assessments List
        item {
            CyberSectionHeader(
                title = "Security & Diagnostic Reports",
                subtitle = "${state.securityReports.size} saved assessments"
            )
        }

        if (state.securityReports.isEmpty()) {
            item {
                CyberPanel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Description, contentDescription = "No reports", tint = CrimsonTextMuted, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "No saved reports yet. Tap 'New Report' to capture current Wi-Fi telemetry, audit findings, and router diagnostics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonTextMuted
                        )
                    }
                }
            }
        } else {
            items(state.securityReports, key = { "rep_${it.id}" }) { report ->
                CyberPanel(
                    onClick = { onPreviewReport(report) },
                    modifier = Modifier.testTag("report_item_${report.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(report.title, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                            Text(
                                SignalQualityUtils.formatTimestamp(report.timestampMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = CrimsonTextMuted
                            )
                        }
                        RiskBadge(riskLevel = report.riskLevel)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(report.summary, style = MaterialTheme.typography.bodySmall, color = PureWhite)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onShareReport(report) },
                            border = BorderStroke(1.dp, CrimsonBorderBright),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share", tint = PureWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", color = PureWhite)
                        }
                        OutlinedButton(
                            onClick = { onExportReportMarkdown(report) },
                            border = BorderStroke(1.dp, CrimsonBorderBright),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = "Export MD", tint = CyberCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(".MD", color = PureWhite)
                        }
                        IconButton(onClick = { onDeleteReport(report.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete report", tint = CyberCritical)
                        }
                    }
                }
            }
        }

        // Local Scan History List
        item {
            CyberSectionHeader(
                title = "Local Scan History",
                subtitle = "${state.scanHistory.size} recorded RF snapshots"
            )
        }

        if (state.scanHistory.isEmpty()) {
            item {
                CyberPanel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.History, contentDescription = "No scan history", tint = CrimsonTextMuted, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "No scan snapshots stored yet. Use 'Log Scan' from the Scanner or Dashboard after scanning.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonTextMuted
                        )
                    }
                }
            }
        } else {
            items(state.scanHistory, key = { "scan_${it.id}" }) { record ->
                CyberPanel(modifier = Modifier.testTag("scan_history_item_${record.id}")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${record.totalNetworks} Access Points Logged",
                                style = MaterialTheme.typography.titleMedium,
                                color = PureWhite
                            )
                            Text(
                                text = SignalQualityUtils.formatTimestamp(record.timestampMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = CrimsonTextMuted
                            )
                        }
                        IconButton(onClick = { onDeleteScanHistory(record.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete snapshot", tint = CyberCritical)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CyberBadge(
                            text = "WPA3: ${record.wpa3Count}",
                            color = CyberGreen,
                            backgroundColor = CyberGreenContainer
                        )
                        CyberBadge(
                            text = "WEAK/WPS: ${record.openOrWeakCount}",
                            color = if (record.openOrWeakCount > 0) CyberCritical else CyberCyan,
                            backgroundColor = if (record.openOrWeakCount > 0) CyberCriticalContainer else CyberCyanContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Strongest AP: ${record.strongestSsid} (${record.strongestRssi} dBm)" +
                            (record.connectedSsid?.let { " • Connected: $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonTextMuted
                    )
                }
            }
        }
    }

    // Report Preview Modal Sheet
    val preview = state.selectedReportForPreview
    if (preview != null) {
        ModalBottomSheet(
            onDismissRequest = { onPreviewReport(null) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = PureBlack,
            contentColor = PureWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(preview.title, style = MaterialTheme.typography.titleLarge, color = PureWhite, modifier = Modifier.weight(1f))
                    RiskBadge(riskLevel = preview.riskLevel)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onShareReport(preview) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                    OutlinedButton(
                        onClick = { onExportReportMarkdown(preview) },
                        border = BorderStroke(1.dp, CrimsonBorderBright)
                    ) {
                        Text("Save .MD", color = PureWhite)
                    }
                    OutlinedButton(
                        onClick = { onExportReportJson(preview) },
                        border = BorderStroke(1.dp, CrimsonBorderBright)
                    ) {
                        Text("Save .JSON", color = PureWhite)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                CyberPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    Text(
                        text = preview.fullMarkdown,
                        style = MonoCodeStyle,
                        color = PureWhite,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showPurgeDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeDialog = false },
            containerColor = CrimsonPanel,
            titleContentColor = PureWhite,
            textContentColor = CrimsonTextMuted,
            title = { Text("Purge All Local Data?") },
            text = { Text("This permanently deletes all saved Wi-Fi scan snapshots and generated security reports from local device storage.") },
            confirmButton = {
                Button(
                    onClick = {
                        showPurgeDialog = false
                        onPurgeAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCritical)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeDialog = false }) {
                    Text("Cancel", color = PureWhite)
                }
            }
        )
    }
}
