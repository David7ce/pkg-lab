package com.david7ce.sleepcycles.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TimeFormatTest {
    @Test
    fun partsIn24Hours() {
        assertEquals(TimeParts("07", "05", false), timeParts(7 * 60 + 5, ClockFormat.H24))
        assertEquals(TimeParts("23", "59", true), timeParts(23 * 60 + 59, ClockFormat.H24))
    }

    @Test
    fun partsIn12HoursHandleMidnightAndNoon() {
        assertEquals(TimeParts("12", "00", false), timeParts(0, ClockFormat.H12))
        assertEquals(TimeParts("12", "30", true), timeParts(12 * 60 + 30, ClockFormat.H12))
        assertEquals(TimeParts("01", "05", true), timeParts(13 * 60 + 5, ClockFormat.H12))
    }

    @Test
    fun parse12HourMidnightAndNoon() {
        assertEquals(0, parseTime("12", "00", false, ClockFormat.H12))
        assertEquals(12 * 60, parseTime("12", "00", true, ClockFormat.H12))
        assertEquals(19 * 60 + 5, parseTime("7", "5", true, ClockFormat.H12))
    }

    @Test
    fun invalidTextIsRejected() {
        assertNull(parseTime("0", "30", false, ClockFormat.H12))
        assertNull(parseTime("13", "00", false, ClockFormat.H12))
        assertNull(parseTime("24", "00", false, ClockFormat.H24))
        assertNull(parseTime("07", "60", false, ClockFormat.H24))
        assertNull(parseTime("", "30", false, ClockFormat.H24))
        assertNull(parseTime("ab", "30", false, ClockFormat.H24))
        assertNull(parseTime("07", "", false, ClockFormat.H24))
        assertNull(parseTime("-1", "30", false, ClockFormat.H24))
    }

    @Test
    fun everyMinuteOfTheDayRoundTripsInBothFormats() {
        for (format in ClockFormat.entries) for (m in 0 until MINUTES_PER_DAY) {
            val p = timeParts(m, format)
            assertEquals(m, parseTime(p.hour, p.minute, p.pm, format), "$format $m")
        }
    }

    @Test
    fun formattedTimes() {
        assertEquals("07:00", formatTime(7 * 60, ClockFormat.H24))
        assertEquals("00:45", formatTime(45, ClockFormat.H24))
        assertEquals("12:00 AM", formatTime(0, ClockFormat.H12))
        assertEquals("7:05 PM", formatTime(19 * 60 + 5, ClockFormat.H12))
        assertEquals("7:05 p. m.", formatTime(19 * 60 + 5, ClockFormat.H12, pm = "p. m."))
    }

    @Test
    fun hoursUseTheLanguageDecimalSeparator() {
        assertEquals("9", formatHours(540, '.'))
        assertEquals("7.5", formatHours(450, '.'))
        assertEquals("1,5", formatHours(90, ','))
        assertEquals("4,5", formatHours(270, ','))
    }

    @Test
    fun latencyTextNeverBreaksTheCalculation() {
        assertEquals(15, parseLatency("15"))
        assertEquals(20, parseLatency(" 20 "))
        assertEquals(0, parseLatency(""))
        assertEquals(0, parseLatency("abc"))
        assertEquals(0, parseLatency("-5"))
        assertEquals(0, parseLatency("1.5"))
        assertEquals(120, parseLatency("500"))
        assertEquals(120, parseLatency("99999999999"))
    }

    @Test
    fun latencyFieldShowsTheValueActuallyUsed() {
        assertEquals("15", latencyText("15"))
        assertEquals("120", latencyText("500"))
        assertEquals("120", latencyText("99999999999"))
        assertEquals("0", latencyText("-5"))
        assertEquals("7", latencyText("007"))
        assertEquals("12", latencyText("12a"))
        assertEquals("", latencyText(""))
        assertEquals("", latencyText("abc"))
    }
}
