package com.david7ce.calistenia.platform

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.david7ce.calistenia.CalisteniaApp
import com.david7ce.calistenia.data.CalisteniaRepository
import com.david7ce.calistenia.db.CalisteniaDatabase
import com.david7ce.calistenia.ui.AppState
import com.david7ce.shared.ui.isDark
import kotlinx.coroutines.MainScope
import java.io.File
import java.util.Locale

private const val WINDOW_TITLE = "Calistenia"

fun main() {
    val state = createAppState()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = WINDOW_TITLE,
            state = rememberWindowState(size = DpSize(480.dp, 820.dp)),
        ) {
            val dark = isDark(state.settings.theme)
            LaunchedEffect(dark) { applyWindowsTitleBarTheme(WINDOW_TITLE, dark) }
            CalisteniaApp(state, rememberPlatformUi())
        }
    }
}

/** Same wiring as CalisteniaApplication on Android; data lives in %APPDATA%\Calistenia on Windows, ~/.calistenia elsewhere. */
private fun createAppState(): AppState {
    val dataDir = System.getenv("APPDATA")?.let { File(it, "Calistenia") }
        ?: File(System.getProperty("user.home"), ".calistenia")
    dataDir.mkdirs()

    val services = Services(
        clock = LocalClock,
        timeFormat = JvmTimeFormat(),
        // Desktop has no rest notification: the in-app timer is the only signal.
        restNotifier = NoRestNotifier,
    )

    // The schema-aware constructor creates a new database at the latest version, or migrates an
    // existing older one through CalisteniaDatabase.Schema.migrate (same .sqm files Android uses).
    val dbFile = File(dataDir, "calistenia.db")
    val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}", schema = CalisteniaDatabase.Schema)
    val database = CalisteniaDatabase(driver)
    val repository = CalisteniaRepository(database, services.clock, services.newId)

    return AppState(
        services = services,
        repo = repository,
        scope = MainScope(),
        systemLanguage = Locale.getDefault().language,
        appVersion = DESKTOP_VERSION,
    )
}
