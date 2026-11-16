package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class DataGuardExtendedColors(
    val shieldActive: Color,
    val shieldActiveGlow: Color,
    val wifiAccent: Color,
    val cellularWarning: Color,
    val blockedCrimson: Color,
    val cardBorder: Color,
    val surfaceGlass: Color
)

val LocalDataGuardColors = staticCompositionLocalOf {
    DataGuardExtendedColors(
        shieldActive = EmeraldPrimary,
        shieldActiveGlow = EmeraldGlow,
        wifiAccent = ArcticCyan,
        cellularWarning = AmberWarning,
        blockedCrimson = CrimsonAlert,
        cardBorder = SlateOutline,
        surfaceGlass = SlateSurfaceElevated
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldGlow,
    secondary = ArcticCyan,
    onSecondary = ArcticOnCyan,
    secondaryContainer = ArcticCyanContainer,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = CrimsonAlert,
    onTertiary = TextPrimaryDark,
    tertiaryContainer = CrimsonContainer,
    onTertiaryContainer = Color(0xFFFFD9DF),
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = SlateSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLow = SlateSurface,
    surfaceContainer = SlateSurfaceElevated,
    surfaceContainerHigh = SlateSurfaceHighest,
    outline = SlateOutline,
    outlineVariant = SlateOutlineVariant,
    error = CrimsonAlert,
    onError = TextPrimaryDark,
    errorContainer = CrimsonContainer
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldLightPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldLightContainer,
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = CyanLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = CyanLightContainer,
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = CrimsonAlert,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE4E6),
    onTertiaryContainer = Color(0xFF881337),
    background = LightBg,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = CrimsonAlert,
    onError = Color.White
)

private val DataGuardShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun DataGuardTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) {
        DataGuardExtendedColors(
            shieldActive = EmeraldPrimary,
            shieldActiveGlow = EmeraldGlow,
            wifiAccent = ArcticCyan,
            cellularWarning = AmberWarning,
            blockedCrimson = CrimsonAlert,
            cardBorder = SlateOutline,
            surfaceGlass = SlateSurfaceElevated
        )
    } else {
        DataGuardExtendedColors(
            shieldActive = EmeraldLightPrimary,
            shieldActiveGlow = EmeraldPrimary,
            wifiAccent = CyanLightSecondary,
            cellularWarning = Color(0xFFD97706),
            blockedCrimson = Color(0xFFE11D48),
            cardBorder = Color(0xFFCBD5E1),
            surfaceGlass = LightSurface
        )
    }

    CompositionLocalProvider(LocalDataGuardColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = DataGuardShapes,
            content = content
        )
    }
}
