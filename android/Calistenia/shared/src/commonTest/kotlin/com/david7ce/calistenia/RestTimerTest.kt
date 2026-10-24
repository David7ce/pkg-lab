package com.david7ce.calistenia

import com.david7ce.calistenia.logic.addMillis
import com.david7ce.calistenia.logic.pause
import com.david7ce.calistenia.logic.resume
import com.david7ce.calistenia.logic.skip
import com.david7ce.calistenia.logic.startRest
import com.david7ce.calistenia.logic.tick
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RestTimerTest {

    @Test
    fun startsRunningFromNow() {
        val rest = startRest(90, now = 1_000)!!
        assertTrue(rest.running)
        assertEquals(90_000L, rest.remaining(1_000))
        assertEquals(45_000L, rest.remaining(46_000))
        assertEquals(0f, rest.progress(1_000))
    }

    @Test
    fun zeroSecondsMeansNoTimer() {
        assertNull(startRest(0, now = 0))
        assertNull(startRest(-5, now = 0))
    }

    @Test
    fun reachingZeroFinishesInsteadOfGoingNegative() {
        val rest = startRest(30, now = 0)!!
        assertFalse(rest.tick(29_999).finished)
        val done = rest.tick(30_000)
        assertTrue(done.finished)
        assertFalse(done.running)
        assertEquals(0L, done.remaining(999_999))
        assertEquals(1f, done.progress(999_999))
    }

    @Test
    fun pauseFreezesAndResumeContinues() {
        val rest = startRest(60, now = 0)!!
        val paused = rest.pause(10_000)
        assertFalse(paused.running)
        assertEquals(50_000L, paused.remaining(200_000)) // time passing while paused does not count
        val resumed = paused.resume(200_000)
        assertTrue(resumed.running)
        assertEquals(50_000L, resumed.remaining(200_000))
        assertTrue(resumed.tick(250_000).finished)
    }

    @Test
    fun addingFifteenSecondsWorksRunningAndPaused() {
        val running = startRest(60, now = 0)!!
        val plus = running.addMillis(15_000, 10_000)
        assertEquals(75_000L, plus.remaining(0))
        val paused = running.pause(10_000).addMillis(15_000, 20_000)
        assertEquals(65_000L, paused.remaining(999_000))
    }

    @Test
    fun skipFinishesImmediately() {
        val skipped = startRest(90, now = 0)!!.skip()
        assertTrue(skipped.finished)
        assertEquals(0L, skipped.remaining(0))
    }

    @Test
    fun progressGrowsWithElapsedTime() {
        val rest = startRest(100, now = 0)!!
        assertEquals(0.25f, rest.progress(25_000))
        assertEquals(0.5f, rest.progress(50_000))
    }
}
