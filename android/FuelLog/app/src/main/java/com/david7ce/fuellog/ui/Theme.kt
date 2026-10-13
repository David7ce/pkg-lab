package com.david7ce.fuellog.ui

import com.david7ce.shared.ui.isDark
import com.david7ce.shared.ui.ThemeMode
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Neutral greys plus a single teal accent. Not dynamic colour: the palette stays restrained.
private val Light = lightColorScheme(
    primary = Color(0xFF0F766E), onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEDE8), onPrimaryContainer = Color(0xFF00201C),
    secondaryContainer = Color(0xFFE4EAE9), onSecondaryContainer = Color(0xFF1A1C1C),
    background = Color(0xFFF8F9F9), onBackground = Color(0xFF191C1C),
    surface = Color(0xFFF8F9F9), onSurface = Color(0xFF191C1C),
    surfaceVariant = Color(0xFFE2E6E5), onSurfaceVariant = Color(0xFF555D5C),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF2F4F4),
    surfaceContainer = Color(0xFFECEFEE), surfaceContainerHigh = Color(0xFFE6E9E9),
    surfaceContainerHighest = Color(0xFFE0E3E3),
    outline = Color(0xFF7E8786), outlineVariant = Color(0xFFD3D9D8),
    error = Color(0xFFB3261E),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF5FD4C4), onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF0E4F48), onPrimaryContainer = Color(0xFFB8F1E8),
    secondaryContainer = Color(0xFF2A302F), onSecondaryContainer = Color(0xFFE0E4E3),
    background = Color(0xFF111414), onBackground = Color(0xFFE1E3E3),
    surface = Color(0xFF111414), onSurface = Color(0xFFE1E3E3),
    surfaceVariant = Color(0xFF3C4443), onSurfaceVariant = Color(0xFFBDC5C4),
    surfaceContainerLowest = Color(0xFF0B0E0E), surfaceContainerLow = Color(0xFF181B1B),
    surfaceContainer = Color(0xFF1D2020), surfaceContainerHigh = Color(0xFF272B2A),
    surfaceContainerHighest = Color(0xFF323535),
    outline = Color(0xFF879190), outlineVariant = Color(0xFF3C4443),
    error = Color(0xFFF2B8B5),
)

@Composable
fun FuelTheme(theme: Int, content: @Composable () -> Unit) {
    val dark = isDark(ThemeMode.fromCode(theme))
    val context = LocalContext.current
    DisposableEffect(dark) {
        val style = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark }
        (context as? ComponentActivity)?.enableEdgeToEdge(style, style)
        onDispose {}
    }
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
