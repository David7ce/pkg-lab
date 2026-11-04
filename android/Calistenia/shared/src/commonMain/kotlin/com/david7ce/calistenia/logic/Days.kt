package com.david7ce.calistenia.logic

import com.david7ce.calistenia.model.PlannedWorkout
import com.david7ce.calistenia.model.WorkoutSession

private const val DAY_MILLIS = 86_400_000L

/**
 * The one place a session's instant becomes a calendar day. [utcOffsetMillis] is the zone's offset at
 * that instant (DST included), supplied by the platform, so a session started at 23:50 local belongs to
 * the day it started and the pure arithmetic here never guesses a timezone. History, the calendar grid
 * and the day sheet all go through this function.
 */
fun dayOf(epochMillis: Long, utcOffsetMillis: (Long) -> Long): String =
    isoOfEpochDay((epochMillis + utcOffsetMillis(epochMillis)).floorDiv(DAY_MILLIS))

private fun floorDiv(a: Long, b: Long): Long = a.floorDiv(b)

/** Days since 1970-01-01 to "YYYY-MM-DD" (proleptic Gregorian, Howard Hinnant's civil_from_days). */
fun isoOfEpochDay(epochDay: Long): String {
    val z = epochDay + 719_468
    val era = floorDiv(z, 146_097)
    val doe = z - era * 146_097
    val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    val y = yoe + era * 400 + if (m <= 2) 1 else 0
    return isoDay(y.toInt(), m.toInt(), d.toInt())
}

/** "YYYY-MM-DD" to days since 1970-01-01, or null when it is not a real date. */
fun epochDayOfIso(iso: String): Long? {
    val parts = iso.split("-")
    if (parts.size != 3) return null
    val y = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val d = parts[2].toIntOrNull() ?: return null
    if (m !in 1..12 || d !in 1..daysInMonth(y, m)) return null
    return epochDay(y, m, d)
}

/** The day of the month in "YYYY-MM-DD": 4 for "2026-11-04". */
fun dayNumber(day: String): Int = day.takeLast(2).toInt()

fun isoDay(year: Int, month: Int, day: Int): String =
    "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

private fun epochDay(year: Int, month: Int, day: Int): Long {
    val y = (if (month <= 2) year - 1 else year).toLong()
    val era = floorDiv(y, 400)
    val yoe = y - era * 400
    val mp = (month + 9) % 12
    val doy = (153 * mp + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146_097 + doe - 719_468
}

fun isLeapYear(year: Int): Boolean = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

fun daysInMonth(year: Int, month: Int): Int = when (month) {
    2 -> if (isLeapYear(year)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

/** 0 = Monday ... 6 = Sunday. */
fun weekdayIndex(epochDay: Long): Int = (epochDay + 3).mod(7L).toInt()

/** A calendar month. [month] is 1..12. */
data class MonthRef(val year: Int, val month: Int) {
    fun plusMonths(n: Int): MonthRef {
        val index = year * 12 + (month - 1) + n
        return MonthRef(index.floorDiv(12), index.mod(12) + 1)
    }

    companion object {
        fun of(day: String): MonthRef? {
            if (epochDayOfIso(day) == null) return null
            return MonthRef(day.substring(0, 4).toInt(), day.substring(5, 7).toInt())
        }
    }
}

/**
 * The cells of a month grid, Monday first: leading and trailing nulls pad whole weeks, and every other
 * cell is that day's ISO date.
 */
fun monthCells(ref: MonthRef): List<String?> {
    val lead = weekdayIndex(epochDay(ref.year, ref.month, 1))
    val days = (1..daysInMonth(ref.year, ref.month)).map { isoDay(ref.year, ref.month, it) }
    val cells = List<String?>(lead) { null } + days
    return cells + List<String?>((7 - cells.size % 7) % 7) { null }
}

/** What a grid cell shows. [planned] and [done] are independent: a day can be both. */
data class DayCell(
    val day: String,
    val planned: Boolean,
    val done: Boolean,
    val inProgress: Boolean,
    val today: Boolean,
)

/**
 * Plans and sessions grouped by day. The sessions are the same objects history lists (finished ones)
 * plus the running one, so the two views cannot disagree: nothing is copied or stored per day.
 */
class CalendarData(
    plans: List<PlannedWorkout>,
    sessions: List<WorkoutSession>,
    utcOffsetMillis: (Long) -> Long,
) {
    private val planByDay: Map<String, PlannedWorkout> = plans.associateBy { it.day }
    private val sessionsByDay: Map<String, List<WorkoutSession>> = sessions
        .distinctBy { it.id }
        .groupBy { dayOf(it.startedAt, utcOffsetMillis) }
        .mapValues { (_, list) -> list.sortedBy { it.startedAt } }

    fun plan(day: String): PlannedWorkout? = planByDay[day]

    /** Every session that started on [day], oldest first, finished or not. */
    fun sessions(day: String): List<WorkoutSession> = sessionsByDay[day] ?: emptyList()

    fun cell(day: String, today: String): DayCell {
        val onDay = sessions(day)
        return DayCell(
            day = day,
            planned = day in planByDay,
            done = onDay.any { it.isFinished },
            inProgress = onDay.any { !it.isFinished },
            today = day == today,
        )
    }
}
