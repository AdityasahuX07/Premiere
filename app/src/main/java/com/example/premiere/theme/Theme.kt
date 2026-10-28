package com.example.premiere.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PremiereDarkColorScheme = darkColorScheme(
    primary = AccentGold,
    onPrimary = CinematicBlack,
    primaryContainer = Color(0xFF3A2E00),
    onPrimaryContainer = AccentGold,
    secondary = AccentBlue,
    onSecondary = CinematicBlack,
    secondaryContainer = Color(0xFF002B6E),
    onSecondaryContainer = AccentBlue,
    tertiary = TextTertiary,
    onTertiary = CinematicBlack,
    background = CinematicBlack,
    onBackground = TextPrimary,
    surface = DeepCharcoal,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceGlass,
    onSurfaceVariant = TextSecondary,
    surfaceTint = AccentGold,
    outline = GlassBorder,
    outlineVariant = GlassBorder2,
    scrim = Color(0xCC000000),
    error = AccentRed,
    onError = TextPrimary,
)

@Composable
fun PremiereTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PremiereDarkColorScheme,
        typography = PremiereTypography,
        content = content
    )
}
