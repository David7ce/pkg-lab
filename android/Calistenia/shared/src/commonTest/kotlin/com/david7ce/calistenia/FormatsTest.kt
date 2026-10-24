package com.david7ce.calistenia

import com.david7ce.calistenia.logic.formatCountdown
import com.david7ce.calistenia.logic.formatDuration
import com.david7ce.calistenia.logic.numberOr
import com.david7ce.calistenia.logic.numberText
import com.david7ce.calistenia.logic.setValueLabel
import kotlin.test.Test
import kotlin.test.assertEquals

class FormatsTest {

    @Test
    fun durationsUseMinutesAndAddHours() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:00", formatDuration(999))
        assertEquals("0:01", formatDuration(1_000))
        assertEquals("1:05", formatDuration(65_000))
        assertEquals("1:01:05", formatDuration(3_665_000))
        assertEquals("0:00", formatDuration(-5_000))
    }

    @Test
    fun countdownNeverGoesNegative() {
        assertEquals("0:00", formatCountdown(-1))
        assertEquals("1:30", formatCountdown(90_000))
    }

    @Test
    fun numericFieldsOnlyKeepDigits() {
        assertEquals("12", numberText("-12a", 4))
        assertEquals("123", numberText("123456", 3))
        assertEquals("", numberText("abc", 2))
        assertEquals(0, numberOr(""))
        assertEquals(30, numberOr("30"))
    }

    @Test
    fun setValuesShowTheirUnit() {
        assertEquals("8", setValueLabel(reps = 8, seconds = null, secondsUnit = "s"))
        assertEquals("30 s", setValueLabel(reps = null, seconds = 30, secondsUnit = "s"))
        assertEquals("—", setValueLabel(reps = null, seconds = null, secondsUnit = "s"))
    }
}
