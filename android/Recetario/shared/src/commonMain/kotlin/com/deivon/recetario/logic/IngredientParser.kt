package com.deivon.recetario.logic

import com.deivon.recetario.model.Ingredient

/**
 * Best-effort split of a free-text ingredient line ("1 ½ cups flour, sifted") into quantity,
 * unit, name and note. Nothing is invented: when a line does not start with a plain quantity
 * (ranges like "2-3", words like "a pinch"), the whole line is kept as the name.
 */
object IngredientParser {
    private val units = listOf(
        // metric
        "g", "gr", "grs", "gram", "grams", "gramo", "gramos", "kg", "kilo", "kilos", "kilogram", "kilograms",
        "kilogramo", "kilogramos", "mg", "ml", "l", "litre", "litres", "liter", "liters", "litro", "litros", "cl", "dl",
        // imperial / US
        "tsp", "teaspoon", "teaspoons", "tbsp", "tbs", "tablespoon", "tablespoons", "cup", "cups", "oz", "ounce",
        "ounces", "lb", "lbs", "pound", "pounds", "pint", "pints", "quart", "quarts", "pinch", "pinches",
        "can", "cans", "clove", "cloves", "slice", "slices", "stick", "sticks",
        // Spanish
        "cucharada", "cucharadas", "cda", "cdas", "cucharadita", "cucharaditas", "cdta", "cdtas", "cdita", "cditas",
        "taza", "tazas", "pizca", "pizcas", "diente", "dientes", "lata", "latas", "vaso", "vasos",
        "rebanada", "rebanadas", "sobre", "sobres", "loncha", "lonchas", "ramita", "ramitas",
    ).sortedByDescending { it.length }

    private val leading = Regex("^(${Quantities.pattern})(?=\\s|[A-Za-zÀ-ÿ]|$)")
    private val range = Regex("^\\s*(-|–|to\\b|a\\b|or\\b|o\\b)\\s*\\d", RegexOption.IGNORE_CASE)
    private val connector = Regex("^(of|de|del)\\s+", RegexOption.IGNORE_CASE)

    fun parse(rawLine: String): Ingredient? {
        val line = Text.plain(rawLine)
        if (line.isEmpty()) return null
        val match = leading.find(line) ?: return Ingredient(name = line)
        val rest = line.substring(match.range.last + 1)
        if (range.containsMatchIn(rest)) return Ingredient(name = line)
        val quantity = Quantities.parse(match.value.trim()) ?: return Ingredient(name = line)

        var remainder = rest.trimStart()
        var unit: String? = null
        val lower = remainder.lowercase()
        for (u in units) {
            if (lower.startsWith(u)) {
                val after = remainder.getOrNull(u.length)
                val endsWord = after == null || after == ' ' || after == '.' || after == ','
                if (endsWord) {
                    unit = remainder.substring(0, u.length)
                    remainder = remainder.substring(u.length).removePrefix(".").trimStart()
                    break
                }
            }
        }
        remainder = remainder.replace(connector, "")
        val comma = remainder.indexOf(',')
        val name = (if (comma >= 0) remainder.substring(0, comma) else remainder).trim()
        val note = if (comma >= 0) remainder.substring(comma + 1).trim().ifEmpty { null } else null
        // A quantity with nothing after it ("2") is not an ingredient we can name: keep the text.
        if (name.isEmpty()) return Ingredient(name = line)
        return Ingredient(name = name, quantity = quantity, unit = unit, note = note)
    }
}
