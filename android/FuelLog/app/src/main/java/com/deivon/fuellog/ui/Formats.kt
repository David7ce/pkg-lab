package com.david7ce.fuellog.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.david7ce.fuellog.logic.Decimals
import com.david7ce.fuellog.logic.UnitSystem
import com.david7ce.fuellog.logic.Units
import java.math.BigDecimal
import java.text.DateFormat
import java.text.NumberFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Date
import java.util.Locale

/** Locale- and unit-aware display of the app's exact metric integers. */
class Formats(val locale: Locale, currencyCode: String, val units: UnitSystem) {
    private val currency = Currency.getInstance(currencyCode)
    private fun money(digits: Int) = NumberFormat.getCurrencyInstance(locale).apply {
        this.currency = this@Formats.currency
        minimumFractionDigits = digits
        maximumFractionDigits = digits
    }
    private val money2 = money(2)
    private val money3 = money(3)
    private val oneDecimal = NumberFormat.getNumberInstance(locale).apply { minimumFractionDigits = 1; maximumFractionDigits = 1 }
    private val integer = NumberFormat.getIntegerInstance(locale)
    private val percentFormat = NumberFormat.getPercentInstance(locale)
    private val date = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
    private val time = DateFormat.getTimeInstance(DateFormat.SHORT, locale)
    private val month = DateTimeFormatter.ofPattern("LLLL yyyy", locale)

    val distanceUnit: String = if (units.usesMiles) "mi" else "km"
    val volumeUnit: String = if (units.usesGallons) "gal" else "L"
    val consumptionUnit: String = if (units == UnitSystem.Metric) "L/100 km" else "mpg"

    fun money(cents: Long): String = money2.format(BigDecimal.valueOf(cents, 2))
    fun pricePerVolume(milliPerLitre: Long): String =
        money3.format(BigDecimal.valueOf(Units.priceFromPerLitre(milliPerLitre, units), 3)) + "/" + volumeUnit
    fun perDistance(costPerKm: BigDecimal): String = money3.format(Units.costPerDistance(costPerKm, units)) + "/" + distanceUnit
    fun volume(ml: Long): String = Decimals.format(Units.mlToVolume(ml, units), 3, locale, minFraction = 2, maxFraction = 2) + " " + volumeUnit
    fun distance(km: Long): String = integer.format(Units.distance(km, units)) + " " + distanceUnit
    /** Consumption value only, in the unit system's figure (L/100 km or mpg). */
    fun consumptionValue(l100: Double): Double = Units.consumption(l100, units)
    fun consumption(l100: Double): String = oneDecimal.format(consumptionValue(l100)) + " " + consumptionUnit
    fun number1(value: Double): String = oneDecimal.format(value)
    /** Signed percentage, e.g. "+12 %" / "−8 %", with locale digits. */
    fun percent(value: Int): String = (if (value > 0) "+" else if (value < 0) "−" else "") + percentFormat.format(kotlin.math.abs(value) / 100.0)
    fun date(millis: Long): String = date.format(Date(millis))
    fun time(millis: Long): String = time.format(Date(millis))
    fun month(ym: YearMonth): String = month.format(ym).replaceFirstChar { it.titlecase(locale) }

    companion object {
        fun yearMonth(millis: Long): YearMonth = YearMonth.from(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
    }
}

@Composable
fun rememberFormats(currency: String, units: UnitSystem): Formats {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale, currency, units) { Formats(locale, currency, units) }
}
