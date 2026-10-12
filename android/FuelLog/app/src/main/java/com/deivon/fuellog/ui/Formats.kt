package com.deivon.fuellog.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.deivon.fuellog.logic.Decimals
import java.math.BigDecimal
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

/** Locale-aware display of the app's exact integer quantities. */
class Formats(val locale: Locale, currencyCode: String) {
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

    fun money(cents: Long): String = money2.format(BigDecimal.valueOf(cents, 2))
    fun pricePerLitre(milli: Long): String = money3.format(BigDecimal.valueOf(milli, 3)) + "/L"
    fun perKm(value: BigDecimal): String = money3.format(value) + "/km"
    fun litres(ml: Long): String = Decimals.format(ml, 3, locale, minFraction = 2, maxFraction = 2) + " L"
    fun km(km: Long): String = integer.format(km) + " km"
    fun consumption(l100: Double): String = oneDecimal.format(l100) + " L/100 km"
    /** Signed percentage, e.g. "+12 %" / "−8 %", with locale digits. */
    fun percent(value: Int): String = (if (value > 0) "+" else if (value < 0) "−" else "") + percentFormat.format(kotlin.math.abs(value) / 100.0)
    fun date(millis: Long): String = date.format(Date(millis))
    fun time(millis: Long): String = time.format(Date(millis))
}

@Composable
fun rememberFormats(currency: String): Formats {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale, currency) { Formats(locale, currency) }
}
