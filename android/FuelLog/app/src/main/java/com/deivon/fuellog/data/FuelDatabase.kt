package com.deivon.fuellog.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Plain SQLite via the platform helper: two tables and a handful of queries do not justify Room
 * (annotation processing + runtime library). Call from a background dispatcher.
 */
class FuelDatabase(context: Context) : SQLiteOpenHelper(context, "fuellog.db", null, VERSION) {

    override fun onConfigure(db: SQLiteDatabase) = db.setForeignKeyConstraintsEnabled(true)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE vehicle (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL DEFAULT '',
                registration TEXT NOT NULL DEFAULT '',
                fuel_type TEXT NOT NULL DEFAULT '',
                notes TEXT NOT NULL DEFAULT '')"""
        )
        db.execSQL(
            """CREATE TABLE refuel (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                vehicle_id INTEGER NOT NULL REFERENCES vehicle(id) ON DELETE CASCADE,
                timestamp INTEGER NOT NULL,
                station TEXT NOT NULL,
                location TEXT NOT NULL DEFAULT '',
                odometer_km INTEGER NOT NULL CHECK (odometer_km >= 0),
                volume_ml INTEGER NOT NULL CHECK (volume_ml > 0),
                price_per_litre_milli INTEGER NOT NULL CHECK (price_per_litre_milli > 0),
                total_cents INTEGER NOT NULL CHECK (total_cents >= 0),
                total_manual INTEGER NOT NULL DEFAULT 0,
                full_tank INTEGER NOT NULL DEFAULT 1,
                notes TEXT NOT NULL DEFAULT '')"""
        )
        db.execSQL("CREATE INDEX refuel_vehicle_time ON refuel(vehicle_id, timestamp)")
        db.execSQL("INSERT INTO vehicle (id) VALUES (1)")
    }

    // Version 1 is the first schema. Future changes: bump VERSION and add `if (oldVersion < N)` steps
    // here that ALTER existing tables — never drop user data.
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun vehicle(id: Long = 1): Vehicle =
        readableDatabase.query("vehicle", null, "id = ?", arrayOf(id.toString()), null, null, null).use { c ->
            if (c.moveToFirst()) c.toVehicle() else Vehicle(id)
        }

    fun saveVehicle(v: Vehicle) {
        writableDatabase.insertWithOnConflict("vehicle", null, v.toValues(), SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun refuels(vehicleId: Long = 1): List<Refuel> =
        readableDatabase.query(
            "refuel", null, "vehicle_id = ?", arrayOf(vehicleId.toString()), null, null, "timestamp DESC, odometer_km DESC"
        ).use { c -> buildList { while (c.moveToNext()) add(c.toRefuel()) } }

    /** Inserts when id == 0, updates otherwise. Returns the row id. */
    fun save(r: Refuel): Long {
        val db = writableDatabase
        return if (r.id == 0L) db.insertOrThrow("refuel", null, r.toValues())
        else { db.update("refuel", r.toValues(), "id = ?", arrayOf(r.id.toString())); r.id }
    }

    fun delete(id: Long) { writableDatabase.delete("refuel", "id = ?", arrayOf(id.toString())) }

    /** Restore: replaces everything in one transaction, so a failure leaves the old data intact. */
    fun replaceAll(vehicle: Vehicle, refuels: List<Refuel>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("refuel", "vehicle_id = ?", arrayOf(vehicle.id.toString()))
            db.insertWithOnConflict("vehicle", null, vehicle.toValues(), SQLiteDatabase.CONFLICT_REPLACE)
            refuels.forEach { db.insertOrThrow("refuel", null, it.copy(id = 0, vehicleId = vehicle.id).toValues()) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun Vehicle.toValues() = ContentValues().apply {
        put("id", id); put("name", name); put("registration", registration)
        put("fuel_type", fuelType); put("notes", notes)
    }

    private fun Refuel.toValues() = ContentValues().apply {
        if (id != 0L) put("id", id)
        put("vehicle_id", vehicleId); put("timestamp", timestamp); put("station", station)
        put("location", location); put("odometer_km", odometerKm); put("volume_ml", volumeMl)
        put("price_per_litre_milli", pricePerLitreMilli); put("total_cents", totalCents)
        put("total_manual", if (totalManual) 1 else 0); put("full_tank", if (fullTank) 1 else 0)
        put("notes", notes)
    }

    private fun Cursor.str(col: String) = getString(getColumnIndexOrThrow(col)) ?: ""
    private fun Cursor.long(col: String) = getLong(getColumnIndexOrThrow(col))

    private fun Cursor.toVehicle() = Vehicle(long("id"), str("name"), str("registration"), str("fuel_type"), str("notes"))

    private fun Cursor.toRefuel() = Refuel(
        id = long("id"), vehicleId = long("vehicle_id"), timestamp = long("timestamp"),
        station = str("station"), location = str("location"), odometerKm = long("odometer_km"),
        volumeMl = long("volume_ml"), pricePerLitreMilli = long("price_per_litre_milli"),
        totalCents = long("total_cents"), totalManual = long("total_manual") != 0L,
        fullTank = long("full_tank") != 0L, notes = str("notes"),
    )

    private companion object { const val VERSION = 1 }
}
