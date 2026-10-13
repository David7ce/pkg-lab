package com.david7ce.fuellog.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class DecimalsTest {

    @Test fun parsesBothDecimalSeparators() {
        assertEquals(1459L, Decimals.parse("1,459", 3))
        assertEquals(1459L, Decimals.parse("1.459", 3))
        assertEquals(45200L, Decimals.parse(" 45.2 ", 3))
        assertEquals(500L, Decimals.parse(".5", 3))
        assertEquals(123456L, Decimals.parse("123456", 0))
    }

    @Test fun trailingZerosBeyondScaleAreAccepted() {
        assertEquals(1459L, Decimals.parse("1.45900", 3))
    }

    @Test fun rejectsInvalidInput() {
        assertNull(Decimals.parse("", 3))
        assertNull(Decimals.parse("-1", 3))
        assertNull(Decimals.parse("abc", 3))
        assertNull(Decimals.parse("1.2.3", 3))
        assertNull(Decimals.parse("1 000", 0))
        assertNull(Decimals.parse("1.4599", 3)) // more precision than stored: reject instead of rounding silently
        assertNull(Decimals.parse("12.5", 0))
    }

    @Test fun formatsWithLocale() {
        assertEquals("45,20", Decimals.format(45200, 3, Locale("es", "ES"), minFraction = 2, maxFraction = 2))
        assertEquals("1.459", Decimals.format(1459, 3, Locale.US))
        assertEquals("45,2", Decimals.toInput(45200, 3, Locale("es", "ES")))
        assertEquals("1234.5", Decimals.toInput(1234500, 3, Locale.US))
        assertEquals("65.95", Decimals.toPlain(6595, 2))
    }

    @Test fun totalIsRoundedHalfUpToTheCent() {
        assertEquals(6595L, totalCents(45200, 1459)) // 45.2 L × 1.459 = 65.9468
        assertEquals(1L, totalCents(10, 500))        // 0.01 L × 0.500 = 0.005 → 0.01
        assertEquals(0L, totalCents(9, 500))         // 0.0045 → 0.00
        assertEquals(10000L, totalCents(50000, 2000))
    }
}
