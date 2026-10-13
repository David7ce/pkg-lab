package com.david7ce.sleepcycles.logic

const val CYCLE_MINUTES = 90
const val MINUTES_PER_DAY = 24 * 60
/** The PWA input for time to fall asleep is 0..120. */
const val MAX_LATENCY_MINUTES = 120
const val DEFAULT_LATENCY_MINUTES = 15

/** Cycles offered, from the most sleep to the least. */
val CYCLE_COUNTS = listOf(6, 5, 4, 3, 2, 1)

/** [Wake]: the user fixes the wake-up time and gets bedtimes. [Bed]: the reverse. */
enum class Mode { Wake, Bed }

/** Recommended sleep per age range, in minutes. The labels (with the years) live in Strings. */
enum class AgeRange(val minMinutes: Int, val maxMinutes: Int) {
    Child(9 * 60, 11 * 60),
    Teen(8 * 60, 10 * 60),
    Adult(7 * 60, 9 * 60),
    Senior(7 * 60, 8 * 60),
}

enum class Rating { Good, Ok, Low }

/** [timeOfDay] is minutes since midnight: the bedtime (Wake mode) or the wake-up time (Bed mode). */
data class Suggestion(val cycles: Int, val timeOfDay: Int, val sleepMinutes: Int, val rating: Rating)

fun wrapDay(minutes: Int): Int = ((minutes % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY

fun clampLatency(minutes: Int): Int = minutes.coerceIn(0, MAX_LATENCY_MINUTES)

/**
 * Good inside the recommended range, ok within one cycle of it, and 4.5 h (three cycles) is always
 * at least ok: the PWA treats it as the cycle-aligned fallback.
 */
fun rate(sleepMinutes: Int, age: AgeRange): Rating = when {
    sleepMinutes in age.minMinutes..age.maxMinutes -> Rating.Good
    sleepMinutes in (age.minMinutes - CYCLE_MINUTES)..(age.maxMinutes + CYCLE_MINUTES) -> Rating.Ok
    sleepMinutes == 3 * CYCLE_MINUTES -> Rating.Ok
    else -> Rating.Low
}

fun suggestions(mode: Mode, anchor: Int, latencyMinutes: Int, age: AgeRange): List<Suggestion> {
    val latency = clampLatency(latencyMinutes)
    return CYCLE_COUNTS.map { cycles ->
        val sleep = cycles * CYCLE_MINUTES
        val offset = latency + sleep
        val time = if (mode == Mode.Wake) wrapDay(anchor - offset) else wrapDay(anchor + offset)
        Suggestion(cycles, time, sleep, rate(sleep, age))
    }
}
