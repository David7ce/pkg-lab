package com.deivon.fuellog.logic

import com.deivon.fuellog.data.Refuel
import com.deivon.fuellog.data.Vehicle
import com.deivon.fuellog.data.VehicleType
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * JSON backup (lossless, for restore) and CSV export (for spreadsheets).
 * JSON stores the same exact integers as the database, so a round trip changes nothing.
 *
 * Format versions: 1 = single vehicle (FuelLog 1.0); 2 = list of vehicles, each with its refuellings.
 * Version 1 files are still accepted: their free-text location is moved into the notes, as in the
 * database migration.
 */
object Backup {
    const val FORMAT = "fuellog-backup"
    const val VERSION = 2

    class InvalidBackup(message: String) : Exception(message)

    data class Parsed(val vehicles: List<Pair<Vehicle, List<Refuel>>>, val skipped: Int) {
        val refuelCount: Int get() = vehicles.sumOf { it.second.size }
    }

    fun toJson(vehicles: List<Vehicle>, refuels: List<Refuel>, exportedAt: Long = System.currentTimeMillis()): String {
        val list = JSONArray()
        vehicles.forEach { v ->
            val items = JSONArray()
            FuelStats.chronological(refuels.filter { it.vehicleId == v.id }).forEach { items.put(refuelJson(it)) }
            list.put(
                JSONObject().put("name", v.name).put("type", v.type.key).put("registration", v.registration)
                    .put("fuelType", v.fuelType).put("notes", v.notes).put("refuels", items)
            )
        }
        return JSONObject().put("format", FORMAT).put("version", VERSION).put("exportedAt", exportedAt)
            .put("vehicles", list).toString(2)
    }

    private fun refuelJson(r: Refuel) = JSONObject()
        .put("timestamp", r.timestamp).put("station", r.station).put("odometerKm", r.odometerKm)
        .put("volumeMl", r.volumeMl).put("pricePerLitreMilli", r.pricePerLitreMilli).put("totalCents", r.totalCents)
        .put("fullTank", r.fullTank).put("notes", r.notes)
        .apply { if (r.latitude != null && r.longitude != null) put("latitude", r.latitude).put("longitude", r.longitude) }

    /**
     * Parses and validates a backup without touching the database. Throws [InvalidBackup] if the
     * file is not a usable backup at all; individual malformed records are skipped and counted.
     */
    fun parseJson(text: String): Parsed {
        val root = try { JSONObject(text) } catch (e: JSONException) { throw InvalidBackup("Not a JSON file") }
        if (root.optString("format") != FORMAT) throw InvalidBackup("Not a FuelLog backup")
        return when (root.optInt("version", -1)) {
            1 -> {
                val items = root.optJSONArray("refuels") ?: throw InvalidBackup("No records in backup")
                val (refuels, skipped) = parseRefuels(items)
                Parsed(listOf(vehicleOf(root.optJSONObject("vehicle") ?: JSONObject()) to refuels), skipped)
            }
            2 -> {
                val list = root.optJSONArray("vehicles") ?: throw InvalidBackup("No vehicles in backup")
                var skipped = 0
                val vehicles = (0 until list.length()).mapNotNull { i ->
                    val o = list.optJSONObject(i) ?: return@mapNotNull null
                    val (refuels, bad) = parseRefuels(o.optJSONArray("refuels") ?: JSONArray())
                    skipped += bad
                    vehicleOf(o) to refuels
                }
                if (vehicles.isEmpty()) throw InvalidBackup("No vehicles in backup")
                Parsed(vehicles, skipped)
            }
            else -> throw InvalidBackup("Unsupported backup version")
        }
    }

    private fun vehicleOf(o: JSONObject) = Vehicle(
        name = o.optString("name"), type = VehicleType.of(o.optString("type", "car")),
        registration = o.optString("registration"), fuelType = o.optString("fuelType"), notes = o.optString("notes"),
    )

    private fun parseRefuels(items: JSONArray): Pair<List<Refuel>, Int> {
        val ok = (0 until items.length()).mapNotNull { i -> items.optJSONObject(i)?.let(::refuelOrNull) }
        return ok to (items.length() - ok.size)
    }

    private fun refuelOrNull(o: JSONObject): Refuel? = try {
        val notes = o.optString("notes")
        val oldLocation = o.optString("location") // version 1 only
        val point = GeoPoint.of(
            o.optDouble("latitude").takeUnless { it.isNaN() },
            o.optDouble("longitude").takeUnless { it.isNaN() },
        )
        Refuel(
            timestamp = o.getLong("timestamp"), station = o.getString("station"),
            odometerKm = o.getLong("odometerKm"), volumeMl = o.getLong("volumeMl"),
            pricePerLitreMilli = o.getLong("pricePerLitreMilli"), totalCents = o.getLong("totalCents"),
            fullTank = o.optBoolean("fullTank", true),
            notes = listOf(notes, oldLocation).filter { it.isNotEmpty() }.joinToString("\n"),
            latitude = point?.lat, longitude = point?.lon,
        ).takeIf {
            it.timestamp > 0 && it.station.isNotBlank() && it.odometerKm >= 0 &&
                it.volumeMl > 0 && it.pricePerLitreMilli > 0 && it.totalCents >= 0
        }
    } catch (e: JSONException) {
        null
    }

    private val csvDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /** Comma-separated, dot decimals and ISO dates: what spreadsheets import without guessing. */
    fun toCsv(vehicles: List<Vehicle>, refuels: List<Refuel>, zone: ZoneId = ZoneId.systemDefault()): String = buildString {
        val names = vehicles.associate { it.id to it.name }
        appendLine("vehicle,date,station,latitude,longitude,odometer_km,litres,price_per_litre,total,full_tank,notes")
        FuelStats.chronological(refuels).forEach { r ->
            val fields = listOf(
                names[r.vehicleId].orEmpty(), csvDate.format(Instant.ofEpochMilli(r.timestamp).atZone(zone)), r.station,
                r.latitude?.toString().orEmpty(), r.longitude?.toString().orEmpty(),
                r.odometerKm.toString(), Decimals.toPlain(r.volumeMl, 3), Decimals.toPlain(r.pricePerLitreMilli, 3),
                Decimals.toPlain(r.totalCents, 2), r.fullTank.toString(), r.notes,
            )
            appendLine(fields.joinToString(",") { csvField(it) })
        }
    }

    internal fun csvField(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\""
        else value
}
