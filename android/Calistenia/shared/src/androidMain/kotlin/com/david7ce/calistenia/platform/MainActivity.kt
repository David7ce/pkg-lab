package com.david7ce.calistenia.platform

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import com.david7ce.calistenia.CalisteniaApp
import com.david7ce.calistenia.logic.pickDeviceLanguage
import com.david7ce.shared.ui.isDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = (application as CalisteniaApplication).state
        app.systemLanguage = deviceLanguage()
        enableEdgeToEdge()
        setContent {
            // System bar icons follow the app theme, which can differ from the system one.
            val dark = isDark(app.settings.theme)
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
                )
                onDispose { }
            }
            // A workout keeps the screen on, when the user asked for it in Settings.
            DisposableEffect(app.keepScreenOn) {
                if (app.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            }
            BackHandler(enabled = app.canGoBack) { app.back() }
            CalisteniaApp(app, rememberPlatformUi())
        }
    }

    /** The first of the device preferred languages that the app translates (not only the very first). */
    private fun deviceLanguage(): String {
        val locales = resources.configuration.locales
        return pickDeviceLanguage((0 until locales.size()).map { locales[it].language })
    }

    private companion object {
        val LIGHT_SCRIM = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x12, 0x10, 0x0E)
    }
}
