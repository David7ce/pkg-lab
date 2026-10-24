package com.david7ce.calistenia

import com.david7ce.calistenia.model.Measure
import com.david7ce.calistenia.model.SessionExercise
import com.david7ce.calistenia.model.WorkoutSession
import com.david7ce.calistenia.model.WorkoutSet
import com.david7ce.calistenia.platform.TimeFormat

class TestTimeFormat : TimeFormat {
    override fun dateTime(epochMillis: Long) = "2026-10-24 18:30"
    override fun shortDate(epochMillis: Long) = "2026-10-24"
    override fun time(epochMillis: Long) = "18:30"
    override fun isoDate(epochMillis: Long) = "2026-10-24"
}

/** A finished session with one exercise; [sets] is reps (or seconds, when [measure] says so). */
fun testSession(
    id: String,
    startedAt: Long,
    sets: List<Int>,
    measure: Measure = Measure.Reps,
    finished: Boolean = true,
    routineId: String? = null,
): WorkoutSession = WorkoutSession(
    id = id,
    name = "Test",
    routineId = routineId,
    startedAt = startedAt,
    finishedAt = if (finished) startedAt + 30 * 60 * 1000 else null,
    notes = "",
    exercises = listOf(
        SessionExercise(
            id = "$id-ex",
            exerciseId = "builtin:push-up",
            name = "Push-up",
            measure = measure,
            notes = "",
            targetSets = sets.size,
            targetReps = if (measure == Measure.Reps) 8 else null,
            targetSeconds = if (measure == Measure.Seconds) 30 else null,
            restSeconds = 90,
            sets = sets.mapIndexed { index, value ->
                WorkoutSet(
                    id = "$id-set-$index",
                    reps = if (measure == Measure.Reps) value else null,
                    seconds = if (measure == Measure.Seconds) value else null,
                    completedAt = startedAt + 1000L * index,
                )
            },
        ),
    ),
)
