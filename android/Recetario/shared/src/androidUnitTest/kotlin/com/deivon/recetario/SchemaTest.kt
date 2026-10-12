package com.deivon.recetario

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.deivon.recetario.db.RecipeDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

class SchemaTest {
    @Test
    fun schemaCreatesAndRoundTrips() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        RecipeDatabase.Schema.create(driver)
        val q = RecipeDatabase(driver).recipeQueries
        q.upsertRecipe("a", "Tortilla", null, 4, 10, 20, null, 1, 2)
        assertEquals("Tortilla", q.selectRecipe("a").executeAsOne().title)
    }
}
