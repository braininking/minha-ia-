package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = BackgroundDark,
    primaryContainer = DarkGreen,
    onPrimaryContainer = NeonGreen,
    secondary = EmeraldGreen,
    onSecondary = BackgroundDark,
    secondaryContainer = DarkGreen,
    onSecondaryContainer = TextPrimary,
    tertiary = InfoCyan,
    onTertiary = BackgroundDark,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = OutlineGreen,
    error = ErrorRed,
    onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // Permanent deep sci-fi dark theme with neon emerald accents
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
