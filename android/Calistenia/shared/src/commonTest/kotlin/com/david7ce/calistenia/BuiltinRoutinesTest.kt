package com.david7ce.calistenia

import com.david7ce.calistenia.logic.BuiltinExercises
import com.david7ce.calistenia.logic.BuiltinRoutines
import com.david7ce.calistenia.logic.isValid
import com.david7ce.calistenia.logic.toDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BuiltinRoutinesTest {

    @Test
    fun thereAreFiveTemplatesReferencingRealExercises() {
        val ids = BuiltinExercises.all.map { it.id }.toSet()
        assertEquals(5, BuiltinRoutines.all.size)
        assertEquals(5, BuiltinRoutines.all.map { it.key }.toSet().size)

        var key = 0
        BuiltinRoutines.all.forEach { template ->
            assertTrue(template.exercises.isNotEmpty(), template.key)
            template.exercises.forEach { line ->
                assertTrue(line.exerciseId in ids, "${template.key} references ${line.exerciseId}")
                assertTrue(line.sets in 1..20, template.key)
                assertTrue(line.restSeconds in 0..600, template.key)
            }
            val draft = assertNotNull(
                template.toDraft(BuiltinExercises.all, "es") { "key-${key++}" },
                template.key,
            )
            assertTrue(draft.isValid, template.key)
            assertEquals(template.exercises.size, draft.exercises.size, template.key)
        }
    }

    @Test
    fun namesAndNotesExistInEveryLanguageWithEnglishFallback() {
        val languages = listOf("es", "fr", "de", "pt", "it")
        BuiltinRoutines.all.forEach { template ->
            languages.forEach { language ->
                assertTrue(template.name(language).length > 2, "${template.key} $language name")
                assertTrue(template.notes(language).length > 20, "${template.key} $language notes")
            }
            assertEquals(template.name, template.name("en"))
            assertEquals(template.notes, template.notes("ja"))
        }
    }

    @Test
    fun draftsUseTheMeasureOfEachExercise() {
        val morning = BuiltinRoutines.all.first { it.key == "morning" }
        val draft = assertNotNull(morning.toDraft(BuiltinExercises.all, "es") { "k" })

        val plank = draft.exercises.first { it.exercise.id == "builtin:plank" }
        assertNull(plank.targetReps)
        assertEquals(30, plank.targetSeconds)

        val squat = draft.exercises.first { it.exercise.id == "builtin:squat" }
        assertEquals(12, squat.targetReps)
        assertNull(squat.targetSeconds)
    }

    @Test
    fun missingExercisesAreSkippedAndAnEmptyCatalogGivesNoDraft() {
        val template = BuiltinRoutines.all.first()
        assertNull(template.toDraft(emptyList(), "es") { "k" })

        val onlySquat = listOf(BuiltinExercises.find("builtin:squat")!!)
        val partial = assertNotNull(template.toDraft(onlySquat, "es") { "k" })
        assertEquals(1, partial.exercises.size)
        assertEquals("builtin:squat", partial.exercises.single().exercise.id)
    }
}
