package com.david7ce.calistenia

import com.david7ce.calistenia.logic.filterExercises
import com.david7ce.calistenia.logic.filterFamilies
import com.david7ce.calistenia.logic.familiesOf
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

    @Test
    fun variantsGroupUnderTheirStandardExercise() {
        val knee = Exercise("4", "Knee Push-up", MuscleGroup.Push, Measure.Reps, true, parentId = pushUp.id)
        val diamond = Exercise("5", "Diamond Push-up", MuscleGroup.Push, Measure.Reps, true, parentId = pushUp.id)
        val all = listOf(pushUp, knee, diamond, pullUp, plank)

        val families = familiesOf(all)
        assertEquals(listOf("1", "2", "3"), families.map { it.parent.id })
        val pushFamily = families.first { it.parent.id == pushUp.id }
        assertEquals(listOf("1", "5", "4"), pushFamily.variants.map { it.id }) // alphabetically after the parent
    }

    @Test
    fun searchingAVariantShowsItsFamily() {
        val knee = Exercise("4", "Knee Push-up", MuscleGroup.Push, Measure.Reps, true, parentId = pushUp.id)
        val diamond = Exercise("5", "Diamond Push-up", MuscleGroup.Push, Measure.Reps, true, parentId = pushUp.id)
        val all = listOf(pushUp, knee, diamond, pullUp, plank)

        assertEquals(listOf(pushUp.id), filterFamilies(all, query = "diamond").map { it.parent.id })
        assertEquals(listOf(pushUp.id), filterFamilies(all, query = "knee").map { it.parent.id })
        assertEquals(emptyList(), filterFamilies(all, query = "muscle-up"))
    }

    @Test
    fun measureFilterMatchesAnyVariantInTheFamily() {
        val plankHold = Exercise("6", "Plank", MuscleGroup.Core, Measure.Seconds, true)
        val pushUpFamily = Exercise("7", "Push-up", MuscleGroup.Push, Measure.Reps, true)
        val holdVariant = Exercise("8", "Explosive Push-up", MuscleGroup.Push, Measure.Seconds, true, parentId = pushUpFamily.id)
        val all = listOf(pushUpFamily, holdVariant, plankHold)

        assertEquals(
            listOf(pushUpFamily.id, plankHold.id),
            filterFamilies(all, measure = Measure.Seconds).map { it.parent.id },
        )
        assertEquals(2, filterFamilies(all, measure = Measure.Seconds).size)
        assertEquals(listOf(plankHold.id), filterFamilies(all, group = MuscleGroup.Core).map { it.parent.id })
    }
}
