package com.david7ce.calistenia.model

/** One line of a routine: which exercise, how many sets, the target, and the rest between sets. */
data class RoutineExercise(
    val id: String,
    val exercise: Exercise,
    val sets: Int,
    /** Exactly one of these is non-null, matching [Exercise.measure]. */
    val targetReps: Int?,
    val targetSeconds: Int?,
    val restSeconds: Int,
) {
    /** "3 × 8" or "3 × 30 s". */
    fun targetLabel(secondsSuffix: String): String = when (exercise.measure) {
        Measure.Reps -> "$sets × ${targetReps ?: 0}"
        Measure.Seconds -> "$sets × ${targetSeconds ?: 0} $secondsSuffix"
    }
}

/** A saved routine. Its exercises are ordered by list position. */
data class Routine(
    val id: String,
    val name: String,
    val notes: String,
    val exercises: List<RoutineExercise>,
    val createdAt: Long,
    val updatedAt: Long,
)
