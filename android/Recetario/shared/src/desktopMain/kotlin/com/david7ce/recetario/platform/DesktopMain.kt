package com.david7ce.recetario.platform

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.david7ce.recetario.RecetarioApp
import com.david7ce.recetario.data.RecipeRepository
import com.david7ce.recetario.db.RecipeDatabase
import com.david7ce.recetario.ui.AppState
import com.david7ce.recetario.ui.isDark
import kotlinx.coroutines.MainScope
import java.io.File
import java.util.Locale

private const val WINDOW_TITLE = "Recetario"

fun main() {
    val state = createAppState()
    state.checkForUpdates("desktop-recetario-latest", DESKTOP_VERSION)
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = WINDOW_TITLE,
            state = rememberWindowState(size = DpSize(1100.dp, 800.dp)),
        ) {
            val dark = isDark(state.themeMode)
            LaunchedEffect(dark) { applyWindowsTitleBarTheme(WINDOW_TITLE, dark) }
            RecetarioApp(state, rememberPlatformUi())
        }
    }
}

/** Same wiring as RecetarioApplication on Android; data lives in %APPDATA%\Recetario on Windows, ~/.recetario elsewhere. */
private fun createAppState(): AppState {
    val dataDir = System.getenv("APPDATA")?.let { File(it, "Recetario") }
        ?: File(System.getProperty("user.home"), ".recetario")
    dataDir.mkdirs()

    val services = Services(
        images = FileImageStore(File(dataDir, "images")),
        codec = DesktopImageCodec(),
        zip = JvmZipCodec(),
        web = HttpWebClient("Mozilla/5.0 (Recetario desktop) Recetario/1.0 (personal recipe import)"),
        pdf = null, // PDF export is Android-only for now; the UI hides the option when this is null.
        clock = System::currentTimeMillis,
    )

    // The schema-aware constructor creates a new database at the latest version, or migrates an
    // existing older one through RecipeDatabase.Schema.migrate (same .sqm files Android uses).
    val dbFile = File(dataDir, "recetario.db")
    val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}", schema = RecipeDatabase.Schema)
    val database = RecipeDatabase(driver)

    val repository = RecipeRepository(database, services.images, services.clock, services.newId)
    return AppState(services, repository, MainScope(), Locale.getDefault().language).also {
        it.systemLanguage = Locale.getDefault().language
    }
}
