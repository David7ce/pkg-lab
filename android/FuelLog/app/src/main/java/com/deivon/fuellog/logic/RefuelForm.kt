package com.deivon.fuellog.logic

import com.deivon.fuellog.data.Refuel
import java.util.Locale

/** Raw text of the add/edit form. Kept as text so the user can type freely; parsed on save. */
data class RefuelForm(
    val id: Long = 0,
    val timestamp: Long,
    val station: String = "",
    val odometer: String = "",
    val litres: String = "",
    val price: String = "",
    val fullTank: Boolean = true,
    val missedPrevious: Boolean = false,
    val notes: String = "",
    val location: GeoPoint? = null,
) {
    /** Litres × price, or null while either is missing/invalid. The total is never typed by hand. */
    fun totalCents(): Long? {
        val ml = Decimals.parse(litres, 3) ?: return null
        val price = Decimals.parse(price, 3) ?: return null
        return totalCents(ml, price)
    }

    companion object {
        fun from(r: Refuel, locale: Locale) = RefuelForm(
            id = r.id, timestamp = r.timestamp, station = r.station,
            odometer = r.odometerKm.toString(),
            litres = Decimals.toInput(r.volumeMl, 3, locale),
            price = Decimals.toInput(r.pricePerLitreMilli, 3, locale),
            fullTank = r.fullTank, missedPrevious = r.missedPrevious, notes = r.notes,
            location = GeoPoint.of(r.latitude, r.longitude),
        )
    }
}

enum class FormError { Station, Odometer, Litres, Price, OdometerNotAfterPrevious, OdometerNotBeforeNext }

sealed interface FormResult {
    data class Valid(val refuel: Refuel) : FormResult
    data class Invalid(val errors: Set<FormError>) : FormResult
}

object RefuelValidator {

    /**
     * Validates the form against the other records of the same vehicle ([others]).
     * The odometer must be strictly between the previous and the next record by date, so an
     * impossible regression is rejected; to fix an older mistake the user edits that record.
     */
    fun validate(form: RefuelForm, others: List<Refuel>, vehicleId: Long): FormResult {
        val errors = mutableSetOf<FormError>()
        if (form.station.isBlank()) errors += FormError.Station

        val odometer = Decimals.parse(form.odometer, 0)
        val ml = Decimals.parse(form.litres, 3)?.takeIf { it > 0 }
        val price = Decimals.parse(form.price, 3)?.takeIf { it > 0 }
        if (odometer == null) errors += FormError.Odometer
        if (ml == null) errors += FormError.Litres
        if (price == null) errors += FormError.Price

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
}
