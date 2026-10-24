package com.david7ce.calistenia

import com.david7ce.calistenia.logic.BuiltinExercises
import com.david7ce.calistenia.logic.addExercise
import com.david7ce.calistenia.logic.addSet
import com.david7ce.calistenia.logic.canFinish
import com.david7ce.calistenia.logic.removeExercise
import com.david7ce.calistenia.logic.removeSet
import com.david7ce.calistenia.logic.toggleSet
import com.david7ce.calistenia.logic.updateNotes
import com.david7ce.calistenia.logic.updateSetValue
import com.david7ce.calistenia.model.Measure
import com.david7ce.calistenia.model.SessionExercise
import com.david7ce.calistenia.model.WorkoutSession
import com.david7ce.calistenia.model.WorkoutSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionLogTest {

    private val session = WorkoutSession(
        id = "s",
        name = "Free",
        routineId = null,
        startedAt = 0,
        finishedAt = null,
        notes = "",
        exercises = listOf(
            SessionExercise(
                id = "ex",
                exerciseId = "builtin:push-up",
                name = "Push-up",
                measure = Measure.Reps,
                notes = "",
                targetSets = 2,
                targetReps = 8,
                targetSeconds = null,
                restSeconds = 90,
                sets = listOf(
                    WorkoutSet("set1", null, null, null),
                    WorkoutSet("set2", null, null, null),
                ),
            ),
        ),
    )

    @Test
    fun typingAValueFillsOnlyTheRightField() {
        val reps = session.updateSetValue("ex", "set1", 12)
        assertEquals(12, reps.exercises.single().sets.first().reps)
        assertEquals(null, reps.exercises.single().sets.first().seconds)
    }

    @Test
    fun holdsWriteSeconds() {
        val hold = testSession("h", 0, listOf(30), measure = Measure.Seconds, finished = false)
        val updated = hold.updateSetValue("h-ex", "h-set-0", 45)
        assertTrue(updated.exercises.single().sets.single().seconds == 45)
        assertEquals(null, updated.exercises.single().sets.single().reps)
    }

    @Test
    fun clearingAValueClearsTheTickToo() {
        val done = session.updateSetValue("ex", "set1", 10).toggleSet("ex", "set1", now = 5_000)
        assertTrue(done.exercises.single().sets.first().isDone)

        val cleared = done.updateSetValue("ex", "set1", null)
        assertFalse(cleared.exercises.single().sets.first().isDone)
        assertEquals(null, cleared.exercises.single().sets.first().completedAt)
    }

    @Test
    fun anEmptySetCannotBeTicked() {
        val after = session.toggleSet("ex", "set1", now = 1_000)
        assertEquals(null, after.exercises.single().sets.first().completedAt)
        assertFalse(after.canFinish)
    }

    @Test
    fun tickingAndUnticking() {
        val done = session.updateSetValue("ex", "set1", 8).toggleSet("ex", "set1", now = 1_000)
        assertEquals(1_000L, done.exercises.single().sets.first().completedAt)
        assertTrue(done.canFinish)
        assertFalse(done.toggleSet("ex", "set1", now = 2_000).exercises.single().sets.first().isDone)
    }

    @Test
    fun addingAndRemovingSets() {
        val withThree = session.addSet("ex", "set3")
        assertEquals(3, withThree.exercises.single().sets.size)
        assertEquals(2, withThree.removeSet("ex", "set3").exercises.single().sets.size)
        // The last remaining set is not removed from under the user.
        assertEquals(1, withThree.removeSet("ex", "set1").removeSet("ex", "set2").exercises.single().sets.size)
    }

    @Test
    fun addingAnExerciseIncludesItsTargetsAndEmptySets() {
        val hold = BuiltinExercises.find("builtin:plank")!!
        val next = session.addExercise(
            exercise = hold,
            sessionExerciseId = "ex2",
            setIds = listOf("a", "b", "c"),
            restSeconds = 60,
        )
        val added = next.exercises.last()
        assertEquals(Measure.Seconds, added.measure)
        assertEquals(3, added.sets.size)
        assertEquals(30, added.targetSeconds)
        assertEquals(null, added.targetReps)
        assertEquals(60, added.restSeconds)
        assertEquals(2, next.exercises.size)
    }

    @Test
    fun removingAnExerciseKeepsTheRest() {
        val next = session.addExercise(
            exercise = BuiltinExercises.find("builtin:plank")!!,
            sessionExerciseId = "ex2",
            setIds = listOf("a", "b", "c"),
        ).removeExercise("ex")
        assertEquals(1, next.exercises.size)
        assertEquals("ex2", next.exercises.single().id)
    }

    @Test
    fun notesAreCapped() {
        val next = session.updateNotes("x".repeat(700))
        assertEquals(500, next.notes.length)
    }
}
