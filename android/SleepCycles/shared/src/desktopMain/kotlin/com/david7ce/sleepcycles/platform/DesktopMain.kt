package com.david7ce.sleepcycles.platform

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.david7ce.sleepcycles.SleepCyclesApp
import com.david7ce.sleepcycles.ui.AppState
import com.david7ce.sleepcycles.ui.isDark
import java.util.Locale

private const val WINDOW_TITLE = "SleepCycles"

fun main() {
    val state = AppState(
        services = Services(settings = JavaPreferencesStore(), clock = LocalClock),
        systemLanguage = Locale.getDefault().language,
        appVersion = DESKTOP_VERSION,
    )
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = WINDOW_TITLE,
            state = rememberWindowState(size = DpSize(480.dp, 820.dp)),
        ) {
            val dark = isDark(state.theme)
            LaunchedEffect(dark) { applyWindowsTitleBarTheme(WINDOW_TITLE, dark) }
            SleepCyclesApp(state)
        }
    }
}
