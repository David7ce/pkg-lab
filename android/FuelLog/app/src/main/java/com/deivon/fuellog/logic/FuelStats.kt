package com.deivon.fuellog.logic

import com.deivon.fuellog.data.Refuel
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * A reliable measurement between two consecutive full-tank refuellings.
 * [volumeMl] and [costCents] include every refuelling after the start (partials too) up to and
 * including the ending full tank: that is the fuel burned over [distanceKm].
 */
data class ConsumptionInterval(
    val startId: Long,
    val endId: Long,
    val endTimestamp: Long,
    val distanceKm: Long,
    val volumeMl: Long,
    val costCents: Long,
) {
    /** L/100 km = (mL / 1000) / km × 100 = mL / (10 × km). */
    val litresPer100Km: Double get() = volumeMl / (10.0 * distanceKm)

    /** Currency units per km, 3 decimals. */
    val costPerKm: BigDecimal get() =
        BigDecimal.valueOf(costCents, 2).divide(BigDecimal.valueOf(distanceKm), 3, RoundingMode.HALF_UP)
}

/**
 * Highest/lowest price per litre and best/worst consumption. Only set when there are at least two
 * values to compare, so a single record is never labelled both "highest" and "lowest".
 */
data class Extremes(
    val highestPrice: Refuel?,
    val lowestPrice: Refuel?,
    val worstConsumption: ConsumptionInterval?,
    val bestConsumption: ConsumptionInterval?,
) {
    companion object {
        val NONE = Extremes(null, null, null, null)

        fun of(records: List<Refuel>, intervals: List<ConsumptionInterval>): Extremes {
            val prices = records.size >= 2 && records.any { it.pricePerLitreMilli != records[0].pricePerLitreMilli }
            val consumption = intervals.size >= 2
            return Extremes(
                highestPrice = if (prices) records.maxByOrNull { it.pricePerLitreMilli } else null,
                lowestPrice = if (prices) records.minByOrNull { it.pricePerLitreMilli } else null,
                worstConsumption = if (consumption) intervals.maxByOrNull { it.litresPer100Km } else null,
                bestConsumption = if (consumption) intervals.minByOrNull { it.litresPer100Km } else null,
            )
        }
    }
}

/** Everything the screens show for one vehicle, computed once per data change (off the UI thread). */
data class FuelSummary(
    val intervals: List<ConsumptionInterval>,
    val averagePriceMilli: Long?,
    val extremes: Extremes,
) {
    val byEndId: Map<Long, ConsumptionInterval> = intervals.associateBy { it.endId }
    val latest: ConsumptionInterval? get() = intervals.lastOrNull()

    companion object {
        val EMPTY = FuelSummary(emptyList(), null, Extremes.NONE)

        fun of(records: List<Refuel>): FuelSummary {
            val intervals = FuelStats.intervals(records)
            return FuelSummary(intervals, FuelStats.averagePriceMilli(records), Extremes.of(records, intervals))
        }
    }
}

object FuelStats {

    fun chronological(records: List<Refuel>): List<Refuel> =
        records.sortedWith(compareBy<Refuel>({ it.timestamp }, { it.odometerKm }))

    /**
     * Full-tank-to-full-tank method:
     * - Records before the first full tank are ignored (the starting tank level is unknown).
     * - Partial refuellings are accumulated into the next full-tank interval.
     * - If the odometer does not strictly increase, the sequence is broken (typo, missing record,
     *   odometer replaced): the pending interval is discarded instead of reporting a wrong figure,
     *   and measurement restarts at the next full tank.
     */
    fun intervals(records: List<Refuel>): List<ConsumptionInterval> {
        val result = mutableListOf<ConsumptionInterval>()
        var anchor: Refuel? = null
        var volume = 0L
        var cost = 0L
        var previousOdometer: Long? = null

        for (r in chronological(records)) {
            if (previousOdometer != null && r.odometerKm <= previousOdometer) anchor = null
            previousOdometer = r.odometerKm

            val start = anchor
            if (start == null) {
                if (r.fullTank) { anchor = r; volume = 0; cost = 0 }
                continue
            }
            volume += r.volumeMl
            cost += r.totalCents
            if (r.fullTank) {
                result += ConsumptionInterval(start.id, r.id, r.timestamp, r.odometerKm - start.odometerKm, volume, cost)
                anchor = r; volume = 0; cost = 0
            }
        }
        return result
    }

    /** Distance-weighted average over all intervals (total fuel / total distance), or null. */
    fun averageConsumption(intervals: List<ConsumptionInterval>): Double? {
        val km = intervals.sumOf { it.distanceKm }
        return if (km == 0L) null else intervals.sumOf { it.volumeMl } / (10.0 * km)
    }

    /** Volume-weighted average price (total paid / total litres) in thousandths per litre, or null. */
    fun averagePriceMilli(records: List<Refuel>): Long? {
        val ml = records.sumOf { it.volumeMl }
        if (ml == 0L) return null
        // cents × 10 = thousandths; ÷ (mL / 1000) = per litre  →  cents × 10 000 / mL, rounded half-up.
        return (records.sumOf { it.totalCents } * 10_000 + ml / 2) / ml
    }

    fun spentCents(records: List<Refuel>): Long = records.sumOf { it.totalCents }
    fun volumeMl(records: List<Refuel>): Long = records.sumOf { it.volumeMl }
}
