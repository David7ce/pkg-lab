package com.david7ce.calistenia.model

/**
 * One set. [reps] or [seconds] is set once the user types a value; [completedAt] is set when the
 * set is ticked. An unticked set with no value is a placeholder row, not history.
 */
data class WorkoutSet(
    val id: String,
    val reps: Int?,
    val seconds: Int?,
    val completedAt: Long?,
) {
    val hasValue: Boolean get() = (reps ?: 0) > 0 || (seconds ?: 0) > 0
    val isDone: Boolean get() = completedAt != null && hasValue

    /** What history shows: "8" or "30 s". */
    fun valueLabel(secondsSuffix: String): String = when {
        reps != null -> reps.toString()
        seconds != null -> "$seconds $secondsSuffix"
        else -> "—"
    }
}

/** One exercise inside a session, with a snapshot of its name, measure and routine targets. */
data class SessionExercise(
    val id: String,
    val exerciseId: String,
    val name: String,
    val measure: Measure,
    val notes: String,
    val targetSets: Int,
    val targetReps: Int?,
    val targetSeconds: Int?,
    val restSeconds: Int,
    val sets: List<WorkoutSet>,
) {
    val doneSets: Int get() = sets.count { it.isDone }

    /** True when every done set has a value for the right measure; keeps history readable. */
    val completed: Boolean get() = doneSets > 0
}

/** A workout. [finishedAt] null means it is still in progress (and can be resumed after a restart). */
data class WorkoutSession(
    val id: String,
    val name: String,
    val routineId: String?,
    val startedAt: Long,
    val finishedAt: Long?,
    val notes: String,
    val exercises: List<SessionExercise>,
) {
    val isFinished: Boolean get() = finishedAt != null
    val durationMillis: Long? get() = finishedAt?.let { (it - startedAt).coerceAtLeast(0) }
    val totalSets: Int get() = exercises.sumOf { it.doneSets }
}
