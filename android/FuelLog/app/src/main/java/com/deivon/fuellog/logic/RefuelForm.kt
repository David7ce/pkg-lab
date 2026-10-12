package com.deivon.fuellog.logic

import com.deivon.fuellog.data.Refuel
import java.util.Locale
import kotlin.math.abs

/**
 * Raw text of the add/edit form, in the user's units. Kept as text so the user can type freely;
 * parsed on save. With [byAmount] the user types the money paid ("50 €") instead of the volume,
 * and the volume is derived from the price.
 */
data class RefuelForm(
    val id: Long = 0,
    val timestamp: Long,
    val station: String = "",
    val odometer: String = "",
    val litres: String = "",
    val price: String = "",
    val byAmount: Boolean = false,
    val amount: String = "",
    val fullTank: Boolean = true,
    val missedPrevious: Boolean = false,
    val notes: String = "",
    val location: GeoPoint? = null,
) {
    /** Price in thousandths per litre, or null while missing/invalid. */
    fun pricePerLitre(u: UnitSystem): Long? =
        Decimals.parse(price, 3)?.takeIf { it > 0 }?.let { Units.priceToPerLitre(it, u) }?.takeIf { it > 0 }

    /** Volume in mL: typed, or derived from amount ÷ price (rounded to the nearest mL). */
    fun volumeMl(u: UnitSystem): Long? {
        if (!byAmount) return Decimals.parse(litres, 3)?.let { Units.volumeToMl(it, u) }?.takeIf { it > 0 }
        val cents = Decimals.parse(amount, 2)?.takeIf { it > 0 } ?: return null
        val price = pricePerLitre(u) ?: return null
        // cents / (milli per L) = cents × 10 000 / milli  mL  (1 cent = 10 thousandths; 1 L = 1000 mL)
        return ((cents * 10_000 + price / 2) / price).takeIf { it > 0 }
    }

    /** Volume × price, or null while either is missing. The total is never typed directly. */
    fun totalCents(u: UnitSystem): Long? {
        val ml = volumeMl(u) ?: return null
        val price = pricePerLitre(u) ?: return null
        return totalCents(ml, price)
    }

    companion object {
        fun from(r: Refuel, locale: Locale, u: UnitSystem) = RefuelForm(
            id = r.id, timestamp = r.timestamp, station = r.station,
            odometer = Units.kmToOdometer(r.odometerKm, u).toString(),
            litres = Decimals.toInput(Units.mlToVolume(r.volumeMl, u), 3, locale),
            price = Decimals.toInput(Units.priceFromPerLitre(r.pricePerLitreMilli, u), 3, locale),
            fullTank = r.fullTank, missedPrevious = r.missedPrevious, notes = r.notes,
            location = GeoPoint.of(r.latitude, r.longitude),
        )
    }
}

enum class FormError { Station, Odometer, Litres, Amount, Price, OdometerNotAfterPrevious, OdometerNotBeforeNext }

/** Plausibility warnings: the record is valid but probably mistyped, so the user is asked to confirm. */
sealed interface FormWarning {
    data class LongDistance(val km: Long) : FormWarning
    data class OddConsumption(val l100: Double) : FormWarning
    data class OddPrice(val averageMilli: Long) : FormWarning
}

sealed interface FormResult {
    data class Valid(val refuel: Refuel) : FormResult
    data class Invalid(val errors: Set<FormError>) : FormResult
}

object RefuelValidator {

    const val MAX_PLAUSIBLE_KM = 2_500L
    const val MIN_PLAUSIBLE_L100 = 2.0
    const val MAX_PLAUSIBLE_L100 = 30.0
    /** Relative difference from the average price that triggers a warning. */
    const val PRICE_TOLERANCE = 0.4

    /**
     * Validates the form against the other records of the same vehicle ([others]).
     * The odometer must be strictly between the previous and the next record by date, so an
     * impossible regression is rejected; to fix an older mistake the user edits that record.
     */
    fun validate(form: RefuelForm, others: List<Refuel>, vehicleId: Long, u: UnitSystem = UnitSystem.Metric): FormResult {
        val errors = mutableSetOf<FormError>()
        if (form.station.isBlank()) errors += FormError.Station

        val odometer = Decimals.parse(form.odometer, 0)?.let { Units.odometerToKm(it, u) }
        val price = form.pricePerLitre(u)
        val ml = form.volumeMl(u)
        if (odometer == null) errors += FormError.Odometer
        if (price == null) errors += FormError.Price
        if (ml == null) {
            if (!form.byAmount) errors += FormError.Litres
            else if (Decimals.parse(form.amount, 2)?.takeIf { it > 0 } == null) errors += FormError.Amount
        }

        if (odometer != null) {
            val rest = others.filter { it.id != form.id && it.vehicleId == vehicleId }
            val previous = rest.filter { it.timestamp <= form.timestamp }.maxByOrNull { it.timestamp }
            val next = rest.filter { it.timestamp > form.timestamp }.minByOrNull { it.timestamp }
            if (previous != null && odometer <= previous.odometerKm) errors += FormError.OdometerNotAfterPrevious
            if (next != null && odometer >= next.odometerKm) errors += FormError.OdometerNotBeforeNext
        }

        if (errors.isNotEmpty()) return FormResult.Invalid(errors)
        return FormResult.Valid(
            Refuel(
                id = form.id, vehicleId = vehicleId, timestamp = form.timestamp,
                station = form.station.trim(), odometerKm = odometer!!, volumeMl = ml!!,
                pricePerLitreMilli = price!!, totalCents = totalCents(ml, price),
                fullTank = form.fullTank, missedPrevious = form.missedPrevious, notes = form.notes.trim(),
                latitude = form.location?.lat, longitude = form.location?.lon,
            )
        )
    }

    /** Checks a valid record against the vehicle's history ([others], same vehicle) for likely typos. */
    fun warnings(refuel: Refuel, others: List<Refuel>): List<FormWarning> {
        val rest = others.filter { it.id != refuel.id && it.vehicleId == refuel.vehicleId }
        val result = mutableListOf<FormWarning>()

        val previous = rest.filter { it.timestamp <= refuel.timestamp }.maxByOrNull { it.timestamp }
        if (previous != null && !refuel.missedPrevious) {
            val km = refuel.odometerKm - previous.odometerKm
            if (km > MAX_PLAUSIBLE_KM) result += FormWarning.LongDistance(km)
        }

        // A new record has id 0, which cannot be told apart in the interval list: use a sentinel.
        val probe = if (refuel.id == 0L) refuel.copy(id = -1) else refuel
        FuelStats.intervals(rest + probe).firstOrNull { it.endId == probe.id }?.let {
            if (it.litresPer100Km < MIN_PLAUSIBLE_L100 || it.litresPer100Km > MAX_PLAUSIBLE_L100) {
                result += FormWarning.OddConsumption(it.litresPer100Km)
            }
        }

        FuelStats.averagePriceMilli(rest)?.let { avg ->
            if (rest.size >= 3 && abs(refuel.pricePerLitreMilli - avg) > avg * PRICE_TOLERANCE) result += FormWarning.OddPrice(avg)
        }
        return result
    }
}
