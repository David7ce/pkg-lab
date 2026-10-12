package com.david7ce.fuellog.logic

import com.david7ce.fuellog.data.Refuel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeoPointTest {

    @Test fun parsesCommonFormats() {
        assertEquals(GeoPoint(40.4168, -3.7038), GeoPoint.parse("40.4168, -3.7038"))
        assertEquals(GeoPoint(40.4168, -3.7038), GeoPoint.parse("40.4168,-3.7038"))
        assertEquals(GeoPoint(40.4168, -3.7038), GeoPoint.parse("40,4168; -3,7038"))
        assertEquals(GeoPoint(-33.86, 151.2), GeoPoint.parse(" -33.86 151.2 "))
    }

    @Test fun rejectsInvalid() {
        assertNull(GeoPoint.parse(""))
        assertNull(GeoPoint.parse("hello"))
        assertNull(GeoPoint.parse("91, 0"))
        assertNull(GeoPoint.parse("0, 181"))
    }

    @Test fun formatsWithDotDecimals() {
        assertEquals("40.41680, -3.70380", GeoPoint(40.4168, -3.7038).format())
    }

    @Test fun mercatorRoundTrips() {
        val zoom = 13.4
        val x = Mercator.x(-3.7038, zoom)
        val y = Mercator.y(40.4168, zoom)
        assertEquals(-3.7038, Mercator.lon(x, zoom), 1e-9)
        assertEquals(40.4168, Mercator.lat(y, zoom), 1e-9)
        // Zoom 0: the whole world is one 256 px tile; (0, 0) is its centre.
        assertEquals(128.0, Mercator.x(0.0, 0.0), 1e-9)
        assertEquals(128.0, Mercator.y(0.0, 0.0), 1e-9)
    }
}

class ExtremesTest {
    private fun r(id: Long, price: Long) = Refuel(id = id, timestamp = id, station = "S", odometerKm = id,
        volumeMl = 1000, pricePerLitreMilli = price, totalCents = price / 10)

    @Test fun findsHighestAndLowestPrice() {
        val e = Extremes.of(listOf(r(1, 1500), r(2, 1399), r(3, 1620)), emptyList())
        assertEquals(3L, e.highestPrice?.id)
        assertEquals(2L, e.lowestPrice?.id)
        assertNull(e.bestConsumption)
    }

    @Test fun noExtremesWithoutSomethingToCompare() {
        assertNull(Extremes.of(listOf(r(1, 1500)), emptyList()).highestPrice)
        assertNull(Extremes.of(listOf(r(1, 1500), r(2, 1500)), emptyList()).highestPrice)
    }

    @Test fun findsBestAndWorstConsumption() {
        val a = ConsumptionInterval(1, 2, 0, 100, 7_000, 0) // 7 L/100
        val b = ConsumptionInterval(2, 3, 0, 100, 5_000, 0) // 5 L/100
        val e = Extremes.of(emptyList(), listOf(a, b))
        assertEquals(a, e.worstConsumption)
        assertEquals(b, e.bestConsumption)
    }
}
