package com.david7ce.shared.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

/**
 * The light/dark choice every app offers in Settings. [code] is what FuelLog and VozReves already store
 * (0 system, 1 light, 2 dark), so adopting this type changes no saved setting; Recetario and SleepCycles
 * store [name], which is the same set of words they used before.
 */
enum class ThemeMode(val code: Int) {
    System(0),
    Light(1),
    Dark(2);

    companion object {
        /** Unknown codes (a newer or corrupt value) fall back to following the system. */
        fun fromCode(code: Int): ThemeMode = entries.firstOrNull { it.code == code } ?: System
    }
}

/** Whether the app should draw its dark palette for [mode]; System follows the device. */
@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}
