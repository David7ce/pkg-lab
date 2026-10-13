package com.david7ce.sleepcycles.platform

import com.david7ce.shared.ui.isDark
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import com.david7ce.sleepcycles.SleepCyclesApp
import com.david7ce.sleepcycles.logic.pickDeviceLanguage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = (application as SleepCyclesApplication).state
        app.systemLanguage = deviceLanguage()
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

    /** The shortcut used while the app is already open: reuse this instance (launchMode singleTop). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == ACTION_SLEEP_NOW) (application as SleepCyclesApplication).state.sleepNow()
    }

    /** The first of the device preferred languages that the app translates (not only the very first). */
    private fun deviceLanguage(): String {
        val locales = resources.configuration.locales
        return pickDeviceLanguage((0 until locales.size()).map { locales[it].language })
    }

    companion object {
        const val ACTION_SLEEP_NOW = "com.david7ce.sleepcycles.SLEEP_NOW"
        private val LIGHT_SCRIM = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        private val DARK_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
