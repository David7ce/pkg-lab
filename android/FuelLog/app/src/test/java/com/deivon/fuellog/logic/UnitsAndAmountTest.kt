package com.deivon.fuellog.logic

import com.deivon.fuellog.data.Refuel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class UnitsTest {

    @Test fun metricIsIdentity() {
        assertEquals(12_345L, Units.odometerToKm(12_345, UnitSystem.Metric))
        assertEquals(45_200L, Units.volumeToMl(45_200, UnitSystem.Metric))
        assertEquals(1459L, Units.priceToPerLitre(1459, UnitSystem.Metric))
        assertEquals(6.2, Units.consumption(6.2, UnitSystem.Metric), 1e-9)
    }

    @Test fun milesRoundTrip() {
        assertEquals(16_093L, Units.odometerToKm(10_000, UnitSystem.Us))
        for (mi in listOf(1L, 999L, 12_345L, 250_000L)) {
            assertEquals(mi, Units.kmToOdometer(Units.odometerToKm(mi, UnitSystem.Uk), UnitSystem.Uk))
        }
    }

    @Test fun usGallons() {
        assertEquals(37_854L, Units.volumeToMl(10_000, UnitSystem.Us)) // 10 gal
        assertEquals(10_000L, Units.mlToVolume(37_854, UnitSystem.Us))
        assertEquals(914L, Units.priceToPerLitre(3459, UnitSystem.Us)) // $3.459/gal → $0.914/L
        assertEquals(45_200L, Units.volumeToMl(45_200, UnitSystem.Uk)) // UK pumps sell litres
    }

    @Test fun mpg() {
        assertEquals(47.08, Units.consumption(6.0, UnitSystem.Uk), 0.01)
        assertEquals(39.20, Units.consumption(6.0, UnitSystem.Us), 0.01)
        assertEquals(60, Units.consumptionToDl100(39.2, UnitSystem.Us))
        assertEquals(54, Units.consumptionToDl100(5.4, UnitSystem.Metric))
        assertNull(Units.consumptionToDl100(0.0, UnitSystem.Uk))
    }

    @Test fun costPerMile() {
        assertEquals(BigDecimal("0.140"), Units.costPerDistance(BigDecimal("0.087"), UnitSystem.Uk))
    }
}

class PayByAmountTest {

    @Test fun volumeIsDerivedFromAmountAndPrice() {
        val f = RefuelForm(timestamp = 1, station = "S", odometer = "1000", price = "1,459", byAmount = true, amount = "50")
        assertEquals(34_270L, f.volumeMl(UnitSystem.Metric)) // 50 / 1.459 = 34.2700 L
        assertEquals(5000L, f.totalCents(UnitSystem.Metric))  // total matches what was paid
    }

    @Test fun totalMatchesAmountForTypicalValues() {
        for (amount in listOf("10", "20", "35,50", "50", "72,13", "100")) {
            for (price in listOf("1,299", "1,459", "1,689", "0,899")) {
                val f = RefuelForm(timestamp = 1, station = "S", odometer = "1", price = price, byAmount = true, amount = amount)
                assertEquals("$amount @ $price", Decimals.parse(amount, 2), f.totalCents(UnitSystem.Metric))
            }
        }
    }

    @Test fun missingAmountIsAnError() {
        val f = RefuelForm(timestamp = 1, station = "S", odometer = "1000", price = "1.5", byAmount = true, amount = "")
        val e = (RefuelValidator.validate(f, emptyList(), 1) as FormResult.Invalid).errors
        assertEquals(setOf(FormError.Amount), e)
    }

    @Test fun formRoundTripInUsUnits() {
        val r = Refuel(id = 3, vehicleId = 1, timestamp = 1, station = "S", odometerKm = 16_093, volumeMl = 37_854,
            pricePerLitreMilli = 914, totalCents = 3460)
        val f = RefuelForm.from(r, Locale.US, UnitSystem.Us)
        assertEquals("10000", f.odometer)
        assertEquals("10", f.litres)
        val back = (RefuelValidator.validate(f, emptyList(), 1, UnitSystem.Us) as FormResult.Valid).refuel
        assertEquals(r.odometerKm, back.odometerKm)
        assertEquals(r.volumeMl, back.volumeMl)
        assertEquals(r.pricePerLitreMilli, back.pricePerLitreMilli)
    }
}

class WarningsTest {
    private fun r(id: Long, day: Long, km: Long, ml: Long, price: Long = 1500) =
        Refuel(id = id, vehicleId = 1, timestamp = day, station = "S", odometerKm = km, volumeMl = ml,
            pricePerLitreMilli = price, totalCents = totalCents(ml, price))

    private val history = listOf(r(1, 1, 10_000, 40_000), r(2, 2, 10_600, 36_000), r(3, 3, 11_200, 36_000))

    @Test fun normalRefuelHasNoWarnings() {
        assertTrue(RefuelValidator.warnings(r(0, 4, 11_800, 36_000), history).isEmpty())
    }

    @Test fun longDistanceIsFlagged() {
        val w = RefuelValidator.warnings(r(0, 4, 21_200, 36_000), history)
        assertTrue(w.any { it is FormWarning.LongDistance && it.km == 10_000L })
    }

    @Test fun implausibleConsumptionIsFlagged() {
        val w = RefuelValidator.warnings(r(0, 4, 11_300, 60_000), history) // 60 L in 100 km
        assertTrue(w.any { it is FormWarning.OddConsumption })
    }

    @Test fun oddPriceIsFlagged() {
        val w = RefuelValidator.warnings(r(0, 4, 11_800, 36_000, price = 15_000), history) // typo ×10
        assertTrue(w.any { it is FormWarning.OddPrice })
    }

    @Test fun missedPreviousSkipsDistanceCheck() {
        val w = RefuelValidator.warnings(r(0, 4, 21_200, 36_000).copy(missedPrevious = true), history)
        assertTrue(w.none { it is FormWarning.LongDistance })
    }
}
