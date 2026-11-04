package com.david7ce.calistenia

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.david7ce.calistenia.data.CalisteniaRepository
import com.david7ce.calistenia.db.CalisteniaDatabase
import com.david7ce.calistenia.logic.BuiltinExercises
import com.david7ce.calistenia.logic.CalendarData
import com.david7ce.calistenia.logic.RoutineDraft
import com.david7ce.calistenia.logic.RoutineDraftExercise
import com.david7ce.calistenia.logic.dayOf
import com.david7ce.calistenia.model.PlannedWorkout
import com.david7ce.calistenia.platform.JvmTimeFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The plan store against real SQLite, and `dayOf` against real time zones (these run in :shared:desktopTest). */
class CalendarStoreTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repo: CalisteniaRepository
    private var ids = 0
    private var clock = 1_000L

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CalisteniaDatabase.Schema.create(driver)
        repo = CalisteniaRepository(CalisteniaDatabase(driver), clock = { clock }, newId = { "id-${++ids}" })
        repo.seedBuiltins()
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun routine(name: String): String = repo.saveRoutine(
        RoutineDraft(
            name = name,
            exercises = listOf(
                RoutineDraftExercise("k", BuiltinExercises.find("builtin:push-up")!!, sets = 3, targetReps = 8, restSeconds = 60),
            ),
        ),
    )

    @Test
    fun plansAreStoredOnePerDayAndTheLaterOneReplaces() {
        repo.savePlannedWorkout(PlannedWorkout("a", "2026-11-04", null, "", ""))
        repo.savePlannedWorkout(PlannedWorkout("b", "2026-11-06", null, "", "note"))
        assertEquals(listOf("2026-11-04", "2026-11-06"), repo.plannedWorkouts().map { it.day })

        repo.savePlannedWorkout(PlannedWorkout("c", "2026-11-04", "r", "Legs", ""))
        val plans = repo.plannedWorkouts()
        assertEquals(2, plans.size)
        assertEquals("Legs", plans.first { it.day == "2026-11-04" }.routineName)

        repo.removePlannedWorkout("2026-11-04")
        assertEquals(listOf("2026-11-06"), repo.plannedWorkouts().map { it.day })
    }

    @Test
    fun deletingARoutineKeepsThePlanReadable() {
        val id = routine("Legs")
        repo.savePlannedWorkout(PlannedWorkout("p", "2026-11-10", id, "Legs", "heavy"))

        repo.deleteRoutine(id)

        val plan = repo.plannedWorkouts().single()
        assertNull(plan.routineId)
        assertEquals("Legs", plan.routineName)
        assertEquals("heavy", plan.note)
        assertTrue(plan.routineDeleted)
    }

    @Test
    fun historyAndCalendarNeverDisagreeAboutASession() {
        val zone = ZoneId.of("Europe/Madrid")
        val time = JvmTimeFormat(Locale.ENGLISH, zone)
        // Started 23:50 local on 2026-11-04; the clock is the same instant the repository stamps.
        clock = ZonedDateTime.of(2026, 11, 4, 23, 50, 0, 0, zone).toInstant().toEpochMilli()
        val routineId = routine("Push")
        val started = repo.startSession("Push", repo.routine(routineId))
        val set = repo.session(started.id)!!.exercises.single().sets.first()
        repo.updateSetValue(set.id, 10, null)
        repo.setCompleted(set.id, clock + 1_000)
        clock += 40 * 60_000L // finishes after midnight
        repo.finishSession(started.id, "")

        val history = repo.finishedSessions() // what the History tab lists
        val calendar = CalendarData(repo.plannedWorkouts(), history, time::utcOffsetMillis)

        val inHistory = history.single()
        val day = dayOf(inHistory.startedAt, time::utcOffsetMillis)
        assertEquals("2026-11-04", day)
        assertEquals(day, time.isoDate(inHistory.startedAt)) // one conversion, whoever asks
        assertEquals(listOf(inHistory.id), calendar.sessions(day).map { it.id })
        assertTrue(calendar.sessions("2026-11-05").isEmpty())
        assertTrue(calendar.cell(day, today = day).done)

        // Deleting it removes it from both views at once.
        repo.deleteSession(inHistory.id)
        val after = CalendarData(repo.plannedWorkouts(), repo.finishedSessions(), time::utcOffsetMillis)
        assertTrue(repo.finishedSessions().isEmpty())
        assertTrue(after.sessions(day).isEmpty())
    }

    @Test
    fun dayOfAcrossRealDstTransitions() {
        val ny = ZoneId.of("America/New_York")
        val time = JvmTimeFormat(Locale.ENGLISH, ny)
        fun at(y: Int, m: Int, d: Int, h: Int, min: Int, sec: Int = 0, nano: Int = 0) =
            ZonedDateTime.of(y, m, d, h, min, sec, nano, ny).toInstant().toEpochMilli()
        fun day(epoch: Long) = dayOf(epoch, time::utcOffsetMillis)

        // Spring forward 2026-03-08: 02:00 -> 03:00, a 23-hour day.
        assertEquals("2026-03-08", day(at(2026, 3, 8, 0, 0)))
        assertEquals("2026-03-08", day(at(2026, 3, 8, 23, 59, 59, 999_000_000)))
        assertEquals("2026-03-09", day(at(2026, 3, 9, 0, 0)))
        // 01:59 EST and 03:00 EDT are one minute apart and the same calendar day.
        assertEquals("2026-03-08", day(at(2026, 3, 8, 1, 59)))
        assertEquals("2026-03-08", day(at(2026, 3, 8, 3, 0)))

        // Fall back 2026-11-01: 02:00 -> 01:00, a 25-hour day; 01:30 happens twice.
        val firstOnePm = ZonedDateTime.of(2026, 11, 1, 1, 30, 0, 0, ny).withEarlierOffsetAtOverlap().toInstant().toEpochMilli()
        val secondOnePm = ZonedDateTime.of(2026, 11, 1, 1, 30, 0, 0, ny).withLaterOffsetAtOverlap().toInstant().toEpochMilli()
        assertTrue(secondOnePm - firstOnePm == 3_600_000L)
        assertEquals("2026-11-01", day(firstOnePm))
        assertEquals("2026-11-01", day(secondOnePm))
        assertEquals("2026-11-01", day(at(2026, 11, 1, 23, 59, 59, 999_000_000)))
        assertEquals("2026-11-02", day(at(2026, 11, 2, 0, 0)))

        // It always agrees with java.time's own answer.
        for (h in 0L..24L * 400 step 5) {
            val epoch = at(2026, 1, 1, 0, 0) + h * 3_600_000L
            assertEquals(java.time.Instant.ofEpochMilli(epoch).atZone(ny).toLocalDate().toString(), day(epoch))
        }
    }
}
