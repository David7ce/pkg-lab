package com.david7ce.calistenia

import com.david7ce.calistenia.logic.BuiltinExercises
import com.david7ce.calistenia.logic.RoutineDraft
import com.david7ce.calistenia.logic.RoutineDraftExercise
import com.david7ce.calistenia.logic.RoutineProblem
import com.david7ce.calistenia.logic.addExercise
import com.david7ce.calistenia.logic.isValid
import com.david7ce.calistenia.logic.normalized
import com.david7ce.calistenia.logic.problems
import com.david7ce.calistenia.logic.toDraft
import com.david7ce.calistenia.model.Measure
import com.david7ce.calistenia.model.Routine
import com.david7ce.calistenia.model.RoutineExercise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoutineDraftTest {

    private val pushUp = BuiltinExercises.find("builtin:push-up")!!
    private val plank = BuiltinExercises.find("builtin:plank")!!

    private fun line(
        exercise: com.david7ce.calistenia.model.Exercise = pushUp,
        sets: Int = 3,
        reps: Int? = 8,
        seconds: Int? = null,
        rest: Int = 90,
    ) = RoutineDraftExercise(
        key = "k",
        exercise = exercise,
        sets = sets,
        targetReps = reps,
        targetSeconds = seconds,
        restSeconds = rest,
    )

    @Test
    fun emptyDraftNeedsANameAndAnExercise() {
        val problems = RoutineDraft().problems()
        assertEquals(listOf(RoutineProblem.NameRequired, RoutineProblem.NoExercises), problems)
        assertFalse(RoutineDraft().isValid)
    }

    @Test
    fun aCompleteDraftIsValid() {
        val draft = RoutineDraft(name = "Push day", exercises = listOf(line()))
        assertTrue(draft.isValid)
        assertEquals("Push day", draft.normalized().name)
        assertEquals("Push day", RoutineDraft(name = "  Push day  ", exercises = listOf(line())).normalized().name)
    }

    @Test
    fun boundsAreCheckedPerLine() {
        val badSets = RoutineDraft(name = "R", exercises = listOf(line(sets = 0)))
        assertEquals(listOf(RoutineProblem.InvalidSets(0)), badSets.problems())

        val badTarget = RoutineDraft(name = "R", exercises = listOf(line(reps = null)))
        assertEquals(listOf(RoutineProblem.InvalidTarget(0)), badTarget.problems())

        val badRest = RoutineDraft(name = "R", exercises = listOf(line(rest = 601)))
        assertEquals(listOf(RoutineProblem.InvalidRest(0)), badRest.problems())
    }

    @Test
    fun holdsUseSecondsAndNotReps() {
        val hold = line(exercise = plank, reps = null, seconds = 30)
        assertTrue(RoutineDraft(name = "Core", exercises = listOf(hold)).isValid)
        assertFalse(RoutineDraft(name = "Core", exercises = listOf(hold.copy(targetSeconds = null))).isValid)
    }

    @Test
    fun addingAnExercisePicksSensibleDefaults() {
        val repsDraft = RoutineDraft().addExercise(pushUp, key = "a", restSeconds = 60)
        assertEquals(3, repsDraft.exercises.single().sets)
        assertEquals(8, repsDraft.exercises.single().targetReps)
        assertEquals(null, repsDraft.exercises.single().targetSeconds)
        assertEquals(60, repsDraft.exercises.single().restSeconds)

        val holdDraft = RoutineDraft().addExercise(plank, key = "b")
        assertEquals(30, holdDraft.exercises.single().targetSeconds)
        assertEquals(null, holdDraft.exercises.single().targetReps)
    }

    @Test
    fun draftRoundTripsThroughASavedRoutine() {
        val saved = Routine(
            id = "r1",
            name = "Push day",
            notes = "Slow",
            exercises = listOf(
                RoutineExercise("l1", pushUp, sets = 4, targetReps = 6, targetSeconds = null, restSeconds = 120),
            ),
            createdAt = 1,
            updatedAt = 2,
        )
        val draft = saved.toDraft()
        assertEquals("r1", draft.id)
        assertEquals("Push day", draft.name)
        assertEquals("Slow", draft.notes)
        assertEquals(Measure.Reps, draft.exercises.single().exercise.measure)
        assertEquals(4, draft.exercises.single().sets)
    }
}
