package com.david7ce.calistenia.logic

import com.david7ce.calistenia.model.SessionExercise
import com.david7ce.calistenia.model.WorkoutSession
import com.david7ce.calistenia.model.WorkoutSet

/** One point of an exercise's history: a finished session and what was done in it. */
data class ProgressPoint(val at: Long, val value: Long)

/** Everything the exercise progress screen shows, computed from finished sessions only. */
data class ExerciseProgress(
    val sessions: Int = 0,
    val totalSets: Int = 0,
    val totalValue: Long = 0,
    val bestValue: Long = 0,
    val bestAt: Long? = null,
    val lastPerformedAt: Long? = null,
    /** Oldest first, at most [SERIES_POINTS] sessions. */
    val series: List<ProgressPoint> = emptyList(),
) {
    val hasData: Boolean get() = sessions > 0
}

/** A rolling seven-day summary for the Train screen. */
data class WeekSummary(val sessions: Int = 0, val sets: Int = 0, val durationMillis: Long = 0)

private const val SERIES_POINTS = 10
private const val WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000

fun WorkoutSet.value(): Long = (reps ?: seconds ?: 0).toLong()

private fun SessionExercise.valueOfSets(): Long = sets.filter { it.isDone }.sumOf { it.value() }

/** Newest first. In-progress sessions are ignored even if the caller passes them. */
fun progressFor(
    exerciseId: String,
    sessions: List<WorkoutSession>,
    limit: Int = SERIES_POINTS,
): ExerciseProgress {
    val matching = sessions
        .filter { it.isFinished }
        .sortedBy { it.startedAt }
        .mapNotNull { session ->
            val exercise = session.exercises.firstOrNull { it.exerciseId == exerciseId && it.completed }
                ?: return@mapNotNull null
            session to exercise
        }
    if (matching.isEmpty()) return ExerciseProgress()

    var best = 0L
    var bestAt: Long? = null
    var totalSets = 0
    var totalValue = 0L
    val points = mutableListOf<ProgressPoint>()
    for ((session, exercise) in matching) {
        val done = exercise.sets.filter { it.isDone }
        totalSets += done.size
        val sessionValue = done.sumOf { it.value() }
        totalValue += sessionValue
        val sessionBest = done.maxOfOrNull { it.value() } ?: 0
        if (sessionBest > best) {
            best = sessionBest
            bestAt = session.finishedAt
        }
        points += ProgressPoint(session.finishedAt ?: session.startedAt, sessionValue)
    }
    return ExerciseProgress(
        sessions = matching.size,
        totalSets = totalSets,
        totalValue = totalValue,
        bestValue = best,
        bestAt = bestAt,
        lastPerformedAt = matching.last().first.finishedAt ?: matching.last().first.startedAt,
        series = points.takeLast(limit),
    )
}

/** The sets of the most recent finished session that included [exerciseId], newest session first. */
fun lastSetsFor(exerciseId: String, sessions: List<WorkoutSession>): List<WorkoutSet> {
    val session = sessions
        .filter { it.isFinished }
        .sortedByDescending { it.startedAt }
        .firstOrNull { s -> s.exercises.any { it.exerciseId == exerciseId && it.completed } }
        ?: return emptyList()
    return session.exercises
        .first { it.exerciseId == exerciseId }
        .sets.filter { it.isDone }
}

/** Sessions, done sets and time in the last seven days (rolling, so no timezone edge cases). */
fun weekSummary(sessions: List<WorkoutSession>, now: Long): WeekSummary {
    val recent = sessions.filter { it.isFinished && (it.finishedAt ?: it.startedAt) >= now - WEEK_MILLIS }
    return WeekSummary(
        sessions = recent.size,
        sets = recent.sumOf { it.totalSets },
        durationMillis = recent.sumOf { it.durationMillis ?: 0 },
    )
}

/** Total done sets across finished sessions (Train screen "last session" and history headers). */
fun WorkoutSession.doneSetCount(): Int = exercises.sumOf { exercise -> exercise.sets.count { it.isDone } }