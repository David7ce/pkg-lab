package com.david7ce.calistenia.logic

import com.david7ce.calistenia.model.Exercise
import com.david7ce.calistenia.model.Measure
import com.david7ce.calistenia.model.Routine
import com.david7ce.calistenia.model.RoutineExercise

const val MAX_ROUTINE_NAME = 60
const val MAX_NOTES = 500
const val MIN_SETS = 1
const val MAX_SETS = 20
const val MIN_REPS = 1
const val MAX_REPS = 999
const val MIN_HOLD_SECONDS = 1
const val MAX_HOLD_SECONDS = 3600
const val MAX_REST_SECONDS = 600
const val DEFAULT_SET_COUNT = 3
const val DEFAULT_REPS = 8
const val DEFAULT_HOLD_SECONDS = 30

/** One line of a routine being edited. [key] is a UI-only identity, stable while reordering. */
data class RoutineDraftExercise(
    val key: String,
    val exercise: Exercise,
    val sets: Int = DEFAULT_SET_COUNT,
    val targetReps: Int? = if (exercise.measure == Measure.Reps) DEFAULT_REPS else null,
    val targetSeconds: Int? = if (exercise.measure == Measure.Seconds) DEFAULT_HOLD_SECONDS else null,
    val restSeconds: Int = DEFAULT_REST_SECONDS,
)

/** A routine as the editor holds it: no ids for new children, inserted as a whole on save. */
data class RoutineDraft(
    val id: String? = null,
    val name: String = "",
    val notes: String = "",
    val exercises: List<RoutineDraftExercise> = emptyList(),
)

sealed interface RoutineProblem {
    data object NameRequired : RoutineProblem
    data object NoExercises : RoutineProblem
    data class InvalidSets(val index: Int) : RoutineProblem
    data class InvalidTarget(val index: Int) : RoutineProblem
    data class InvalidRest(val index: Int) : RoutineProblem
}

fun RoutineDraft.problems(): List<RoutineProblem> = buildList {
    if (name.trim().isEmpty()) add(RoutineProblem.NameRequired)
    if (exercises.isEmpty()) add(RoutineProblem.NoExercises)
    exercises.forEachIndexed { index, line ->
        if (line.sets !in MIN_SETS..MAX_SETS) add(RoutineProblem.InvalidSets(index))
        val targetValid = when (line.exercise.measure) {
            Measure.Reps -> line.targetReps != null && line.targetReps in MIN_REPS..MAX_REPS
            Measure.Seconds -> line.targetSeconds != null && line.targetSeconds in MIN_HOLD_SECONDS..MAX_HOLD_SECONDS
        }
        if (!targetValid) add(RoutineProblem.InvalidTarget(index))
        if (line.restSeconds !in 0..MAX_REST_SECONDS) add(RoutineProblem.InvalidRest(index))
    }
}

val RoutineDraft.isValid: Boolean get() = problems().isEmpty()

/** Trimmed and capped, ready to store. */
fun RoutineDraft.normalized(): RoutineDraft = copy(
    name = name.trim().take(MAX_ROUTINE_NAME),
    notes = notes.trim().take(MAX_NOTES),
)

/** Adds [exercise] with sensible defaults for its measure. */
fun RoutineDraft.addExercise(exercise: Exercise, key: String, restSeconds: Int = DEFAULT_REST_SECONDS): RoutineDraft =
    copy(exercises = exercises + RoutineDraftExercise(key = key, exercise = exercise, restSeconds = restSeconds))

fun Routine.toDraft(): RoutineDraft = RoutineDraft(
    id = id,
    name = name,
    notes = notes,
    exercises = exercises.map { line ->
        RoutineDraftExercise(
            key = line.id,
            exercise = line.exercise,
            sets = line.sets,
            targetReps = line.targetReps,
            targetSeconds = line.targetSeconds,
            restSeconds = line.restSeconds,
        )
    },
)
