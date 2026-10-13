package com.david7ce.fuellog.data

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
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL DEFAULT '',
                registration TEXT NOT NULL DEFAULT '',
                fuel_type TEXT NOT NULL DEFAULT '',
                notes TEXT NOT NULL DEFAULT '',
                type TEXT NOT NULL DEFAULT 'car',
                ref_consumption_dl INTEGER)"""
        )
        createRefuelTable(db, "refuel")
        db.execSQL("INSERT INTO vehicle (id) VALUES (1)")
    }

    private fun createRefuelTable(db: SQLiteDatabase, name: String) {
        db.execSQL(
            """CREATE TABLE $name (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                vehicle_id INTEGER NOT NULL REFERENCES vehicle(id) ON DELETE CASCADE,
                timestamp INTEGER NOT NULL,
                station TEXT NOT NULL,
                odometer_km INTEGER NOT NULL CHECK (odometer_km >= 0),
                volume_ml INTEGER NOT NULL CHECK (volume_ml > 0),
                price_per_litre_milli INTEGER NOT NULL CHECK (price_per_litre_milli > 0),
                total_cents INTEGER NOT NULL CHECK (total_cents >= 0),
                full_tank INTEGER NOT NULL DEFAULT 1,
                missed_previous INTEGER NOT NULL DEFAULT 0,
                notes TEXT NOT NULL DEFAULT '',
                latitude REAL,
                longitude REAL)"""
        )
    }

    /**
     * v1 → v2: vehicles get a type (existing ones become cars); refuels lose the free-text location
     * and the manual-total flag, and gain coordinates. SQLite cannot drop columns on older Android
     * versions, so the table is rebuilt. Old location text is appended to the notes so nothing is
     * lost, and totals keep the amount that was stored.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE vehicle ADD COLUMN type TEXT NOT NULL DEFAULT 'car'")
            createRefuelTable(db, "refuel_v2")
            db.execSQL(
                """INSERT INTO refuel_v2 (id, vehicle_id, timestamp, station, odometer_km, volume_ml,
                       price_per_litre_milli, total_cents, full_tank, notes)
                   SELECT id, vehicle_id, timestamp, station, odometer_km, volume_ml,
                       price_per_litre_milli, total_cents, full_tank,
                       CASE WHEN location = '' THEN notes
                            WHEN notes = '' THEN location
                            ELSE notes || char(10) || location END
                   FROM refuel"""
            )
            db.execSQL("DROP TABLE refuel")
            db.execSQL("ALTER TABLE refuel_v2 RENAME TO refuel")
        }
        // v2 → v3: optional reference consumption per vehicle and the "missed previous refuel" flag.
        // A v1 database already got missed_previous from the table rebuilt above, hence the checks.
        if (oldVersion < 3) {
            if (!db.hasColumn("vehicle", "ref_consumption_dl")) db.execSQL("ALTER TABLE vehicle ADD COLUMN ref_consumption_dl INTEGER")
            if (!db.hasColumn("refuel", "missed_previous")) {
                db.execSQL("ALTER TABLE refuel ADD COLUMN missed_previous INTEGER NOT NULL DEFAULT 0")
            }
        }
        // Future: `if (oldVersion < 4) { ... }` — always migrate, never drop user data.
        db.execSQL("CREATE INDEX IF NOT EXISTS refuel_vehicle_time ON refuel(vehicle_id, timestamp)")
    }

    private fun SQLiteDatabase.hasColumn(table: String, column: String): Boolean =
        rawQuery("PRAGMA table_info($table)", null).use { c ->
            val name = c.getColumnIndexOrThrow("name")
            generateSequence { if (c.moveToNext()) c.getString(name) else null }.any { it == column }
        }

    override fun onOpen(db: SQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS refuel_vehicle_time ON refuel(vehicle_id, timestamp)")
    }

    fun vehicles(): List<Vehicle> =
        readableDatabase.query("vehicle", null, null, null, null, null, "id").use { c ->
            buildList { while (c.moveToNext()) add(c.toVehicle()) }
        }

    /** Inserts when id == 0, updates otherwise. Returns the row id. */
    fun saveVehicle(v: Vehicle): Long {
        val db = writableDatabase
        return if (v.id == 0L) db.insertOrThrow("vehicle", null, v.toValues())
        else { db.update("vehicle", v.toValues(), "id = ?", arrayOf(v.id.toString())); v.id }
    }

    /** Also deletes its refuellings (ON DELETE CASCADE). */
    fun deleteVehicle(id: Long) { writableDatabase.delete("vehicle", "id = ?", arrayOf(id.toString())) }

    /** All refuellings of all vehicles, newest first. A personal log is small enough to keep in memory. */
    fun refuels(): List<Refuel> =
        readableDatabase.query("refuel", null, null, null, null, null, "timestamp DESC, odometer_km DESC").use { c ->
            buildList { while (c.moveToNext()) add(c.toRefuel()) }
        }

    fun save(r: Refuel): Long {
        val db = writableDatabase
        return if (r.id == 0L) db.insertOrThrow("refuel", null, r.toValues())
        else { db.update("refuel", r.toValues(), "id = ?", arrayOf(r.id.toString())); r.id }
    }

    fun delete(id: Long) { writableDatabase.delete("refuel", "id = ?", arrayOf(id.toString())) }

    /** Puts a deleted record back with its original id (undo). */
    fun restore(r: Refuel) { writableDatabase.insertWithOnConflict("refuel", null, r.toValues(), SQLiteDatabase.CONFLICT_REPLACE) }

    /** Restore: replaces everything in one transaction, so a failure leaves the old data intact. */
    fun replaceAll(data: List<Pair<Vehicle, List<Refuel>>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("refuel", null, null)
            db.delete("vehicle", null, null)
            data.forEach { (vehicle, refuels) ->
                val id = db.insertOrThrow("vehicle", null, vehicle.copy(id = 0).toValues())
                refuels.forEach { db.insertOrThrow("refuel", null, it.copy(id = 0, vehicleId = id).toValues()) }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun Vehicle.toValues() = ContentValues().apply {
        if (id != 0L) put("id", id)
        put("name", name); put("type", type.key); put("registration", registration)
        put("fuel_type", fuelType); put("notes", notes)
        if (referenceDl100 != null) put("ref_consumption_dl", referenceDl100) else putNull("ref_consumption_dl")
    }

    private fun Refuel.toValues() = ContentValues().apply {
        if (id != 0L) put("id", id)
        put("vehicle_id", vehicleId); put("timestamp", timestamp); put("station", station)
        put("odometer_km", odometerKm); put("volume_ml", volumeMl)
        put("price_per_litre_milli", pricePerLitreMilli); put("total_cents", totalCents)
        put("full_tank", if (fullTank) 1 else 0); put("missed_previous", if (missedPrevious) 1 else 0)
        put("notes", notes)
        if (latitude != null && longitude != null) { put("latitude", latitude); put("longitude", longitude) }
        else { putNull("latitude"); putNull("longitude") }
    }

    private fun Cursor.str(col: String) = getString(getColumnIndexOrThrow(col)) ?: ""
    private fun Cursor.long(col: String) = getLong(getColumnIndexOrThrow(col))
    private fun Cursor.doubleOrNull(col: String) = getColumnIndexOrThrow(col).let { if (isNull(it)) null else getDouble(it) }
    private fun Cursor.intOrNull(col: String) = getColumnIndexOrThrow(col).let { if (isNull(it)) null else getInt(it) }

    private fun Cursor.toVehicle() = Vehicle(
        id = long("id"), name = str("name"), type = VehicleType.of(str("type")),
        registration = str("registration"), fuelType = str("fuel_type"), notes = str("notes"),
        referenceDl100 = intOrNull("ref_consumption_dl"),
    )

    private fun Cursor.toRefuel() = Refuel(
        id = long("id"), vehicleId = long("vehicle_id"), timestamp = long("timestamp"),
        station = str("station"), odometerKm = long("odometer_km"), volumeMl = long("volume_ml"),
        pricePerLitreMilli = long("price_per_litre_milli"), totalCents = long("total_cents"),
        fullTank = long("full_tank") != 0L, missedPrevious = long("missed_previous") != 0L, notes = str("notes"),
        latitude = doubleOrNull("latitude"), longitude = doubleOrNull("longitude"),
    )

    private companion object { const val VERSION = 3 }
}
