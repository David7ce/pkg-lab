package com.deivon.fuellog.logic

import com.deivon.fuellog.data.Refuel
import com.deivon.fuellog.data.Vehicle
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * JSON backup (lossless, for restore) and CSV export (for spreadsheets).
 * JSON stores the same exact integers as the database, so a round trip changes nothing.
 */
object Backup {
    const val FORMAT = "fuellog-backup"
    const val VERSION = 1

    class InvalidBackup(message: String) : Exception(message)

    data class Parsed(val vehicle: Vehicle, val refuels: List<Refuel>, val skipped: Int)

    fun toJson(vehicle: Vehicle, refuels: List<Refuel>, exportedAt: Long = System.currentTimeMillis()): String {
        val items = JSONArray()
        FuelStats.chronological(refuels).forEach { r ->
            items.put(
                JSONObject()
                    .put("timestamp", r.timestamp).put("station", r.station).put("location", r.location)
                    .put("odometerKm", r.odometerKm).put("volumeMl", r.volumeMl)
                    .put("pricePerLitreMilli", r.pricePerLitreMilli).put("totalCents", r.totalCents)
                    .put("totalManual", r.totalManual).put("fullTank", r.fullTank).put("notes", r.notes)
            )
        }
        return JSONObject()
            .put("format", FORMAT).put("version", VERSION).put("exportedAt", exportedAt)
            .put(
                "vehicle", JSONObject().put("name", vehicle.name).put("registration", vehicle.registration)
                    .put("fuelType", vehicle.fuelType).put("notes", vehicle.notes)
            )
            .put("refuels", items)
            .toString(2)
    }

    /**
     * Parses and validates a backup without touching the database. Throws [InvalidBackup] if the
     * file is not a usable backup at all; individual malformed records are skipped and counted.
     */
    fun parseJson(text: String): Parsed {
        val root = try { JSONObject(text) } catch (e: JSONException) { throw InvalidBackup("Not a JSON file") }
        if (root.optString("format") != FORMAT) throw InvalidBackup("Not a FuelLog backup")
        if (root.optInt("version", -1) !in 1..VERSION) throw InvalidBackup("Unsupported backup version")
        val items = root.optJSONArray("refuels") ?: throw InvalidBackup("No records in backup")

        val v = root.optJSONObject("vehicle") ?: JSONObject()
        val vehicle = Vehicle(
            name = v.optString("name"), registration = v.optString("registration"),
            fuelType = v.optString("fuelType"), notes = v.optString("notes"),
        )
        val refuels = (0 until items.length()).mapNotNull { i -> items.optJSONObject(i)?.let(::refuelOrNull) }
        return Parsed(vehicle, refuels, skipped = items.length() - refuels.size)
    }

    private fun refuelOrNull(o: JSONObject): Refuel? = try {
        Refuel(
            timestamp = o.getLong("timestamp"), station = o.getString("station"),
            location = o.optString("location"), odometerKm = o.getLong("odometerKm"),
            volumeMl = o.getLong("volumeMl"), pricePerLitreMilli = o.getLong("pricePerLitreMilli"),
            totalCents = o.getLong("totalCents"), totalManual = o.optBoolean("totalManual"),
            fullTank = o.optBoolean("fullTank", true), notes = o.optString("notes"),
        ).takeIf {
            it.timestamp > 0 && it.station.isNotBlank() && it.odometerKm >= 0 &&
                it.volumeMl > 0 && it.pricePerLitreMilli > 0 && it.totalCents >= 0
        }
    } catch (e: JSONException) {
        null
    }

    private val csvDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /** Comma-separated, dot decimals and ISO dates: what spreadsheets import without guessing. */
    fun toCsv(refuels: List<Refuel>, zone: ZoneId = ZoneId.systemDefault()): String = buildString {
        appendLine("date,station,location,odometer_km,litres,price_per_litre,total,total_entered_manually,full_tank,notes")
        FuelStats.chronological(refuels).forEach { r ->
            val fields = listOf(
                csvDate.format(Instant.ofEpochMilli(r.timestamp).atZone(zone)), r.station, r.location,
                r.odometerKm.toString(), Decimals.toPlain(r.volumeMl, 3), Decimals.toPlain(r.pricePerLitreMilli, 3),
                Decimals.toPlain(r.totalCents, 2), r.totalManual.toString(), r.fullTank.toString(), r.notes,
            )
            appendLine(fields.joinToString(",") { csvField(it) })
        }
    }

    internal fun csvField(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\""
        else value
}
