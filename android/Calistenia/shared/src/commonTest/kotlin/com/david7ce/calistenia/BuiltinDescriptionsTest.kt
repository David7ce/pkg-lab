package com.david7ce.calistenia

import com.david7ce.calistenia.logic.BuiltinDescriptions
import com.david7ce.calistenia.logic.BuiltinExercises
import com.david7ce.calistenia.logic.descriptionFor
import com.david7ce.calistenia.model.Exercise
import com.david7ce.calistenia.model.Measure
import com.david7ce.calistenia.model.MuscleGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BuiltinDescriptionsTest {

    private val translatedLanguages = listOf("es", "fr", "de", "pt", "it")

    @Test
    fun everyBuiltinHasEveryTranslation() {
        BuiltinExercises.all.forEach { exercise ->
            translatedLanguages.forEach { language ->
                val text = BuiltinDescriptions.text(exercise.id, language)
                assertNotNull(text, "${exercise.id} has no $language description")
                assertTrue(text.length >= 100, "${exercise.id} $language description is too short")
            }
        }
    }

    @Test
    fun resolvesTheAppLanguageAndFallsBackToEnglish() {
        val pushUp = BuiltinExercises.find("builtin:push-up")!!
        assertEquals(BuiltinDescriptions.text(pushUp.id, "es"), pushUp.descriptionFor("es"))
        assertEquals(BuiltinDescriptions.text(pushUp.id, "pt"), pushUp.descriptionFor("pt-BR"))
        assertEquals(BuiltinDescriptions.text(pushUp.id, "de"), pushUp.descriptionFor("DE"))
        assertEquals(pushUp.description, pushUp.descriptionFor("en"))
        assertEquals(pushUp.description, pushUp.descriptionFor("ja"))
    }

    @Test
    fun customExercisesKeepTheTextTheUserWrote() {
        val custom = Exercise(
            id = "custom:x",
            name = "Lever row",
            group = MuscleGroup.Pull,
            measure = Measure.Reps,
            builtin = false,
            description = "Mi nota",
        )
        assertEquals("Mi nota", custom.descriptionFor("es"))
        assertEquals("Mi nota", custom.descriptionFor("ja"))
    }
}
