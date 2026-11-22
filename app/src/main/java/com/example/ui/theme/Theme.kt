package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val CyberDarkColorScheme = darkColorScheme(
    primary = NeonRed,
    onPrimary = PureWhite,
    primaryContainer = CrimsonDeep,
    onPrimaryContainer = PureWhite,
    secondary = NeonRedBright,
    onSecondary = PureBlack,
    secondaryContainer = CrimsonMuted,
    onSecondaryContainer = PureWhite,
    tertiary = NeonRedGlow,
    onTertiary = PureBlack,
    tertiaryContainer = CrimsonPanelHighest,
    onTertiaryContainer = PureWhite,
    background = PureBlack,
    onBackground = PureWhite,
    surface = ObsidianSurface,
    onSurface = PureWhite,
    surfaceVariant = CrimsonPanel,
    onSurfaceVariant = CrimsonTextMuted,
    surfaceContainerLowest = PureBlack,
    surfaceContainerLow = CrimsonPanelLow,
    surfaceContainer = CrimsonPanel,
    surfaceContainerHigh = CrimsonPanelHigh,
    surfaceContainerHighest = CrimsonPanelHighest,
    outline = CrimsonBorderBright,
    outlineVariant = CrimsonBorder,
    error = CyberCritical,
    onError = PureWhite,
    errorContainer = CyberCriticalContainer,
    onErrorContainer = NeonRedGlow
)

private val CyberShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun WifiToolsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberDarkColorScheme,
        typography = Typography,
        shapes = CyberShapes,
        content = content
    )
}
