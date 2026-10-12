package com.david7ce.fuellog.logic

import com.david7ce.fuellog.data.Refuel
import com.david7ce.fuellog.data.Vehicle
import com.david7ce.fuellog.data.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.ZoneOffset

class BackupTest {

    private val car = Vehicle(id = 1, name = "Golf", type = VehicleType.Car, registration = "1234ABC", fuelType = "Diesel")
    private val bike = Vehicle(id = 2, name = "MT-07", type = VehicleType.Motorcycle)
    private val refuels = listOf(
        Refuel(vehicleId = 1, timestamp = 1_700_000_000_000, station = "Repsol, Centro", odometerKm = 10_000,
            volumeMl = 45_200, pricePerLitreMilli = 1459, totalCents = 6595, notes = "said \"hi\"",
            latitude = 40.4168, longitude = -3.7038),
        Refuel(vehicleId = 1, timestamp = 1_700_500_000_000, station = "BP", odometerKm = 10_600,
            volumeMl = 30_000, pricePerLitreMilli = 1500, totalCents = 4500, fullTank = false),
        Refuel(vehicleId = 2, timestamp = 1_700_600_000_000, station = "Shell", odometerKm = 3_000,
            volumeMl = 12_000, pricePerLitreMilli = 1600, totalCents = 1920),
    )

    @Test fun jsonRoundTripIsLossless() {
        val parsed = Backup.parseJson(Backup.toJson(listOf(car, bike), refuels))
        assertEquals(0, parsed.skipped)
        assertEquals(listOf(car.copy(id = 0), bike.copy(id = 0)), parsed.vehicles.map { it.first })
        // Vehicle ids are reassigned on restore; everything else must match exactly.
        assertEquals(refuels.filter { it.vehicleId == 1L }.map { it.copy(vehicleId = 0) }, parsed.vehicles[0].second)
        assertEquals(refuels.filter { it.vehicleId == 2L }.map { it.copy(vehicleId = 0) }, parsed.vehicles[1].second)
        assertEquals(3, parsed.refuelCount)
    }

    @Test fun readsVersion1Backups() {
        val json = """{"format":"fuellog-backup","version":1,
            "vehicle":{"name":"Old car","registration":"","fuelType":"","notes":""},
            "refuels":[{"timestamp":1,"station":"A","location":"Main St","odometerKm":1,"volumeMl":1000,
                        "pricePerLitreMilli":1500,"totalCents":150,"totalManual":false,"fullTank":true,"notes":"n"}]}"""
        val parsed = Backup.parseJson(json)
        val (vehicle, list) = parsed.vehicles.single()
        assertEquals("Old car", vehicle.name)
        assertEquals(VehicleType.Car, vehicle.type)
        assertEquals("n\nMain St", list.single().notes) // old location kept in the notes
        assertNull(list.single().latitude)
    }

    @Test fun malformedRecordsAreSkippedAndCounted() {
        val json = """{"format":"fuellog-backup","version":2,"vehicles":[{"name":"X","type":"spaceship","refuels":[
            {"timestamp":1,"station":"A","odometerKm":1,"volumeMl":1000,"pricePerLitreMilli":1500,"totalCents":150,"latitude":99,"longitude":0},
            {"timestamp":1,"station":"A","odometerKm":1,"volumeMl":-5,"pricePerLitreMilli":1500,"totalCents":150},
            {"station":"missing fields"},
            "not an object"
        ]}]}"""
        val parsed = Backup.parseJson(json)
        val (vehicle, list) = parsed.vehicles.single()
        assertEquals(VehicleType.Other, vehicle.type) // unknown type
        assertEquals(1, list.size)
        assertNull(list.single().latitude) // out-of-range coordinates dropped, record kept
        assertEquals(3, parsed.skipped)
    }

    @Test fun rejectsFilesThatAreNotBackups() {
        assertThrows(Backup.InvalidBackup::class.java) { Backup.parseJson("hello") }
        assertThrows(Backup.InvalidBackup::class.java) { Backup.parseJson("""{"format":"other"}""") }
        assertThrows(Backup.InvalidBackup::class.java) { Backup.parseJson("""{"format":"fuellog-backup","version":99}""") }
        assertThrows(Backup.InvalidBackup::class.java) { Backup.parseJson("""{"format":"fuellog-backup","version":1}""") }
        assertThrows(Backup.InvalidBackup::class.java) { Backup.parseJson("""{"format":"fuellog-backup","version":2,"vehicles":[]}""") }
    }

    @Test fun csvEscapesAndUsesPlainNumbers() {
        val lines = Backup.toCsv(listOf(car, bike), refuels, ZoneOffset.UTC).lines()
        assertEquals("vehicle,date,station,latitude,longitude,odometer_km,litres,price_per_litre,total,full_tank,notes", lines[0])
        assertEquals("Golf,2023-11-14 22:13,\"Repsol, Centro\",40.4168,-3.7038,10000,45.2,1.459,65.95,true,\"said \"\"hi\"\"\"", lines[1])
        assertEquals("MT-07", lines[3].split(",")[0])
    }
}
