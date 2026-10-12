package com.deivon.recetario.platform

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.deivon.recetario.RecetarioApp
import com.deivon.recetario.data.RecipeRepository
import com.deivon.recetario.db.RecipeDatabase
import com.deivon.recetario.ui.AppState
import kotlinx.coroutines.MainScope
import java.io.File
import java.util.Locale

fun main() {
    val state = createAppState()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Recetario",
            state = rememberWindowState(size = DpSize(1100.dp, 800.dp)),
        ) {
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

    // A new database gets the latest schema. Upgrading an existing desktop database will need a
    // migration step here when the schema changes (Android already has the migrations in androidMain).
    val dbFile = File(dataDir, "recetario.db")
    val isNew = !dbFile.exists() // checked before opening: opening a SQLite file creates it
    val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
    if (isNew) RecipeDatabase.Schema.create(driver)
    val database = RecipeDatabase(driver)

    val repository = RecipeRepository(database, services.images, services.clock, services.newId)
    return AppState(services, repository, MainScope(), Locale.getDefault().language).also {
        it.systemLanguage = Locale.getDefault().language
    }
}
