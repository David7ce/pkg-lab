package com.david7ce.recetario.logic

import com.david7ce.recetario.recipe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImportPlanTest {
    private val mine = recipe("a", title = "Tortilla", updatedAt = 10)
    private val existing = mapOf(mine.id to mine)

    @Test
    fun sortsIncomingIntoAddedIdenticalAndConflicts() {
        val incoming = listOf(
            mine.copy(updatedAt = 99, imageFile = "img9.jpg"), // same content
            recipe("b"),
        )
        val plan = ImportPlan.of(existing, incoming)
        assertEquals(listOf("b"), plan.added.map { it.id })
        assertEquals(listOf("a"), plan.identical.map { it.id })
        assertTrue(plan.conflicts.isEmpty())

        val conflicting = ImportPlan.of(existing, listOf(mine.copy(title = "Tortilla de la abuela")))
        assertEquals(1, conflicting.conflicts.size)
        assertTrue(ImportPlan.of(existing, listOf(mine)).isEmpty)
    }

    @Test
    fun conflictPoliciesNeverOverwriteSilently() {
        val theirs = mine.copy(title = "Otra versión", createdAt = 500)
        val plan = ImportPlan.of(existing, listOf(theirs, recipe("b")))
        var n = 0
        val ids = { "new${++n}" }

        val keep = plan.resolve(ConflictPolicy.KeepMine, existing, ids, now = 1000)
        assertEquals(listOf("b"), keep.insert.map { it.id })
        assertTrue(keep.replace.isEmpty())

        val copies = plan.resolve(ConflictPolicy.ImportAsCopies, existing, ids, now = 1000)
        assertEquals(listOf("b", "new1"), copies.insert.map { it.id })
        assertEquals("Otra versión", copies.insert[1].title)
        assertTrue(copies.replace.isEmpty())

        val replace = plan.resolve(ConflictPolicy.ReplaceMine, existing, ids, now = 1000)
        assertEquals(listOf("a"), replace.replace.map { it.id })
        assertEquals(mine.createdAt, replace.replace.single().createdAt)
        assertEquals(1000, replace.replace.single().updatedAt)
    }
}
