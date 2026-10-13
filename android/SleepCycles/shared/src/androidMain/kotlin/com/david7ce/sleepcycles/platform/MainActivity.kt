package com.david7ce.sleepcycles.platform

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import com.david7ce.sleepcycles.SleepCyclesApp
import com.david7ce.sleepcycles.ui.isDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = (application as SleepCyclesApplication).state
        app.systemLanguage = resources.configuration.locales[0].language
        // The launcher shortcut. Only on a fresh start: a recreation (rotation) must not repeat it.
        if (savedInstanceState == null && intent?.action == ACTION_SLEEP_NOW) app.sleepNow()
        val platform = AndroidPlatformUi(this) { app.strings.noClockApp }
        enableEdgeToEdge()
        setContent {
            // System bar icons follow the app theme, which can differ from the system one.
            val dark = isDark(app.theme)
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
                )
                onDispose { }
            }
            BackHandler(enabled = app.canGoBack) { app.back() }
            SleepCyclesApp(app, platform)
        }
    }

    companion object {
        const val ACTION_SLEEP_NOW = "com.david7ce.sleepcycles.SLEEP_NOW"
        private val LIGHT_SCRIM = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        private val DARK_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
