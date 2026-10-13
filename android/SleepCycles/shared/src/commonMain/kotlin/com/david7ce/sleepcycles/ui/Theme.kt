package com.david7ce.sleepcycles.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.david7ce.sleepcycles.logic.ThemeMode

/** Mode accents from the PWA: warm sand for "wake up at", pale lavender for "go to bed at". */
val WakeAccent = Color(0xFFDFB987)
val BedAccent = Color(0xFFE2DDF8)

// Night palette taken from the PWA stylesheet.
private val Dark = darkColorScheme(
    primary = Color(0xFFB3A5F2),
    onPrimary = Color(0xFF1A114F),
    primaryContainer = Color(0xFF2C2170),
    onPrimaryContainer = Color(0xFFECE8FB),
    secondary = Color(0xFFB3A5F2),
    onSecondary = Color(0xFF1A114F),
    secondaryContainer = Color(0xFF3A2E8C),
    onSecondaryContainer = Color(0xFFECE8FB),
    tertiary = WakeAccent,
    background = Color(0xFF0A0920),
    onBackground = Color(0xFFECE8FB),
    surface = Color(0xFF140E3D),
    onSurface = Color(0xFFECE8FB),
    surfaceVariant = Color(0xFF251A63),
    onSurfaceVariant = Color(0xFFBEB5E3),
    surfaceContainerLowest = Color(0xFF0A0920),
    surfaceContainerLow = Color(0xFF140E3D),
    surfaceContainer = Color(0xFF1A114F),
    surfaceContainerHigh = Color(0xFF251A63),
    surfaceContainerHighest = Color(0xFF2C2170),
    outline = Color(0xFF8A80C0),
    outlineVariant = Color(0xFF453A86),
    error = Color(0xFFF08A8A),
)

private val Light = lightColorScheme(
    primary = Color(0xFF5B4BB8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5DEFF),
    onPrimaryContainer = Color(0xFF1A0B5E),
    secondary = Color(0xFF5B4BB8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5DEFF),
    onSecondaryContainer = Color(0xFF1A0B5E),
    tertiary = Color(0xFF8A5A12),
    background = Color(0xFFFCF8FF),
    onBackground = Color(0xFF1B1730),
    surface = Color(0xFFFCF8FF),
    onSurface = Color(0xFF1B1730),
    surfaceVariant = Color(0xFFE6E0F5),
    onSurfaceVariant = Color(0xFF48435C),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F1FF),
    surfaceContainer = Color(0xFFF0EAFB),
    surfaceContainerHigh = Color(0xFFEAE4F6),
    surfaceContainerHighest = Color(0xFFE4DEF0),
    outline = Color(0xFF79728F),
    outlineVariant = Color(0xFFCAC3DC),
    error = Color(0xFFBA1A1A),
)

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

@Composable
fun SleepCyclesTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
