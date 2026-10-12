package com.deivon.fuellog.logic

import com.deivon.fuellog.data.Refuel
import java.util.Locale

/** Raw text of the add/edit form. Kept as text so the user can type freely; parsed on save. */
data class RefuelForm(
    val id: Long = 0,
    val timestamp: Long,
    val station: String = "",
    val location: String = "",
    val odometer: String = "",
    val litres: String = "",
    val price: String = "",
    /** Only meaningful when [totalManual]; otherwise the total is calculated. */
    val total: String = "",
    val totalManual: Boolean = false,
    val fullTank: Boolean = true,
    val notes: String = "",
) {
    /** Litres × price, or null while either is missing/invalid. */
    fun calculatedTotalCents(): Long? {
        val ml = Decimals.parse(litres, 3) ?: return null
        val price = Decimals.parse(price, 3) ?: return null
        return totalCents(ml, price)
    }

    companion object {
        fun from(r: Refuel, locale: Locale) = RefuelForm(
            id = r.id, timestamp = r.timestamp, station = r.station, location = r.location,
            odometer = r.odometerKm.toString(),
            litres = Decimals.toInput(r.volumeMl, 3, locale),
            price = Decimals.toInput(r.pricePerLitreMilli, 3, locale),
            total = if (r.totalManual) Decimals.toInput(r.totalCents, 2, locale) else "",
            totalManual = r.totalManual, fullTank = r.fullTank, notes = r.notes,
        )
    }
}

enum class FormError { Station, Odometer, Litres, Price, Total, OdometerNotAfterPrevious, OdometerNotBeforeNext }

sealed interface FormResult {
    data class Valid(val refuel: Refuel) : FormResult
    data class Invalid(val errors: Set<FormError>) : FormResult
}

object RefuelValidator {

    /**
     * Validates the form against the other records of the same vehicle.
     * The odometer must be strictly between the previous and the next record by date, so an
     * impossible regression is rejected; to fix an older mistake the user edits that record.
     */
    fun validate(form: RefuelForm, others: List<Refuel>, vehicleId: Long = 1): FormResult {
        val errors = mutableSetOf<FormError>()
        if (form.station.isBlank()) errors += FormError.Station

        val odometer = Decimals.parse(form.odometer, 0)
        val ml = Decimals.parse(form.litres, 3)?.takeIf { it > 0 }
        val price = Decimals.parse(form.price, 3)?.takeIf { it > 0 }
        if (odometer == null) errors += FormError.Odometer
        if (ml == null) errors += FormError.Litres
        if (price == null) errors += FormError.Price

        val total = if (form.totalManual) Decimals.parse(form.total, 2) else if (ml != null && price != null) totalCents(ml, price) else null
        if (form.totalManual && total == null) errors += FormError.Total

        if (odometer != null) {
            val rest = others.filter { it.id != form.id }
            val previous = rest.filter { it.timestamp <= form.timestamp }.maxByOrNull { it.timestamp }
            val next = rest.filter { it.timestamp > form.timestamp }.minByOrNull { it.timestamp }
            if (previous != null && odometer <= previous.odometerKm) errors += FormError.OdometerNotAfterPrevious
            if (next != null && odometer >= next.odometerKm) errors += FormError.OdometerNotBeforeNext
        }

        if (errors.isNotEmpty()) return FormResult.Invalid(errors)
        return FormResult.Valid(
            Refuel(
                id = form.id, vehicleId = vehicleId, timestamp = form.timestamp,
                station = form.station.trim(), location = form.location.trim(),
                odometerKm = odometer!!, volumeMl = ml!!, pricePerLitreMilli = price!!,
                totalCents = total!!, totalManual = form.totalManual, fullTank = form.fullTank,
                notes = form.notes.trim(),
            )
        )
    }
}
