package com.david7ce.sleepcycles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.david7ce.shared.ui.LocalSharedStrings
import com.david7ce.shared.ui.SharedStrings
import com.david7ce.sleepcycles.ui.AppState
import com.david7ce.sleepcycles.ui.HelpDialog
import com.david7ce.sleepcycles.ui.LocalStrings
import com.david7ce.sleepcycles.ui.MainScreen
import com.david7ce.sleepcycles.ui.Screen
import com.david7ce.sleepcycles.ui.SettingsScreen
import com.david7ce.sleepcycles.ui.SleepCyclesTheme
import com.david7ce.sleepcycles.ui.isDark

/** Root of the shared UI. Each platform builds an [AppState], handles back, and calls this. */
@Composable
fun SleepCyclesApp(app: AppState) {
    val strings = app.strings
    CompositionLocalProvider(
        LocalStrings provides strings,
        // Text for the shared components (android/shared), in the app chosen language.
        LocalSharedStrings provides SharedStrings(
            cancel = strings.cancel, back = strings.back, close = strings.close,
            delete = strings.delete, selectAll = strings.selectAll,
        ),
    ) {
        SleepCyclesTheme(isDark(app.theme)) {
            when (app.screen) {
                Screen.Main -> MainScreen(app)
                Screen.Settings -> SettingsScreen(app)
            }
            if (app.showHelp) HelpDialog(onDismiss = app::closeHelp)
        }
    }
}
