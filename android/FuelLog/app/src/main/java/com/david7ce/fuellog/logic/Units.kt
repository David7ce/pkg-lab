package com.david7ce.fuellog.logic

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Display/input unit systems. Storage is always metric (km, mL, thousandths per litre), so switching
 * units never changes data. UK pumps sell litres but people quote miles and mpg (imperial gallon);
 * the US uses miles, US gallons and mpg.
 */
enum class UnitSystem(val key: String) {
    Metric("metric"), Uk("uk"), Us("us");

    val usesMiles: Boolean get() = this != Metric
    val usesGallons: Boolean get() = this == Us

    companion object {
        fun of(key: String?): UnitSystem = entries.firstOrNull { it.key == key } ?: Metric
    }
}

/**
 * Exact conversions with BigDecimal, rounded half-up to the stored integer unit.
 * A price typed per US gallon is stored per litre to the thousandth, so a US total can differ from
 * the pump receipt by about a cent; the stored figures stay internally consistent.
 */
object Units {
    private val KM_PER_MILE = BigDecimal("1.609344")
    private val ML_PER_US_GALLON = BigDecimal("3.785411784") // thousandths of a gallon → mL
    /** mpg = K / (L/100 km). */
    private const val MPG_UK = 282.480936
    private const val MPG_US = 235.214583

    private fun BigDecimal.toLongRounded(): Long = setScale(0, RoundingMode.HALF_UP).longValueExact()

    fun odometerToKm(value: Long, u: UnitSystem): Long =
        if (u.usesMiles) BigDecimal.valueOf(value).multiply(KM_PER_MILE).toLongRounded() else value

    fun kmToOdometer(km: Long, u: UnitSystem): Long =
        if (u.usesMiles) BigDecimal.valueOf(km).divide(KM_PER_MILE, 6, RoundingMode.HALF_UP).toLongRounded() else km

    /** Volume typed with 3 decimals (thousandths of a litre or of a US gallon) → mL. */
    fun volumeToMl(thousandths: Long, u: UnitSystem): Long =
        if (u.usesGallons) BigDecimal.valueOf(thousandths).multiply(ML_PER_US_GALLON).toLongRounded() else thousandths

    fun mlToVolume(ml: Long, u: UnitSystem): Long =
        if (u.usesGallons) BigDecimal.valueOf(ml).divide(ML_PER_US_GALLON, 6, RoundingMode.HALF_UP).toLongRounded() else ml

    /** Price typed in thousandths per litre or per US gallon → thousandths per litre. */
    fun priceToPerLitre(milli: Long, u: UnitSystem): Long =
        if (u.usesGallons) BigDecimal.valueOf(milli).divide(ML_PER_US_GALLON, 6, RoundingMode.HALF_UP).toLongRounded() else milli

    fun priceFromPerLitre(milliPerLitre: Long, u: UnitSystem): Long =
        if (u.usesGallons) BigDecimal.valueOf(milliPerLitre).multiply(ML_PER_US_GALLON).toLongRounded() else milliPerLitre

    /** L/100 km → the unit system's consumption figure (L/100 km or mpg). */
    fun consumption(l100: Double, u: UnitSystem): Double = when (u) {
        UnitSystem.Metric -> l100
        UnitSystem.Uk -> MPG_UK / l100
        UnitSystem.Us -> MPG_US / l100
    }

    /** Inverse of [consumption], to tenths of L/100 km (the stored reference). Null if not positive. */
    fun consumptionToDl100(value: Double, u: UnitSystem): Int? {
        if (value <= 0) return null
        val l100 = when (u) {
            UnitSystem.Metric -> value
            UnitSystem.Uk -> MPG_UK / value
            UnitSystem.Us -> MPG_US / value
        }
        return Math.round(l100 * 10).toInt()
    }

    fun distance(km: Long, u: UnitSystem): Long = kmToOdometer(km, u)

    /** Cost per km → cost per mile when needed, 3 decimals. */
    fun costPerDistance(costPerKm: BigDecimal, u: UnitSystem): BigDecimal =
        if (u.usesMiles) costPerKm.multiply(KM_PER_MILE).setScale(3, RoundingMode.HALF_UP) else costPerKm
}
