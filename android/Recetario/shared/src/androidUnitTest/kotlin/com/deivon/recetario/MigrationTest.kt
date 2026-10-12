package com.deivon.recetario

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.deivon.recetario.data.RecipeRepository
import com.deivon.recetario.db.RecipeDatabase
import com.deivon.recetario.logic.ImageChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** A database created by version 1.0.x of the app is upgraded in place and keeps its recipes. */
class MigrationTest {
    @Test
    fun upgradesVersion1Databases() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        listOf(
            """CREATE TABLE recipe (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, image_file TEXT, servings INTEGER,
               prep_minutes INTEGER, cook_minutes INTEGER, source_url TEXT, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)""",
            """CREATE TABLE ingredient (recipe_id TEXT NOT NULL, position INTEGER NOT NULL, name TEXT NOT NULL, quantity REAL,
               unit TEXT, note TEXT, PRIMARY KEY (recipe_id, position))""",
            "CREATE TABLE step (recipe_id TEXT NOT NULL, position INTEGER NOT NULL, body TEXT NOT NULL, PRIMARY KEY (recipe_id, position))",
            "CREATE TABLE tag (recipe_id TEXT NOT NULL, position INTEGER NOT NULL, name TEXT NOT NULL, PRIMARY KEY (recipe_id, position))",
            "CREATE TABLE setting (name TEXT NOT NULL PRIMARY KEY, content TEXT NOT NULL)",
            "INSERT INTO recipe VALUES ('a', 'Vieja', NULL, 2, NULL, 15, NULL, 1, 2)",
            "INSERT INTO step VALUES ('a', 0, 'Remover.')",
        ).forEach { driver.execute(null, it, 0) }

        assertEquals(3L, RecipeDatabase.Schema.version)
        RecipeDatabase.Schema.migrate(driver, 1, RecipeDatabase.Schema.version)

        val repo = RecipeRepository(RecipeDatabase(driver), MemoryImageStore(), { 0L }, { "n" })
        val old = repo.get("a")!!
        assertEquals("Vieja", old.title)
        assertEquals(15, old.cookMinutes)
        assertEquals(listOf("Remover."), old.steps)
        assertNull(old.notes)
        assertNull(old.rating)
        assertEquals(false, old.favorite)

        repo.save(old.copy(notes = "Nota nueva"), ImageChange.Keep)
        assertEquals("Nota nueva", repo.loadAll().single().notes)
        assertEquals(2L, repo.loadAll().single().updatedAt)
        repo.setRating("a", 4)
        repo.setFavorite("a", true)
        val rated = repo.get("a")!!
        assertEquals(4, rated.rating)
        assertEquals(true, rated.favorite)
        assertEquals("Nota nueva", rated.notes, "columns added by different migrations are read in the right order")
    }
}
