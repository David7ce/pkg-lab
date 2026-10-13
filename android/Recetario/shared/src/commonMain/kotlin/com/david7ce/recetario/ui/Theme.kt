package com.david7ce.recetario.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode { System, Light, Dark }

// Warm terracotta palette (kitchen, not office); contrast checked for text on containers.
private val Light = lightColorScheme(
    primary = Color(0xFF9A4521),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCD),
    onPrimaryContainer = Color(0xFF380D00),
    secondary = Color(0xFF6E5B2E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF8E0A6),
    onSecondaryContainer = Color(0xFF241A00),
    tertiary = Color(0xFF4F6538),
    onTertiary = Color.White,
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF231A16),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF231A16),
    surfaceVariant = Color(0xFFF4DED5),
    onSurfaceVariant = Color(0xFF53433D),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF1EB),
    surfaceContainer = Color(0xFFFCEAE3),
    surfaceContainerHigh = Color(0xFFF6E5DD),
    surfaceContainerHighest = Color(0xFFF0DFD7),
    outline = Color(0xFF85736C),
    outlineVariant = Color(0xFFD8C2BA),
    error = Color(0xFFBA1A1A),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB597),
    onPrimary = Color(0xFF5A1B00),
    primaryContainer = Color(0xFF7B2F0B),
    onPrimaryContainer = Color(0xFFFFDBCD),
    secondary = Color(0xFFDBC48C),
    onSecondary = Color(0xFF3C2E04),
    secondaryContainer = Color(0xFF544418),
    onSecondaryContainer = Color(0xFFF8E0A6),
    tertiary = Color(0xFFB5CF98),
    onTertiary = Color(0xFF223510),
    background = Color(0xFF1A110E),
    onBackground = Color(0xFFF1DFD8),
    surface = Color(0xFF1A110E),
    onSurface = Color(0xFFF1DFD8),
    surfaceVariant = Color(0xFF53433D),
    onSurfaceVariant = Color(0xFFD8C2BA),
    surfaceContainerLowest = Color(0xFF140C09),
    surfaceContainerLow = Color(0xFF231A16),
    surfaceContainer = Color(0xFF271E1A),
    surfaceContainerHigh = Color(0xFF322824),
    surfaceContainerHighest = Color(0xFF3D332E),
    outline = Color(0xFFA08D86),
    outlineVariant = Color(0xFF53433D),
    error = Color(0xFFFFB4AB),
)

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

@Composable
fun RecetarioTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
