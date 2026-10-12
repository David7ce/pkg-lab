package com.david7ce.recetario.platform

import android.app.Application
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.david7ce.recetario.data.RecipeRepository
import com.david7ce.recetario.db.RecipeDatabase
import com.david7ce.recetario.ui.AppState
import kotlinx.coroutines.MainScope
import java.io.File
import java.util.Locale

/** Builds the Android services once per process; the shared [AppState] outlives activity recreation. */
class RecetarioApplication : Application() {
    val state: AppState by lazy {
        val services = Services(
            images = FileImageStore(File(filesDir, "images")),
            codec = AndroidImageCodec(),
            zip = JvmZipCodec(),
            web = HttpWebClient("Mozilla/5.0 (Linux; Android) Recetario/1.0 (personal recipe import)"),
            pdf = AndroidPdfExporter(),
            clock = System::currentTimeMillis,
        )
        // The driver opens the database lazily, on the first query (a background thread).
        val database = RecipeDatabase(AndroidSqliteDriver(RecipeDatabase.Schema, this, "recetario.db"))
        val repository = RecipeRepository(database, services.images, services.clock, services.newId)
        AppState(services, repository, MainScope(), Locale.getDefault().language).also {
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            if (versionName != null) it.checkForUpdates("android-Recetario-latest", versionName)
        }
    }
}
