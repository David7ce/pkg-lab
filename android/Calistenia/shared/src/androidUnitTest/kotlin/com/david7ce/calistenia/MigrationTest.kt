package com.david7ce.calistenia

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.david7ce.calistenia.data.CalisteniaRepository
import com.david7ce.calistenia.db.CalisteniaDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A database created before descriptions existed is upgraded and gets the built-in how-to text. */
class MigrationTest {

    @Test
    fun addsDescriptionsToVersion1Databases() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(
            null,
            """CREATE TABLE exercise (
                id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, muscleGroup TEXT NOT NULL,
                measure TEXT NOT NULL, builtin INTEGER NOT NULL DEFAULT 0, createdAt INTEGER NOT NULL
            )""",
            0,
        )
        driver.execute(
            null,
            "INSERT INTO exercise VALUES ('builtin:push-up', 'Push-up', 'Push', 'Reps', 1, 1)",
            0,
        )

        assertEquals(5L, CalisteniaDatabase.Schema.version)
        CalisteniaDatabase.Schema.migrate(driver, 1, CalisteniaDatabase.Schema.version)

        val repo = CalisteniaRepository(
            db = CalisteniaDatabase(driver),
            clock = { 2_000L },
            newId = { "id" },
        )
        // Seeding both inserts the missing exercises and refreshes the description of the old row.
        repo.seedBuiltins()

        val pushUp = repo.exercises().first { it.id == "builtin:push-up" }
        assertTrue(pushUp.description.length >= 100, "the migrated row should receive the how-to text")
        assertEquals(47, repo.exercises().size)
        assertTrue(repo.exercises().all { it.description.isNotBlank() })
        assertNull(pushUp.parentId)
        assertEquals(
            "builtin:push-up",
            repo.exercises().first { it.id == "builtin:knee-push-up" }.parentId,
        )
    }
}
