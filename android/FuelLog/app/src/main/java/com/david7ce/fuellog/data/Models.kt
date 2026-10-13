package com.david7ce.fuellog.data

/** Stored as its lowercase [key]; unknown keys from newer backups fall back to [Other]. */
enum class VehicleType(val key: String) {
    Car("car"), Motorcycle("motorcycle"), Van("van"), Truck("truck"), Bus("bus"), Plane("plane"), Boat("boat"), Other("other");

    companion object {
        fun of(key: String?): VehicleType = entries.firstOrNull { it.key == key } ?: Other
    }
}

data class Vehicle(
    val id: Long = 0,
    val name: String = "",
    val type: VehicleType = VehicleType.Car,
    val registration: String = "",
    val fuelType: String = "",
    val notes: String = "",
    /** Expected consumption in tenths of a litre per 100 km (e.g. 54 = 5.4 L/100 km), or null. */
    val referenceDl100: Int? = null,
)

/**
 * One refuelling. Quantities are exact integers, never floating point:
 * money in cents, volume in millilitres, price in thousandths of a currency unit per litre
 * (fuel prices usually have 3 decimals, e.g. 1.459 €/L). [totalCents] is always litres × price.
 * Coordinates are optional and only used for display, so Double is fine there.
 */
data class Refuel(
    val id: Long = 0,
    val vehicleId: Long = 0,
    val timestamp: Long, // epoch millis
    val station: String,
    val odometerKm: Long,
    val volumeMl: Long,
    val pricePerLitreMilli: Long,
    val totalCents: Long,
    val fullTank: Boolean = true,
    /** The user skipped logging the refuelling before this one: consumption must not span the gap. */
    val missedPrevious: Boolean = false,
    val notes: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
)
