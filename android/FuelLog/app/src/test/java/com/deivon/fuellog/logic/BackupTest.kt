package com.deivon.fuellog.logic

import com.deivon.fuellog.data.Refuel
import com.deivon.fuellog.data.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.ZoneOffset

class BackupTest {

    private val vehicle = Vehicle(name = "Golf", registration = "1234ABC", fuelType = "Diesel", notes = "")
    private val refuels = listOf(
        Refuel(timestamp = 1_700_000_000_000, station = "Repsol, Centro", odometerKm = 10_000, volumeMl = 45_200,
            pricePerLitreMilli = 1459, totalCents = 6595, notes = "said \"hi\""),
        Refuel(timestamp = 1_700_500_000_000, station = "BP", odometerKm = 10_600, volumeMl = 30_000,
            pricePerLitreMilli = 1500, totalCents = 4400, totalManual = true, fullTank = false),
    )

    @Test fun jsonRoundTripIsLossless() {
        val parsed = Backup.parseJson(Backup.toJson(vehicle, refuels))
        assertEquals(0, parsed.skipped)
        assertEquals(vehicle, parsed.vehicle)
        assertEquals(refuels, parsed.refuels)
    }

    @Test fun malformedRecordsAreSkippedAndCounted() {
        val json = """{"format":"fuellog-backup","version":1,"refuels":[
            {"timestamp":1,"station":"A","odometerKm":1,"volumeMl":1000,"pricePerLitreMilli":1500,"totalCents":150},
            {"timestamp":1,"station":"A","odometerKm":1,"volumeMl":-5,"pricePerLitreMilli":1500,"totalCents":150},
            {"station":"missing fields"},
            "not an object"
        ]}"""
        val parsed = Backup.parseJson(json)
        assertEquals(1, parsed.refuels.size)
        assertEquals(3, parsed.skipped)
    }

    @Test fun rejectsFilesThatAreNotBackups() {
        assertThrows(Backup.InvalidBackup::class.java) { Backup.parseJson("hello") }
        assertThrows(Backup.InvalidBackup::class.java) { Backup.parseJson("""{"format":"other"}""") }
        assertThrows(Backup.InvalidBackup::class.java) { Backup.parseJson("""{"format":"fuellog-backup","version":99,"refuels":[]}""") }
        assertThrows(Backup.InvalidBackup::class.java) { Backup.parseJson("""{"format":"fuellog-backup","version":1}""") }
    }

    @Test fun csvEscapesAndUsesPlainNumbers() {
        val lines = Backup.toCsv(refuels, ZoneOffset.UTC).lines()
        assertEquals("date,station,location,odometer_km,litres,price_per_litre,total,total_entered_manually,full_tank,notes", lines[0])
        assertEquals("2023-11-14 22:13,\"Repsol, Centro\",,10000,45.2,1.459,65.95,false,true,\"said \"\"hi\"\"\"", lines[1])
        assertEquals("BP", lines[2].split(",")[1])
    }
}
