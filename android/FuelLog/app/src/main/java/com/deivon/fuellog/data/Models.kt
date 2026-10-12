package com.deivon.fuellog.data

/** Only one vehicle (id = 1) for now; the id column keeps the schema ready for more. */
data class Vehicle(
    val id: Long = 1,
    val name: String = "",
    val registration: String = "",
    val fuelType: String = "",
    val notes: String = "",
)

/**
 * One refuelling. All quantities are exact integers, never floating point:
 * money in cents, volume in millilitres, price in thousandths of a currency unit per litre
 * (fuel prices usually have 3 decimals, e.g. 1.459 €/L).
 */
data class Refuel(
    val id: Long = 0,
    val vehicleId: Long = 1,
    val timestamp: Long, // epoch millis
    val station: String,
    val location: String = "",
    val odometerKm: Long,
    val volumeMl: Long,
    val pricePerLitreMilli: Long,
    val totalCents: Long,
    /** True when the user overrode the calculated total (e.g. a discount at the pump). */
    val totalManual: Boolean = false,
    val fullTank: Boolean = true,
    val notes: String = "",
)
