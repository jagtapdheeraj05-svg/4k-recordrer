package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentGold,
    onPrimary = Color(0xFF1E1300),
    primaryContainer = Color(0xFF382900),
    onPrimaryContainer = Color(0xFFFFE088),
    secondary = AccentCyan,
    onSecondary = Color(0xFF003730),
    secondaryContainer = Color(0xFF004F46),
    onSecondaryContainer = Color(0xFF70FCE4),
    tertiary = AccentRed,
    onTertiary = Color.White,
    background = StudioBackground,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = StudioBorder,
    outlineVariant = StudioBorderActive
)

private val LightColorScheme = DarkColorScheme // Cine camera UI stays dark & high contrast for optimal color grading

@Composable
fun CineProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

// Backwards compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    CineProTheme(darkTheme = darkTheme, content = content)
}
