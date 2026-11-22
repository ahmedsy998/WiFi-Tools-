package com.example.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.AuditCheckStatus
import com.example.domain.RiskLevel
import com.example.permissions.WifiPermissionSnapshot
import com.example.ui.theme.CrimsonBorder
import com.example.ui.theme.CrimsonBorderBright
import com.example.ui.theme.CrimsonPanel
import com.example.ui.theme.CrimsonPanelHigh
import com.example.ui.theme.CrimsonPanelLow
import com.example.ui.theme.CrimsonTextMuted
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberAmberContainer
import com.example.ui.theme.CyberCritical
import com.example.ui.theme.CyberCriticalContainer
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanContainer
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberGreenContainer
import com.example.ui.theme.MonoMetricLarge
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonRedBright
import com.example.ui.theme.PureWhite

@Composable
fun CyberPanel(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val borderColor by animateColorAsState(
        targetValue = if (highlighted) NeonRed else CrimsonBorder,
        animationSpec = tween(250),
        label = "panelBorder"
    )
    val gradient = Brush.verticalGradient(
        colors = if (highlighted) {
            listOf(CrimsonPanelHigh, CrimsonPanelLow)
        } else {
            listOf(CrimsonPanel, CrimsonPanelLow)
        }
    )

    val baseModifier = modifier
        .clip(shape)
        .background(gradient)
        .border(width = 1.dp, color = borderColor, shape = shape)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }

    Column(
        modifier = baseModifier.padding(16.dp),
        content = content
    )
}

@Composable
fun CyberSectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(NeonRed)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = PureWhite
                )
            }
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = CrimsonTextMuted,
                    modifier = Modifier.padding(start = 11.dp)
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun CyberBadge(
    text: String,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RiskBadge(riskLevel: RiskLevel, modifier: Modifier = Modifier) {
    val (fg, bg) = riskColors(riskLevel)
    CyberBadge(text = riskLevel.label, color = fg, backgroundColor = bg, modifier = modifier)
}

@Composable
fun AuditStatusBadge(status: AuditCheckStatus, modifier: Modifier = Modifier) {
    val (fg, bg) = when (status) {
        AuditCheckStatus.PASS -> CyberGreen to CyberGreenContainer
        AuditCheckStatus.WARN -> CyberAmber to CyberAmberContainer
        AuditCheckStatus.FAIL -> CyberCritical to CyberCriticalContainer
        AuditCheckStatus.INFO -> CyberCyan to CyberCyanContainer
    }
    CyberBadge(text = status.label, color = fg, backgroundColor = bg, modifier = modifier)
}

fun riskColors(riskLevel: RiskLevel): Pair<Color, Color> = when (riskLevel) {
    RiskLevel.SECURE -> CyberGreen to CyberGreenContainer
    RiskLevel.LOW -> CyberCyan to CyberCyanContainer
    RiskLevel.MODERATE -> CyberAmber to CyberAmberContainer
    RiskLevel.HIGH -> NeonRedBright to CyberCriticalContainer
    RiskLevel.CRITICAL -> CyberCritical to CyberCriticalContainer
}

@Composable
fun CyberMetricTile(
    label: String,
    value: String,
    unit: String? = null,
    accentColor: Color = NeonRed,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    CyberPanel(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = CrimsonTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MonoMetricLarge,
                color = PureWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!unit.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CyberKeyValueRow(
    label: String,
    value: String,
    valueColor: Color = PureWhite,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = CrimsonTextMuted,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            style = MonoMetricMedium,
            color = valueColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.55f)
        )
    }
}

@Composable
fun PulsingLiveDot(
    active: Boolean,
    color: Color = NeonRed,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "liveDot")
    val alpha by transition.animateFloat(
        initialValue = if (active) 0.25f else 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = if (active) alpha else 0.35f))
    )
}

@Composable
fun PermissionStatusBanner(
    snapshot: WifiPermissionSnapshot,
    onRequestPermissions: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (snapshot.canTriggerWifiScan) return

    val isMissingPerm = snapshot.missingPermissions.isNotEmpty()
    val accent = if (isMissingPerm) NeonRed else CyberAmber

    CyberPanel(
        modifier = modifier.fillMaxWidth(),
        highlighted = isMissingPerm
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = "Permission or radio notice",
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = when {
                    isMissingPerm -> "ANDROID PERMISSION REQUIRED"
                    !snapshot.isLocationServiceEnabled -> "LOCATION SERVICES DISABLED"
                    else -> "WI-FI RADIO DISABLED"
                },
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = snapshot.statusSummary,
            style = MaterialTheme.typography.bodySmall,
            color = PureWhite
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isMissingPerm) {
                Button(
                    onClick = onRequestPermissions,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                    modifier = Modifier.testTag("grant_permissions_button")
                ) {
                    Text("Grant Access")
                }
                OutlinedButton(
                    onClick = onOpenAppSettings,
                    border = BorderStroke(1.dp, CrimsonBorderBright),
                    modifier = Modifier.testTag("open_app_settings_button")
                ) {
                    Text("App Settings", color = PureWhite)
                }
            } else if (!snapshot.isLocationServiceEnabled) {
                Button(
                    onClick = onOpenLocationSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberAmber, contentColor = Color.Black),
                    modifier = Modifier.testTag("open_location_settings_button")
                ) {
                    Text("Enable Location")
                }
            } else if (!snapshot.isWifiEnabled) {
                Button(
                    onClick = onOpenWifiSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                    modifier = Modifier.testTag("open_wifi_settings_button")
                ) {
                    Text("Enable Wi-Fi")
                }
            }
        }
    }
}

@Composable
fun AndroidLimitationDisclosureCard(
    modifier: Modifier = Modifier
) {
    CyberPanel(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = "Android OS Limitations",
                tint = CyberCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ANDROID OS & HARDWARE BOUNDARIES",
                style = MaterialTheme.typography.labelLarge,
                color = CyberCyan
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "• Standard non-rooted Android applications operate within Android's sandbox and cannot enable 802.11 monitor mode, capture raw management frames, or perform packet injection.\n" +
                "• Android strictly blocks ordinary apps from reading arbitrary saved Wi-Fi passphrases or bypassing WPA/WPA2/WPA3 authentication.\n" +
                "• Android 9+ enforces foreground Wi-Fi scan throttling (up to 4 active scans per 2 minutes) and requires Location Services for BSSID/SSID discovery.",
            style = MaterialTheme.typography.bodySmall,
            color = CrimsonTextMuted
        )
    }
}
