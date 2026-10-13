package com.david7ce.sleepcycles.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SleepCalcTest {
    private fun hm(h: Int, m: Int) = h * 60 + m

    @Test
    fun wakeModeCountsBackFromTheWakeTimeIncludingLatency() {
        val s = suggestions(Mode.Wake, hm(7, 0), 15, AgeRange.Adult)
        assertEquals(listOf(6, 5, 4, 3, 2, 1), s.map { it.cycles })
        assertEquals(
            listOf(hm(21, 45), hm(23, 15), hm(0, 45), hm(2, 15), hm(3, 45), hm(5, 15)),
            s.map { it.timeOfDay },
        )
        assertEquals(listOf(540, 450, 360, 270, 180, 90), s.map { it.sleepMinutes })
    }

    @Test
    fun bedModeCountsForwardFromTheBedTimeIncludingLatency() {
        val s = suggestions(Mode.Bed, hm(23, 0), 15, AgeRange.Adult)
        assertEquals(
            listOf(hm(8, 15), hm(6, 45), hm(5, 15), hm(3, 45), hm(2, 15), hm(0, 45)),
            s.map { it.timeOfDay },
        )
    }

    @Test
    fun resultsAlwaysStayInsideTheDayAroundMidnight() {
        for (mode in Mode.entries) for (anchor in listOf(0, 1, hm(23, 59), 1439)) for (latency in listOf(0, 15, 120)) {
            suggestions(mode, anchor, latency, AgeRange.Adult).forEach {
                assertTrue(it.timeOfDay in 0 until MINUTES_PER_DAY, "$mode $anchor $latency -> ${it.timeOfDay}")
            }
        }
        assertEquals(hm(22, 30), suggestions(Mode.Wake, 0, 0, AgeRange.Adult).last().timeOfDay)
        assertEquals(hm(1, 29), suggestions(Mode.Bed, hm(23, 59), 0, AgeRange.Adult).last().timeOfDay)
    }

    @Test
    fun latencyIsClampedToTheSupportedRange() {
        assertEquals(
            suggestions(Mode.Bed, 0, MAX_LATENCY_MINUTES, AgeRange.Adult),
            suggestions(Mode.Bed, 0, 500, AgeRange.Adult),
        )
        assertEquals(suggestions(Mode.Bed, 0, 0, AgeRange.Adult), suggestions(Mode.Bed, 0, -30, AgeRange.Adult))
        assertEquals(0, clampLatency(-1))
        assertEquals(120, clampLatency(121))
    }

    @Test
    fun ratingsPerAgeRange() {
        fun ratings(age: AgeRange) = suggestions(Mode.Wake, 0, 0, age).map { it.rating }
        val g = Rating.Good
        val o = Rating.Ok
        val l = Rating.Low
        assertEquals(listOf(g, g, o, o, l, l), ratings(AgeRange.Adult))
        assertEquals(listOf(g, o, l, o, l, l), ratings(AgeRange.Child))
        assertEquals(listOf(g, o, l, o, l, l), ratings(AgeRange.Teen))
        assertEquals(listOf(o, g, o, o, l, l), ratings(AgeRange.Senior))
    }

    @Test
    fun ratingBoundaries() {
        assertEquals(Rating.Low, rate(329, AgeRange.Adult))
        assertEquals(Rating.Ok, rate(330, AgeRange.Adult))
        assertEquals(Rating.Ok, rate(419, AgeRange.Adult))
        assertEquals(Rating.Good, rate(420, AgeRange.Adult))
        assertEquals(Rating.Good, rate(540, AgeRange.Adult))
        assertEquals(Rating.Ok, rate(541, AgeRange.Adult))
        assertEquals(Rating.Ok, rate(630, AgeRange.Adult))
        assertEquals(Rating.Low, rate(631, AgeRange.Adult))
        assertEquals(Rating.Ok, rate(270, AgeRange.Adult))
        assertEquals(Rating.Low, rate(271, AgeRange.Adult))
    }
}
