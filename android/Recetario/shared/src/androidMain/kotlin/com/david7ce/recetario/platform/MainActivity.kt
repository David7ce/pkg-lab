package com.david7ce.recetario.platform

import com.david7ce.shared.ui.isDark
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import com.david7ce.recetario.RecetarioApp
import com.david7ce.recetario.ui.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = (application as RecetarioApplication).state
        app.systemLanguage = resources.configuration.locales[0].language
        enableEdgeToEdge()
        setContent {
            // System bar icons follow the app theme, which can differ from the system one.
            val dark = isDark(app.themeMode)
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
                )
                onDispose { }
            }
            // Cooking mode keeps the screen on; hands are usually busy.
            val cooking = app.screen is Screen.Cooking
            DisposableEffect(cooking) {
                if (cooking) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                onDispose { }
            }
            BackHandler(enabled = app.canGoBack) { app.back() }
            RecetarioApp(app, rememberPlatformUi())
        }
    }

    private companion object {
        val LIGHT_SCRIM = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
