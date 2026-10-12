package com.deivon.recetario.platform

import android.app.Application
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.deivon.recetario.db.RecipeDatabase

class RecetarioApplication : Application() {
    val database: RecipeDatabase by lazy {
        RecipeDatabase(AndroidSqliteDriver(RecipeDatabase.Schema, this, "recetario.db"))
    }
}
