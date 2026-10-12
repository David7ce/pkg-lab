package com.deivon.fuellog.logic

import com.deivon.fuellog.data.Refuel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RefuelValidatorTest {

    private val existing = listOf(
        Refuel(id = 1, timestamp = 1_000, station = "A", odometerKm = 10_000, volumeMl = 40_000, pricePerLitreMilli = 1500, totalCents = 6000),
        Refuel(id = 2, timestamp = 3_000, station = "B", odometerKm = 10_800, volumeMl = 40_000, pricePerLitreMilli = 1500, totalCents = 6000),
    )

    private fun form(ts: Long = 2_000, km: String = "10400", id: Long = 0) =
        RefuelForm(id = id, timestamp = ts, station = "C", odometer = km, litres = "45,2", price = "1.459")

    private fun errors(f: RefuelForm) = (RefuelValidator.validate(f, existing) as FormResult.Invalid).errors

    @Test fun validFormCalculatesTotal() {
        val r = (RefuelValidator.validate(form(), existing) as FormResult.Valid).refuel
        assertEquals(6595L, r.totalCents)
        assertEquals(45_200L, r.volumeMl)
        assertEquals(1459L, r.pricePerLitreMilli)
        assertEquals(false, r.totalManual)
    }

    @Test fun manualTotalOverridesCalculation() {
        val r = (RefuelValidator.validate(form().copy(totalManual = true, total = "60,00"), existing) as FormResult.Valid).refuel
        assertEquals(6000L, r.totalCents)
        assertTrue(r.totalManual)
    }

    @Test fun rejectsMissingAndNonPositiveValues() {
        val e = errors(RefuelForm(timestamp = 2_000, station = " ", odometer = "", litres = "0", price = "-1"))
        assertEquals(setOf(FormError.Station, FormError.Odometer, FormError.Litres, FormError.Price), e)
        assertEquals(setOf(FormError.Total), errors(form().copy(totalManual = true, total = "abc")))
    }

    @Test fun rejectsOdometerRegression() {
        assertEquals(setOf(FormError.OdometerNotAfterPrevious), errors(form(km = "9999")))
        assertEquals(setOf(FormError.OdometerNotBeforeNext), errors(form(km = "10800")))
        assertEquals(setOf(FormError.OdometerNotAfterPrevious), errors(form(ts = 5_000, km = "10800")))
    }

    @Test fun editingARecordIgnoresItsOwnOldValue() {
        // Correcting record 2 (between 1 and nothing) to a lower but still valid reading is allowed.
        assertTrue(RefuelValidator.validate(form(ts = 3_000, km = "10700", id = 2), existing) is FormResult.Valid)
    }
}
