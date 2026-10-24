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
}
