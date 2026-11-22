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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.domain.SecurityProtocol
import com.example.presentation.components.AndroidLimitationDisclosureCard
import com.example.presentation.components.AuditStatusBadge
import com.example.presentation.components.CyberBadge
import com.example.presentation.components.CyberPanel
import com.example.presentation.components.CyberSectionHeader
import com.example.presentation.components.RiskBadge
import com.example.presentation.components.riskColors
import com.example.presentation.viewmodel.WifiToolsUiState
import com.example.security.EncryptionKnowledgeBase
import com.example.ui.theme.CrimsonBorder
import com.example.ui.theme.CrimsonBorderBright
import com.example.ui.theme.CrimsonPanel
import com.example.ui.theme.CrimsonPanelHigh
import com.example.ui.theme.CrimsonPanelHighest
import com.example.ui.theme.CrimsonTextMuted
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCritical
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberGreenContainer
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.NeonRed
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

@Composable
fun SecurityScreen(
    state: WifiToolsUiState,
    onAuthorizationToggle: (Boolean) -> Unit,
    onRunAuthorizedAudit: () -> Unit,
    onToggleHardeningItem: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onGeneratePassphrase: () -> Unit,
    onGenerateReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by rememberSaveable { mutableIntStateOf(0) }
    val subTabs = listOf("Audit", "Passwords", "Standards")

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = PureBlack,
            contentColor = PureWhite,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                    color = NeonRed
                )
            }
        ) {
            subTabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedSubTab == idx,
                    onClick = { selectedSubTab = idx },
                    text = {
                        Text(
                            text = title.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selectedSubTab == idx) NeonRed else CrimsonTextMuted
                        )
                    },
                    modifier = Modifier.testTag("security_subtab_$idx")
                )
            }
        }

        when (selectedSubTab) {
            0 -> AuthorizedAuditTab(
                state = state,
                onAuthorizationToggle = onAuthorizationToggle,
                onRunAuthorizedAudit = onRunAuthorizedAudit,
                onGenerateReport = onGenerateReport
            )
            1 -> LocalPasswordValidatorTab(
                state = state,
                onPasswordChange = onPasswordChange,
                onGeneratePassphrase = onGeneratePassphrase
            )
            else -> StandardsAndHardeningTab(
                state = state,
                onToggleHardeningItem = onToggleHardeningItem
            )
        }
    }
}

@Composable
private fun AuthorizedAuditTab(
    state: WifiToolsUiState,
    onAuthorizationToggle: (Boolean) -> Unit,
    onRunAuthorizedAudit: () -> Unit,
    onGenerateReport: () -> Unit
) {
    val weakNetworks = state.discoveredNetworks.filter {
        it.securityProtocol == SecurityProtocol.OPEN ||
            it.securityProtocol == SecurityProtocol.WEP_LEGACY ||
            it.securityProtocol == SecurityProtocol.WPA_LEGACY ||
            it.hasWps ||
            it.hasTkip
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Explicit Authorization Gate
        item {
            CyberPanel(highlighted = state.isAuthorizationConfirmed) {
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
                            imageVector = Icons.Rounded.Policy,
                            contentDescription = "Authorization Confirmation",
                            tint = if (state.isAuthorizationConfirmed) CyberGreen else NeonRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AUTHORIZED SECURITY TESTING",
                                style = MaterialTheme.typography.titleSmall,
                                color = PureWhite
                            )
                            Text(
                                text = "I own or hold explicit written permission to audit this network.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CrimsonTextMuted
                            )
                        }
                    }
                    Switch(
                        checked = state.isAuthorizationConfirmed,
                        onCheckedChange = onAuthorizationToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PureWhite,
                            checkedTrackColor = NeonRed,
                            uncheckedTrackColor = CrimsonPanelHighest
                        ),
                        modifier = Modifier.testTag("authorization_confirm_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onRunAuthorizedAudit,
                        enabled = state.isAuthorizationConfirmed && !state.isRunningAuthorizedAudit,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("run_authorized_audit_button")
                    ) {
                        if (state.isRunningAuthorizedAudit) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = PureWhite,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Auditing…")
                        } else {
                            Icon(Icons.Rounded.GppGood, contentDescription = "Run Audit", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Config Audit")
                        }
                    }

                    OutlinedButton(
                        onClick = onGenerateReport,
                        border = BorderStroke(1.dp, CrimsonBorderBright),
                        modifier = Modifier.testTag("security_save_report_button")
                    ) {
                        Text("Save Report", color = PureWhite)
                    }
                }
            }
        }

        // Audit Results
        val report = state.authorizedAuditReport
        if (report != null) {
            item {
                CyberSectionHeader(
                    title = "Configuration Audit — ${report.targetSsid}",
                    subtitle = "Gateway: ${report.targetGateway ?: "N/A"}",
                    trailing = {
                        CyberBadge(
                            text = "SCORE ${report.overallScore}/100",
                            color = if (report.overallScore >= 80) CyberGreen else CyberAmber,
                            backgroundColor = CyberGreenContainer
                        )
                    }
                )
            }
            items(report.checks) { check ->
                CyberPanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = check.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = PureWhite,
                            modifier = Modifier.weight(1f)
                        )
                        AuditStatusBadge(status = check.status)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(check.detail, style = MaterialTheme.typography.bodySmall, color = PureWhite)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Action: ${check.remediation}", style = MaterialTheme.typography.bodySmall, color = CrimsonTextMuted)
                }
            }
        }

        // Weak Configurations Found in Current Scan
        item {
            CyberSectionHeader(
                title = "Weak Configs In RF Range",
                subtitle = "${weakNetworks.size} access points flagged for legacy ciphers, open access, or WPS"
            )
        }

        if (weakNetworks.isEmpty()) {
            item {
                CyberPanel {
                    Text(
                        text = if (state.discoveredNetworks.isEmpty()) {
                            "No Wi-Fi scan results loaded yet. Scan nearby networks in the Scanner tab to inspect security posture."
                        } else {
                            "No open, WEP, WPA1, TKIP, or WPS-enabled networks detected in the current scan."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonTextMuted
                    )
                }
            }
        } else {
            items(weakNetworks, key = { it.bssid }) { net ->
                CyberPanel(highlighted = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(net.ssid, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                            Text(
                                "${net.securityProtocol.label}${if (net.hasWps) " • WPS ON" else ""}${if (net.hasTkip) " • TKIP" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberCritical
                            )
                        }
                        RiskBadge(riskLevel = net.riskLevel)
                    }
                }
            }
        }

        item {
            AndroidLimitationDisclosureCard()
        }
    }
}

@Composable
private fun LocalPasswordValidatorTab(
    state: WifiToolsUiState,
    onPasswordChange: (String) -> Unit,
    onGeneratePassphrase: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }
    val analysis = state.passwordAnalysis

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CyberSectionHeader(
                title = "Local Passphrase Validator",
                subtitle = "100% on-device entropy & WPA2/WPA3 compliance check — never transmitted"
            )
            Spacer(modifier = Modifier.height(8.dp))
            CyberPanel {
                OutlinedTextField(
                    value = state.passwordInput,
                    onValueChange = onPasswordChange,
                    label = { Text("Test Your Own Wi-Fi Passphrase", color = CrimsonTextMuted) },
                    leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = "Passphrase", tint = NeonRed) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (showPassword) "Hide passphrase" else "Show passphrase",
                                tint = CrimsonTextMuted
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
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
                        .testTag("password_validator_input")
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onGeneratePassphrase,
                    border = BorderStroke(1.dp, CrimsonBorderBright),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generate_passphrase_button")
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = "Generate", tint = NeonRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate Local 20-Char SecureRandom Key", color = PureWhite)
                }

                if (analysis != null) {
                    val (fg, bg) = riskColors(analysis.riskLevel)
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Strength: ${analysis.strengthLabel}",
                            style = MaterialTheme.typography.titleMedium,
                            color = fg
                        )
                        CyberBadge(
                            text = "${analysis.entropyBits} bits • ${analysis.score}/100",
                            color = fg,
                            backgroundColor = bg
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (analysis.score / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = fg,
                        trackColor = CrimsonPanelHighest
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Length: ${analysis.length} chars (8–63 WPA spec: ${if (analysis.wpaLengthCompliant) "OK" else "INVALID"})",
                        style = MonoMetricMedium,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Offline WPA2 Resistance: ${analysis.estimatedCrackResistance}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonTextMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CharClassPill("a-z", analysis.hasLowercase)
                        CharClassPill("A-Z", analysis.hasUppercase)
                        CharClassPill("0-9", analysis.hasDigits)
                        CharClassPill("!@#", analysis.hasSymbols)
                    }

                    if (analysis.warnings.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        analysis.warnings.forEach { warn ->
                            Text(
                                text = "⚠ $warn",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberAmber,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }

                    if (analysis.recommendations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        analysis.recommendations.forEach { rec ->
                            Text(
                                text = "• $rec",
                                style = MaterialTheme.typography.bodySmall,
                                color = PureWhite,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CharClassPill(label: String, present: Boolean) {
    CyberBadge(
        text = label,
        color = if (present) CyberGreen else CrimsonTextMuted,
        backgroundColor = if (present) CyberGreenContainer else CrimsonPanelHigh
    )
}

@Composable
private fun StandardsAndHardeningTab(
    state: WifiToolsUiState,
    onToggleHardeningItem: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CyberSectionHeader(
                title = "Router Hardening Checklist",
                subtitle = "${state.completedHardeningIds.size}/${EncryptionKnowledgeBase.routerHardeningChecklist.size} verified"
            )
        }

        items(EncryptionKnowledgeBase.routerHardeningChecklist, key = { it.id }) { item ->
            val checked = item.id in state.completedHardeningIds
            CyberPanel(
                onClick = { onToggleHardeningItem(item.id) },
                modifier = Modifier.testTag("hardening_item_${item.id}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { onToggleHardeningItem(item.id) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = CyberGreen,
                            uncheckedColor = CrimsonBorderBright,
                            checkmarkColor = PureBlack
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (checked) CyberGreen else PureWhite,
                                modifier = Modifier.weight(1f)
                            )
                            RiskBadge(riskLevel = item.priority)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(item.detail, style = MaterialTheme.typography.bodySmall, color = CrimsonTextMuted)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            CyberSectionHeader(
                title = "Wi-Fi Encryption Reference",
                subtitle = "WPA3, WPA2, OWE & legacy protocol architecture"
            )
        }

        items(EncryptionKnowledgeBase.standards, key = { it.name }) { std ->
            CyberPanel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = if (std.statusBadge == "RECOMMENDED") Icons.Rounded.CheckCircle else Icons.Rounded.Shield,
                            contentDescription = std.name,
                            tint = riskColors(std.riskLevel).first,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(std.name, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                    }
                    RiskBadge(riskLevel = std.riskLevel)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Cipher Suite: ${std.cipherSuite}", style = MaterialTheme.typography.labelMedium, color = CyberCyan)
                Spacer(modifier = Modifier.height(6.dp))
                Text(std.summary, style = MaterialTheme.typography.bodySmall, color = PureWhite)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Risk Profile: ${std.vulnerabilities}", style = MaterialTheme.typography.bodySmall, color = CrimsonTextMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Guidance: ${std.guidance}", style = MaterialTheme.typography.bodySmall, color = CyberGreen)
            }
        }
    }
}
