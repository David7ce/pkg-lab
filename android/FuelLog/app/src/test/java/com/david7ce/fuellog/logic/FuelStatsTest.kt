package com.david7ce.fuellog.logic

import com.david7ce.fuellog.data.Refuel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

class FuelStatsTest {

    private fun r(id: Long, day: Long, km: Long, ml: Long, full: Boolean = true, cents: Long = ml / 10) =
        Refuel(id = id, timestamp = day * 86_400_000, station = "S", odometerKm = km, volumeMl = ml,
            pricePerLitreMilli = 1000, totalCents = cents, fullTank = full)

    @Test fun twoFullTanksGiveOneInterval() {
        val i = FuelStats.intervals(listOf(r(1, 1, 10_000, 40_000), r(2, 10, 10_500, 30_000)))
        assertEquals(1, i.size)
        assertEquals(500L, i[0].distanceKm)
        assertEquals(30_000L, i[0].volumeMl) // the starting fill is not counted
        assertEquals(6.0, i[0].litresPer100Km, 1e-9)
        assertEquals(1L, i[0].startId)
        assertEquals(2L, i[0].endId)
    }

    @Test fun partialRefillsAccumulateIntoNextFullInterval() {
        val records = listOf(
            r(1, 1, 10_000, 40_000),
            r(2, 4, 10_200, 10_000, full = false),
            r(3, 8, 10_600, 26_000),
        )
        val i = FuelStats.intervals(records).single()
        assertEquals(600L, i.distanceKm)
        assertEquals(36_000L, i.volumeMl)
        assertEquals(6.0, i.litresPer100Km, 1e-9)
        assertEquals(3600L, i.costCents)
    }

    @Test fun inputOrderDoesNotMatter() {
        val records = listOf(r(3, 8, 10_600, 26_000), r(1, 1, 10_000, 40_000), r(2, 4, 10_200, 10_000, full = false))
        assertEquals(36_000L, FuelStats.intervals(records).single().volumeMl)
    }

    @Test fun partialsBeforeFirstFullTankAreIgnored() {
        val records = listOf(r(1, 1, 9_000, 5_000, full = false), r(2, 2, 10_000, 40_000), r(3, 5, 10_400, 24_000))
        val i = FuelStats.intervals(records).single()
        assertEquals(400L, i.distanceKm)
        assertEquals(24_000L, i.volumeMl)
    }

    @Test fun insufficientDataGivesNoInterval() {
        assertTrue(FuelStats.intervals(emptyList()).isEmpty())
        assertTrue(FuelStats.intervals(listOf(r(1, 1, 10_000, 40_000))).isEmpty())
        assertTrue(FuelStats.intervals(listOf(r(1, 1, 10_000, 40_000), r(2, 2, 10_300, 20_000, full = false))).isEmpty())
        assertNull(FuelStats.averageConsumption(emptyList()))
    }

    @Test fun odometerRegressionBreaksTheSequence() {
        val records = listOf(
            r(1, 1, 10_000, 40_000),
            r(2, 5, 9_900, 20_000),   // impossible: lower than before
            r(3, 9, 10_400, 30_000),  // measured from record 2, the new anchor
        )
        val i = FuelStats.intervals(records).single()
        assertEquals(2L, i.startId)
        assertEquals(500L, i.distanceKm)
    }

    @Test fun equalOdometerNeverDividesByZero() {
        val records = listOf(r(1, 1, 10_000, 40_000), r(2, 2, 10_000, 1_000))
        assertTrue(FuelStats.intervals(records).isEmpty())
    }

    @Test fun averagesAreWeighted() {
        val intervals = listOf(
            ConsumptionInterval(1, 2, 0, 100, 10_000, 0), // 10 L/100
            ConsumptionInterval(2, 3, 0, 900, 45_000, 0), //  5 L/100
        )
        assertEquals(5.5, FuelStats.averageConsumption(intervals)!!, 1e-9) // 55 L / 1000 km
    }

    @Test fun averagePriceIsTotalPaidOverTotalLitres() {
        val records = listOf(
            r(1, 1, 1, 10_000, cents = 1500), // 1.500 €/L
            r(2, 2, 2, 30_000, cents = 4200), // 1.400 €/L
        )
        assertEquals(1425L, FuelStats.averagePriceMilli(records)) // 57.00 € / 40 L
        assertNull(FuelStats.averagePriceMilli(emptyList()))
    }

    @Test fun costPerKmIsExact() {
        assertEquals(BigDecimal("0.087"), ConsumptionInterval(1, 2, 0, 600, 36_000, 5_220).costPerKm)
    }

    @Test fun periodsFilterByStartOfPeriod() {
        val today = LocalDate.of(2026, 10, 12)
        fun at(y: Int, m: Int, d: Int) = Refuel(timestamp = LocalDate.of(y, m, d).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            station = "S", odometerKm = 1, volumeMl = 1, pricePerLitreMilli = 1, totalCents = 1)
        val records = listOf(at(2026, 10, 1), at(2026, 9, 30), at(2026, 8, 1), at(2026, 7, 31), at(2025, 12, 31))
        assertEquals(1, Period.ThisMonth.filter(records, today, ZoneOffset.UTC).size)
        assertEquals(3, Period.Last3Months.filter(records, today, ZoneOffset.UTC).size)
        assertEquals(4, Period.ThisYear.filter(records, today, ZoneOffset.UTC).size)
        assertEquals(5, Period.All.filter(records, today, ZoneOffset.UTC).size)
    }
}
