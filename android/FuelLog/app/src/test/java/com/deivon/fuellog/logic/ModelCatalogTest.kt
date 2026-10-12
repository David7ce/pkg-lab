package com.deivon.fuellog.logic

import com.deivon.fuellog.data.Refuel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ModelCatalogTest {

    @Test fun parsesAndSkipsBadLines() {
        val csv = """
            # comment
            brand,model,fuel,l100
            Seat,León,petrol,5.6
            Toyota,Yaris,hybrid,4.0
            Bad,Line,rocket,5.0
            Too,Few,petrol
            Absurd,Value,diesel,99
        """.trimIndent()
        val models = ModelCatalog.parse(csv)
        assertEquals(listOf("Seat León", "Toyota Yaris"), models.map { it.name })
        assertEquals(56, models[0].referenceDl100)
        assertEquals(CatalogFuel.Hybrid, models[1].fuel)
    }

    @Test fun searchIgnoresCaseAndAccents() {
        val models = ModelCatalog.parse("Seat,León,petrol,5.6\nCupra,León,petrol,6.0\nSeat,Ibiza,petrol,5.3")
        assertEquals(listOf("Seat León"), ModelCatalog.search(models, "seat leon").map { it.name })
        assertEquals(2, ModelCatalog.search(models, "LEÓN").size)
        assertEquals(3, ModelCatalog.search(models, "  ").size)
    }

    /** The shipped file must parse completely: every data line valid, no duplicates. */
    @Test fun shippedCatalogIsValid() {
        val file = File("src/main/assets/models_es.csv")
        val text = file.readText()
        val dataLines = text.lines().count { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("brand,") }
        val models = ModelCatalog.parse(text)
        assertEquals(dataLines, models.size)
        assertEquals(models.size, models.map { it.name }.distinct().size)
        assertTrue(models.size >= 40)
    }
}

class MissedRefuelTest {
    private fun r(id: Long, km: Long, ml: Long, full: Boolean = true, missed: Boolean = false) =
        Refuel(id = id, timestamp = id, station = "S", odometerKm = km, volumeMl = ml, pricePerLitreMilli = 1000,
            totalCents = ml / 10, fullTank = full, missedPrevious = missed)

    @Test fun missedRefuelBreaksTheInterval() {
        val records = listOf(r(1, 10_000, 40_000), r(2, 11_000, 30_000, missed = true), r(3, 11_500, 30_000))
        val i = FuelStats.intervals(records).single()
        assertEquals(2L, i.startId) // 1 → 2 discarded: unlogged fuel in between
        assertEquals(500L, i.distanceKm)
    }

    @Test fun versusReference() {
        assertEquals(12, FuelStats.versusReference(5.6, 50))
        assertEquals(-10, FuelStats.versusReference(4.5, 50))
        assertNull(FuelStats.versusReference(null, 50))
        assertNull(FuelStats.versusReference(5.0, null))
    }
}
