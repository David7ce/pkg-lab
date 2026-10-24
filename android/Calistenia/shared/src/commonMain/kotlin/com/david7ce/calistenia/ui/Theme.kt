package com.david7ce.calistenia.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Chalk and steel: warm off-white paper in light, near-black blue-grey at night, one orange
// accent that means "work". Both palettes define every token the screens use, so a mode swap never
// leaves unreadable content behind.
private val Light = lightColorScheme(
    primary = Color(0xFFB9441A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC8),
    onPrimaryContainer = Color(0xFF3A1000),
    secondary = Color(0xFF4E6159),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E5DB),
    onSecondaryContainer = Color(0xFF0B1F18),
    tertiary = Color(0xFF2F6B7A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB4EBFA),
    onTertiaryContainer = Color(0xFF001F27),
    background = Color(0xFFFAF7F2),
    onBackground = Color(0xFF1C1917),
    surface = Color(0xFFFAF7F2),
    onSurface = Color(0xFF1C1917),
    surfaceVariant = Color(0xFFE9E1D8),
    onSurfaceVariant = Color(0xFF4A423A),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF5F0E9),
    surfaceContainer = Color(0xFFEFE9E1),
    surfaceContainerHigh = Color(0xFFE9E2D9),
    surfaceContainerHighest = Color(0xFFE3DBD1),
    outline = Color(0xFF7D746B),
    outlineVariant = Color(0xFFCCC2B8),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB68F),
    onPrimary = Color(0xFF5A1B00),
    primaryContainer = Color(0xFF7E2E0A),
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFFB7CCC0),
    onSecondary = Color(0xFF22352B),
    secondaryContainer = Color(0xFF394C41),
    onSecondaryContainer = Color(0xFFD3E5DB),
    tertiary = Color(0xFF8FD1E0),
    onTertiary = Color(0xFF003640),
    tertiaryContainer = Color(0xFF104E5A),
    onTertiaryContainer = Color(0xFFB4EBFA),
    background = Color(0xFF12100E),
    onBackground = Color(0xFFEDE7E1),
    surface = Color(0xFF12100E),
    onSurface = Color(0xFFEDE7E1),
    surfaceVariant = Color(0xFF4A423A),
    onSurfaceVariant = Color(0xFFD0C5BA),
    surfaceContainerLowest = Color(0xFF0D0B09),
    surfaceContainerLow = Color(0xFF1C1917),
    surfaceContainer = Color(0xFF201C19),
    surfaceContainerHigh = Color(0xFF2C2620),
    surfaceContainerHighest = Color(0xFF382F27),
    outline = Color(0xFF998E83),
    outlineVariant = Color(0xFF4A423A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun CalisteniaTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
