package com.david7ce.calistenia

import com.david7ce.calistenia.logic.BuiltinExercises
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BuiltinExercisesTest {

    @Test
    fun catalogIsBigEnoughToStartWith() {
        assertTrue(BuiltinExercises.all.size >= 30, "got ${BuiltinExercises.all.size}")
    }

    @Test
    fun idsAndNamesAreUniqueAndStable() {
        val ids = BuiltinExercises.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate id")
        assertEquals(ids.size, BuiltinExercises.all.map { it.name }.toSet().size, "duplicate name")
        ids.forEach { assertTrue(it.startsWith("builtin:"), it) }
    }

    @Test
    fun everyEntryIsComplete() {
        BuiltinExercises.all.forEach { exercise ->
            assertTrue(exercise.name.isNotBlank(), exercise.id)
            assertTrue(exercise.builtin, exercise.id)
            assertTrue(exercise.id.removePrefix("builtin:").isNotBlank(), exercise.id)
            assertTrue(exercise.description.length >= 100, "${exercise.id} needs a detailed description")
        }
    }

    @Test
    fun lookupByStableId() {
        val pushUp = BuiltinExercises.find("builtin:push-up")
        assertNotNull(pushUp)
        assertEquals("Push-up", pushUp.name)
        assertNull(BuiltinExercises.find("builtin:not-there"))
    }

    @Test
    fun variantsPointAtAnExistingTopLevelExerciseInTheSameGroup() {
        val byId = BuiltinExercises.all.associateBy { it.id }
        val topLevel = BuiltinExercises.all.filter { it.isTopLevel }
        assertEquals(16, topLevel.size, "expected sixteen families/singles")
        assertEquals(40, BuiltinExercises.all.size)

        BuiltinExercises.all.filterNot { it.isTopLevel }.forEach { variant ->
            val parentId = assertNotNull(variant.parentId, "${variant.id} has no parent")
            val parent = assertNotNull(byId[parentId], "${variant.id} points at a missing parent")
            assertTrue(parent.isTopLevel, "${variant.id} points at another variant")
            assertEquals(parent.group, variant.group, "${variant.id} changes group inside its family")
        }
    }

    @Test
    fun familiesCoverEveryVariantOnce() {
        val ids = mutableSetOf<String>()
        BuiltinExercises.all.filter { it.isTopLevel }.forEach { parent ->
            ids += parent.id
            BuiltinExercises.all.filter { it.parentId == parent.id }.forEach { ids += it.id }
        }
        assertEquals(BuiltinExercises.all.map { it.id }.toSet(), ids)
    }
}
