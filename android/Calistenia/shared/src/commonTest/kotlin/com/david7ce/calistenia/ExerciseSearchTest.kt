package com.david7ce.calistenia

import com.david7ce.calistenia.logic.filterExercises
import com.david7ce.calistenia.logic.foldText
import com.david7ce.calistenia.logic.matchesQuery
import com.david7ce.calistenia.model.Exercise
import com.david7ce.calistenia.model.Measure
import com.david7ce.calistenia.model.MuscleGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExerciseSearchTest {

    private val pushUp = Exercise("1", "Push-up", MuscleGroup.Push, Measure.Reps, true)
    private val pullUp = Exercise("2", "Domináda", MuscleGroup.Pull, Measure.Reps, false)
    private val plank = Exercise("3", "Plancha lateral", MuscleGroup.Core, Measure.Seconds, true)

    @Test
    fun foldsAccentsAndCase() {
        assertEquals("dominada", foldText("Domináda"))
        assertTrue(pullUp.matchesQuery("DOMINADA"))
        assertTrue(pullUp.matchesQuery("domin"))
        assertFalse(pullUp.matchesQuery("plancha"))
    }

    @Test
    fun emptyQueryMatchesEverything() {
        assertTrue(pushUp.matchesQuery("  "))
    }

    @Test
    fun filtersByGroupAndMeasure() {
        val all = listOf(pushUp, pullUp, plank)
        assertEquals(listOf(pullUp), filterExercises(all, query = "domi"))
        assertEquals(listOf(pushUp), filterExercises(all, group = MuscleGroup.Push))
        assertEquals(listOf(plank), filterExercises(all, measure = Measure.Seconds))
        assertEquals(listOf(pullUp), filterExercises(all, query = "DOM", group = MuscleGroup.Pull))
        assertEquals(emptyList(), filterExercises(all, query = "dominada", group = MuscleGroup.Push))
    }
}
