package com.david7ce.calistenia.logic

import com.david7ce.calistenia.model.Exercise
import com.david7ce.calistenia.model.Measure
import com.david7ce.calistenia.model.SessionExercise
import com.david7ce.calistenia.model.WorkoutSession
import com.david7ce.calistenia.model.WorkoutSet

/** The set of the given exercise, or the session unchanged when the ids don't match. */
fun WorkoutSession.updateSetValue(exerciseId: String, setId: String, value: Int?): WorkoutSession =
    mapExercise(exerciseId) { exercise ->
        exercise.copy(sets = exercise.sets.map { set ->
            if (set.id != setId) set else set.withValue(exercise.measure, value)
        })
    }

private fun WorkoutSet.withValue(measure: Measure, value: Int?): WorkoutSet {
    val clean = value?.takeIf { it > 0 }
    return when (measure) {
        // An emptied set is no longer done: a value and its tick must not drift apart.
        Measure.Reps -> copy(reps = clean, seconds = null, completedAt = if (clean == null) null else completedAt)
        Measure.Seconds -> copy(seconds = clean, reps = null, completedAt = if (clean == null) null else completedAt)
    }
}

/**
 * Ticks a set done, or clears the tick. An empty set cannot be completed: the UI keeps the button
 * disabled, and this is the second guard.
 */
fun WorkoutSession.toggleSet(exerciseId: String, setId: String, now: Long): WorkoutSession =
    mapExercise(exerciseId) { exercise ->
        exercise.copy(sets = exercise.sets.map { set ->
            if (set.id != setId) set
            else when {
                set.completedAt != null -> set.copy(completedAt = null)
                set.hasValue -> set.copy(completedAt = now)
                else -> set
            }
        })
    }

fun WorkoutSession.addSet(exerciseId: String, setId: String): WorkoutSession =
    mapExercise(exerciseId) { exercise ->
        exercise.copy(sets = exercise.sets + WorkoutSet(id = setId, reps = null, seconds = null, completedAt = null))
    }

/** Never leaves an exercise with no rows: the last set stays (the user can remove the exercise instead). */
fun WorkoutSession.removeSet(exerciseId: String, setId: String): WorkoutSession =
    mapExercise(exerciseId) { exercise ->
        if (exercise.sets.size <= 1) exercise else exercise.copy(sets = exercise.sets.filterNot { it.id == setId })
    }

/** Appends an exercise to an in-progress workout, with [targetSets] empty placeholder sets. */
fun WorkoutSession.addExercise(
    exercise: Exercise,
    sessionExerciseId: String,
    setIds: List<String>,
    targetSets: Int = DEFAULT_SET_COUNT,
    targetReps: Int? = if (exercise.measure == Measure.Reps) DEFAULT_REPS else null,
    targetSeconds: Int? = if (exercise.measure == Measure.Seconds) DEFAULT_HOLD_SECONDS else null,
    restSeconds: Int = DEFAULT_REST_SECONDS,
): WorkoutSession = copy(
    exercises = exercises + SessionExercise(
        id = sessionExerciseId,
        exerciseId = exercise.id,
        name = exercise.name,
        measure = exercise.measure,
        notes = "",
        targetSets = targetSets,
        targetReps = targetReps,
        targetSeconds = targetSeconds,
        restSeconds = restSeconds,
        sets = setIds.map { WorkoutSet(id = it, reps = null, seconds = null, completedAt = null) },
    ),
)

fun WorkoutSession.removeExercise(sessionExerciseId: String): WorkoutSession =
    copy(exercises = exercises.filterNot { it.id == sessionExerciseId })

fun WorkoutSession.updateExerciseNotes(sessionExerciseId: String, notes: String): WorkoutSession =
    copy(exercises = exercises.map { if (it.id == sessionExerciseId) it.copy(notes = notes.take(MAX_NOTES)) else it })

fun WorkoutSession.updateNotes(notes: String): WorkoutSession = copy(notes = notes.take(MAX_NOTES))

/** A workout can be finished once at least one set is ticked. */
val WorkoutSession.canFinish: Boolean get() = exercises.any { exercise -> exercise.sets.any { it.isDone } }

private fun WorkoutSession.mapExercise(exerciseId: String, change: (SessionExercise) -> SessionExercise): WorkoutSession =
    copy(exercises = exercises.map { if (it.id == exerciseId) change(it) else it })
