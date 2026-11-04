package com.david7ce.calistenia

import com.david7ce.calistenia.logic.CalendarData
import com.david7ce.calistenia.logic.MonthRef
import com.david7ce.calistenia.logic.dayOf
import com.david7ce.calistenia.logic.daysInMonth
import com.david7ce.calistenia.logic.epochDayOfIso
import com.david7ce.calistenia.logic.isoOfEpochDay
import com.david7ce.calistenia.logic.monthCells
import com.david7ce.calistenia.model.PlannedWorkout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalendarTest {

    private val hour = 3_600_000L
    private val utc: (Long) -> Long = { 0L }
    private fun fixed(hours: Int): (Long) -> Long = { hours * hour }

    // 2026-11-04T00:00:00Z
    private val nov4 = 1_793_750_400_000L

    @Test
    fun dayOfUsesTheLocalDayNotTheUtcDay() {
        assertEquals("2026-11-04", dayOf(nov4, utc))
        // 23:50 UTC on the 4th is already the 5th in Madrid (+1) but still the 4th in New York (-5).
        val late = nov4 + 23 * hour + 50 * 60_000L
        assertEquals("2026-11-04", dayOf(late, utc))
        assertEquals("2026-11-05", dayOf(late, fixed(1)))
        assertEquals("2026-11-04", dayOf(late, fixed(-5)))
    }

    @Test
    fun dayOfAroundMidnight() {
        val lastMilli = nov4 - 1
        assertEquals("2026-11-03", dayOf(lastMilli, utc))
        assertEquals("2026-11-04", dayOf(nov4, utc))
        assertEquals("2026-11-04", dayOf(nov4 + 24 * hour - 1, utc))
        assertEquals("2026-11-05", dayOf(nov4 + 24 * hour, utc))
    }

    @Test
    fun dayOfWithAnOffsetThatChangesAtDst() {
        // A zone that is +1 before the change instant and +2 after it (spring forward).
        val change = nov4 + 12 * hour
        val zone: (Long) -> Long = { if (it < change) hour else 2 * hour }
        // 22:30 UTC the day before is 23:30 local: still the day before.
        assertEquals("2026-11-03", dayOf(nov4 - 90 * 60_000L, zone))
        // 21:59:59.999 UTC on the 4th is 23:59:59.999 local (+2): the 4th. One milli later: the 5th.
        assertEquals("2026-11-04", dayOf(nov4 + 22 * hour - 1, zone))
        assertEquals("2026-11-05", dayOf(nov4 + 22 * hour, zone))
    }

    @Test
    fun dayOfBeforeTheEpochAndOnLeapDays() {
        assertEquals("1970-01-01", dayOf(0, utc))
        assertEquals("1969-12-31", dayOf(-1, utc))
        assertEquals("2024-02-29", dayOf(1_709_164_800_000L, utc))
        assertEquals("2100-03-01", dayOf(4_107_542_400_000L, utc)) // 2100 is not a leap year
    }

    @Test
    fun isoAndEpochDayRoundTrip() {
        for (epochDay in -700_000L..800_000L step 97) {
            val iso = isoOfEpochDay(epochDay)
            assertEquals(epochDay, epochDayOfIso(iso), iso)
        }
        assertNull(epochDayOfIso("2026-02-30"))
        assertNull(epochDayOfIso("2026-13-01"))
        assertNull(epochDayOfIso("nope"))
    }

    @Test
    fun monthGridIsMondayFirstAndPadsWholeWeeks() {
        // November 2026 starts on a Sunday: six leading blanks, 30 days, then padding to a multiple of 7.
        val cells = monthCells(MonthRef(2026, 11))
        assertEquals(42, cells.size)
        assertEquals(List(6) { null }, cells.take(6))
        assertEquals("2026-11-01", cells[6])
        assertEquals("2026-11-30", cells.last { it != null })
        // June 2026 starts on a Monday: no leading blanks; February 2021 fits exactly four weeks.
        assertEquals("2026-06-01", monthCells(MonthRef(2026, 6)).first())
        assertEquals(28, monthCells(MonthRef(2021, 2)).size)
        assertEquals(29, daysInMonth(2028, 2))
    }

    @Test
    fun monthNavigationWrapsYears() {
        assertEquals(MonthRef(2027, 1), MonthRef(2026, 12).plusMonths(1))
        assertEquals(MonthRef(2025, 12), MonthRef(2026, 1).plusMonths(-1))
        assertEquals(MonthRef(2026, 11), MonthRef(2026, 11).plusMonths(0))
    }

    private fun plan(day: String, name: String = "Push", routineId: String? = "r1") =
        PlannedWorkout("p-$day", day, routineId, name, "")

    @Test
    fun todayShowsPlannedAndDoneIndependently() {
        val session = testSession("s1", startedAt = nov4 + 10 * hour, sets = listOf(8, 8))
        val data = CalendarData(listOf(plan("2026-11-04")), listOf(session), utc)

        val both = data.cell("2026-11-04", today = "2026-11-04")
        assertTrue(both.planned && both.done && both.today)
        assertFalse(both.inProgress)

        // Planned only, done only, and neither.
        val plannedOnly = CalendarData(listOf(plan("2026-11-04")), emptyList(), utc).cell("2026-11-04", "2026-11-04")
        assertTrue(plannedOnly.planned && !plannedOnly.done)
        val doneOnly = CalendarData(emptyList(), listOf(session), utc).cell("2026-11-04", "2026-11-04")
        assertTrue(!doneOnly.planned && doneOnly.done)
        val empty = CalendarData(emptyList(), emptyList(), utc).cell("2026-11-05", "2026-11-04")
        assertTrue(!empty.planned && !empty.done && !empty.inProgress && !empty.today)
    }

    @Test
    fun anUnfinishedSessionIsInProgressNotDone() {
        val running = testSession("s2", startedAt = nov4 + hour, sets = listOf(8), finished = false)
        val cell = CalendarData(emptyList(), listOf(running), utc).cell("2026-11-04", "2026-11-04")
        assertTrue(cell.inProgress)
        assertFalse(cell.done)
    }

    @Test
    fun aSessionBelongsToTheDayItStarted() {
        // Started 23:50 local, finished after midnight: the day it started.
        val late = nov4 + 23 * hour + 50 * 60_000L
        val session = testSession("s3", startedAt = late, sets = listOf(5))
        val data = CalendarData(emptyList(), listOf(session), utc)
        assertEquals(listOf("s3"), data.sessions("2026-11-04").map { it.id })
        assertTrue(data.sessions("2026-11-05").isEmpty())
    }

    @Test
    fun twoSessionsOnOneDayAreBothInTheSheetAndTheCellStaysOneMark() {
        val a = testSession("a", startedAt = nov4 + 8 * hour, sets = listOf(8))
        val b = testSession("b", startedAt = nov4 + 18 * hour, sets = listOf(8))
        val data = CalendarData(emptyList(), listOf(b, a), utc)
        assertEquals(listOf("a", "b"), data.sessions("2026-11-04").map { it.id })
        assertTrue(data.cell("2026-11-04", "x").done)
    }

    @Test
    fun aPlanWhoseRoutineWasDeletedStaysReadable() {
        val gone = PlannedWorkout("p", "2026-11-10", null, "Legs", "")
        assertTrue(gone.routineDeleted)
        assertEquals("Legs", gone.routineName)
        val decide = PlannedWorkout("q", "2026-11-11", null, "", "")
        assertFalse(decide.routineDeleted)
        assertTrue(CalendarData(listOf(gone), emptyList(), utc).cell("2026-11-10", "x").planned)
    }
}
