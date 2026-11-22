package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.presentation.i18n.AppLanguage
import com.example.presentation.i18n.ArabicStrings
import com.example.presentation.i18n.EnglishStrings
import com.example.presentation.i18n.LocalAppStrings

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
    language: AppLanguage = AppLanguage.ARABIC,
    content: @Composable () -> Unit
) {
    val isArabic = language == AppLanguage.ARABIC
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    val strings = if (isArabic) ArabicStrings else EnglishStrings

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalAppStrings provides strings
    ) {
        MaterialTheme(
            colorScheme = CyberDarkColorScheme,
            typography = buildAppTypography(isArabic = isArabic),
            shapes = CyberShapes,
            content = content
        )
    }
}
