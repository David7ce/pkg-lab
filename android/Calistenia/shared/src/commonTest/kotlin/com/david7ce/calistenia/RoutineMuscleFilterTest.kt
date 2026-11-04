package com.david7ce.calistenia

import com.david7ce.calistenia.model.Exercise
import com.david7ce.calistenia.model.Measure
import com.david7ce.calistenia.model.Muscle
import com.david7ce.calistenia.model.MuscleGroup
import com.david7ce.calistenia.model.Routine
import com.david7ce.calistenia.model.RoutineExercise
import com.david7ce.calistenia.logic.trainsAny
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Routine filtering by the body-map selection (M3): a routine passes if ANY exercise passes. */
class RoutineMuscleFilterTest {

    private fun exercise(id: String, group: MuscleGroup, muscles: Set<Muscle> = emptySet()) =
        Exercise(id = id, name = id, group = group, measure = Measure.Reps, builtin = false, muscles = muscles)

    private fun line(e: Exercise) = RoutineExercise("l-${e.id}", e, 3, 8, null, 60)

    private fun routine(vararg e: Exercise) = Routine("r", "R", "", e.map(::line), 0, 0)

    private val squat = exercise("squat", MuscleGroup.Legs, setOf(Muscle.Quads, Muscle.Glutes))
    private val pushUp = exercise("push-up", MuscleGroup.Push, setOf(Muscle.Chest, Muscle.Triceps))

    @Test
    fun emptySelectionPassesEveryRoutineEvenAnEmptyOne() {
        assertTrue(routine(squat).trainsAny(emptySet()))
        assertTrue(routine().trainsAny(emptySet()))
    }

    @Test
    fun aRoutinePassesWhenAnyExerciseMatches() {
        val mixed = routine(pushUp, squat)
        assertTrue(mixed.trainsAny(setOf(Muscle.Quads)))
        assertTrue(mixed.trainsAny(setOf(Muscle.Chest, Muscle.Calves)))
        assertFalse(mixed.trainsAny(setOf(Muscle.Biceps)))
    }

    @Test
    fun anEmptyRoutineIsHiddenByAnyRealSelection() {
        assertFalse(routine().trainsAny(setOf(Muscle.Abs)))
    }

    @Test
    fun untaggedExercisesMatchThroughTheirGroup() {
        assertTrue(routine(exercise("mine", MuscleGroup.Pull)).trainsAny(setOf(Muscle.Biceps)))
    }

    @Test
    fun highlightIsPerExerciseAndHidesNothing() {
        val r = routine(pushUp, squat)
        val sel = setOf(Muscle.Quads)
        assertEquals(listOf(false, true), r.exercises.map { it.exercise.trainsAny(sel) })
        assertEquals(2, r.exercises.size)
    }
}
