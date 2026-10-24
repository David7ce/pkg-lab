package com.david7ce.calistenia

import com.david7ce.calistenia.logic.lastSetsFor
import com.david7ce.calistenia.logic.progressFor
import com.david7ce.calistenia.logic.weekSummary
import com.david7ce.calistenia.model.Measure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkoutMathTest {

    private val finishedA = testSession("a", startedAt = 1_000, sets = listOf(8, 8, 7))
    private val finishedB = testSession("b", startedAt = 10_000, sets = listOf(10, 9))
    private val inProgress = testSession("c", startedAt = 20_000, sets = listOf(5), finished = false)

    @Test
    fun progressCountsOnlyFinishedSessions() {
        val progress = progressFor("builtin:push-up", listOf(finishedA, inProgress, finishedB))
        assertEquals(2, progress.sessions)
        assertEquals(5, progress.totalSets)
        assertEquals(42L, progress.totalValue)
        assertEquals(10L, progress.bestValue)
        assertEquals(finishedB.finishedAt, progress.bestAt)
        assertEquals(finishedB.finishedAt, progress.lastPerformedAt)
        assertEquals(listOf(23L, 19L), progress.series.map { it.value })
    }

    @Test
    fun seriesKeepsTheLastNInOrder() {
        val progress = progressFor("builtin:push-up", listOf(finishedA, finishedB), limit = 1)
        assertEquals(listOf(19L), progress.series.map { it.value })
    }

    @Test
    fun noDataForAnUnknownExercise() {
        val progress = progressFor("builtin:pull-up", listOf(finishedA, finishedB))
        assertEquals(0, progress.sessions)
        assertTrue(progress.series.isEmpty())
        assertNull(progress.lastPerformedAt)
    }

    @Test
    fun holdsAreMeasuredInSeconds() {
        val hold = testSession("h", startedAt = 0, sets = listOf(45, 40), measure = Measure.Seconds)
        val progress = progressFor("builtin:push-up", listOf(hold))
        assertEquals(85L, progress.totalValue)
        assertEquals(45L, progress.bestValue)
    }

    @Test
    fun lastSetsComeFromTheMostRecentSession() {
        val sets = lastSetsFor("builtin:push-up", listOf(finishedB, finishedA))
        assertEquals(listOf(10, 9), sets.map { it.reps })
    }

    @Test
    fun weekSummaryIsARollingSevenDays() {
        val now = finishedB.finishedAt!! + 60 * 60 * 1000
        val summary = weekSummary(listOf(finishedA, finishedB, inProgress), now)
        assertEquals(2, summary.sessions)
        assertEquals(5, summary.sets)
        assertEquals(2 * 30 * 60 * 1000L, summary.durationMillis)

        val later = now + 8L * 24 * 60 * 60 * 1000
        assertEquals(0, weekSummary(listOf(finishedA, finishedB), later).sessions)
    }
}
