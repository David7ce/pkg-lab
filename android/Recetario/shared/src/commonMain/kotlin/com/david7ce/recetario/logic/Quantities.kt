package com.david7ce.recetario.logic

import com.david7ce.recetario.model.Ingredient
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

/** Parsing, formatting and scaling of ingredient quantities. */
object Quantities {
    private val unicodeFractions = mapOf(
        '¼' to 0.25, '½' to 0.5, '¾' to 0.75, '⅓' to 1.0 / 3, '⅔' to 2.0 / 3,
        '⅛' to 0.125, '⅜' to 0.375, '⅝' to 0.625, '⅞' to 0.875,
        '⅕' to 0.2, '⅖' to 0.4, '⅗' to 0.6, '⅘' to 0.8, '⅙' to 1.0 / 6, '⅚' to 5.0 / 6,
    )
    private val fractionChars = unicodeFractions.keys.joinToString("")

    /**
     * The quantity forms people write: "1 1/2", "1/2", "2", "2.5", "2,5", "½", "1½", "1 ½".
     * Longest forms first, because a regex alternation takes the first branch that fits.
     */
    internal val pattern =
        "\\d+\\s+\\d+/\\d+|\\d+/\\d+|\\d+(?:[.,]\\d+)?\\s*[$fractionChars]?|[$fractionChars]"

    private val whole = Regex("^(?:$pattern)$")

    /** Parses a whole field. Returns null when the text is not exactly one quantity or is not > 0. */
    fun parse(text: String): Double? {
        val s = text.trim()
        if (!whole.matches(s)) return null
        val value = valueOf(s) ?: return null
        return value.takeIf { it > 0 && it.isFinite() }
    }

    private fun valueOf(s: String): Double? {
        val glyph = s.lastOrNull()?.let { unicodeFractions[it] }
        if (glyph != null) {
            val rest = s.dropLast(1).trim()
            return if (rest.isEmpty()) glyph else rest.replace(',', '.').toDoubleOrNull()?.plus(glyph)
        }
        if ('/' in s) {
            val parts = s.split(Regex("\\s+"))
            val (num, den) = parts.last().split('/').map { it.toLongOrNull() ?: return null }
            if (den == 0L) return null
            val wholePart = if (parts.size == 2) parts[0].toLongOrNull() ?: return null else 0L
            return wholePart + num.toDouble() / den
        }
        return s.replace(',', '.').toDoubleOrNull()
    }

    private val niceFractions = listOf(0.25 to "¼", 1.0 / 3 to "⅓", 0.5 to "½", 2.0 / 3 to "⅔", 0.75 to "¾")

    /**
     * Human formatting: "2", "1½", "0.33" → "⅓", otherwise up to two decimals with [decimalSeparator].
     * Fraction glyphs are only used for small amounts, where cooks expect them.
     */
    fun format(value: Double, decimalSeparator: Char = '.'): String {
        if (!value.isFinite() || value <= 0) return "0"
        if (value >= 1e9) return value.roundToLong().toString()
        val wholePart = floor(value)
        val frac = value - wholePart
        if (value < 10) {
            niceFractions.firstOrNull { abs(frac - it.first) < 0.01 }?.let { (_, glyph) ->
                return (if (wholePart > 0) wholePart.toLong().toString() else "") + glyph
            }
        }
        val hundredths = (value * 100).roundToLong()
        val intPart = hundredths / 100
        val decimals = (hundredths % 100).toString().padStart(2, '0').trimEnd('0')
        return if (decimals.isEmpty()) intPart.toString() else "$intPart$decimalSeparator$decimals"
    }

    /** Multiplies numeric quantities by [factor]. Text-only ingredients are returned unchanged. */
    fun scale(ingredients: List<Ingredient>, factor: Double): List<Ingredient> {
        if (factor == 1.0 || factor <= 0 || !factor.isFinite()) return ingredients
        return ingredients.map { ing -> ing.quantity?.let { ing.copy(quantity = it * factor) } ?: ing }
    }

    /** Scales numeric quantities by to/from servings. Text-only ingredients are returned unchanged. */
    fun scale(ingredients: List<Ingredient>, fromServings: Int, toServings: Int): List<Ingredient> {
        if (fromServings <= 0 || toServings <= 0 || fromServings == toServings) return ingredients
        val factor = toServings.toDouble() / fromServings
        return ingredients.map { ing -> ing.quantity?.let { ing.copy(quantity = it * factor) } ?: ing }
    }
}
