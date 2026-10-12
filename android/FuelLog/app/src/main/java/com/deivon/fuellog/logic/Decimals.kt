package com.deivon.fuellog.logic

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/** Conversion between user text and exact scaled integers (value × 10^scale). */
object Decimals {
    private val plainNumber = Regex("""\d+([.,]\d*)?|[.,]\d+""")

    /**
     * Parses "1,459" or "1.459" (either separator, no grouping) into 1459 for scale 3.
     * Returns null for empty, negative, non-numeric or over-precise input (more decimals than [scale]).
     */
    fun parse(text: String, scale: Int): Long? {
        val t = text.trim()
        if (!plainNumber.matches(t)) return null
        return try {
            BigDecimal(t.replace(',', '.')).movePointRight(scale).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
        } catch (e: ArithmeticException) {
            null
        }
    }

    /** Locale-aware display, e.g. 45200 (scale 3) → "45,20" in Spanish with [minFraction] = 2. */
    fun format(value: Long, scale: Int, locale: Locale, minFraction: Int = scale, maxFraction: Int = scale): String =
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = minFraction
            maximumFractionDigits = maxFraction
        }.format(BigDecimal.valueOf(value, scale))

    /** For pre-filling input fields: locale separator, no grouping, no trailing zeros. */
    fun toInput(value: Long, scale: Int, locale: Locale): String =
        NumberFormat.getNumberInstance(locale).apply {
            isGroupingUsed = false
            minimumFractionDigits = 0
            maximumFractionDigits = scale
        }.format(BigDecimal.valueOf(value, scale))

    /** Machine-readable ("45.2"), used in CSV. */
    fun toPlain(value: Long, scale: Int): String = BigDecimal.valueOf(value, scale).stripTrailingZeros().toPlainString()
}

/**
 * Total in cents = litres × price per litre, rounded half-up to the cent.
 * millilitres × thousandths-per-litre = 10^-6 currency units; ÷ 10^4 → cents. Inputs are non-negative.
 */
fun totalCents(volumeMl: Long, pricePerLitreMilli: Long): Long = (volumeMl * pricePerLitreMilli + 5_000) / 10_000
