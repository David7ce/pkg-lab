package com.deivon.recetario.logic

import com.deivon.recetario.model.Ingredient
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

/**
 * Reads a schema.org/Recipe from a web page's JSON-LD. Only what the page states is taken;
 * missing parts are reported in [Imported.warnings] and left empty for the user to fill in.
 */
object RecipePage {
    enum class Warning { NoTitle, NoIngredients, NoSteps, OnlyTotalTime }

    data class Imported(
        val title: String,
        val ingredients: List<Ingredient>,
        val steps: List<String>,
        val servings: Int?,
        val prepMinutes: Int?,
        val cookMinutes: Int?,
        val totalMinutes: Int?,
        val tags: List<String>,
        val imageUrl: String?,
        /** The page's description, kept as the recipe's notes. */
        val notes: String?,
        val warnings: List<Warning>,
    )

    private val scriptTag = Regex(
        // [\s\S] instead of "." + DOT_MATCHES_ALL, which is not available on every Kotlin target.
        "<script[^>]*type\\s*=\\s*[\"']?application/ld\\+json[\"']?[^>]*>([\\s\\S]*?)</script>",
        RegexOption.IGNORE_CASE,
    )
    private val lenient = Json { isLenient = true }

    /** Null when the page has no schema.org Recipe. */
    fun parse(html: String): Imported? {
        for (block in scriptTag.findAll(html)) {
            val element = try {
                lenient.parseToJsonElement(block.groupValues[1].trim().removePrefix("<!--").removeSuffix("-->"))
            } catch (e: SerializationException) {
                continue // one broken block does not make the page unusable; try the next one
            } catch (e: IllegalArgumentException) {
                continue
            }
            findRecipe(element, depth = 0)?.let { return map(it) }
        }
        return null
    }

    private fun findRecipe(e: JsonElement, depth: Int): JsonObject? {
        if (depth > 8) return null
        return when (e) {
            is JsonArray -> e.firstNotNullOfOrNull { findRecipe(it, depth + 1) }
            is JsonObject -> when {
                isRecipe(e) -> e
                else -> e.values.firstNotNullOfOrNull { findRecipe(it, depth + 1) }
            }
            else -> null
        }
    }

    internal fun isRecipe(o: JsonObject): Boolean = when (val t = o["@type"]) {
        is JsonPrimitive -> t.contentOrNull.isRecipeType()
        is JsonArray -> t.any { (it as? JsonPrimitive)?.contentOrNull.isRecipeType() }
        else -> false
    }

    private fun String?.isRecipeType() = this == "Recipe" || this?.endsWith("/Recipe") == true

    private fun map(o: JsonObject): Imported {
        val title = text(o["name"]).orEmpty()
        val ingredients = strings(o["recipeIngredient"] ?: o["ingredients"]).mapNotNull(IngredientParser::parse)
        val steps = instructions(o["recipeInstructions"])
        val prep = Durations.minutes(text(o["prepTime"]))
        val cook = Durations.minutes(text(o["cookTime"]))
        val total = Durations.minutes(text(o["totalTime"]))
        val tags = (strings(o["recipeCategory"]) + strings(o["recipeCuisine"]))
            .flatMap { it.split(',') }
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.length <= RecipeRules.MAX_TAG }
            .distinctBy(Text::fold)
            .take(10)
        val warnings = buildList {
            if (title.isEmpty()) add(Warning.NoTitle)
            if (ingredients.isEmpty()) add(Warning.NoIngredients)
            if (steps.isEmpty()) add(Warning.NoSteps)
            if (prep == null && cook == null && total != null) add(Warning.OnlyTotalTime)
        }
        return Imported(
            title = title.take(RecipeRules.MAX_TITLE),
            ingredients = ingredients.take(RecipeRules.MAX_ITEMS),
            steps = steps.take(RecipeRules.MAX_ITEMS).map { it.take(RecipeRules.MAX_TEXT) },
            servings = servings(o["recipeYield"]),
            prepMinutes = prep,
            cookMinutes = cook,
            totalMinutes = total,
            tags = tags,
            imageUrl = imageUrl(o["image"]),
            notes = multiline(o["description"])?.take(RecipeRules.MAX_NOTES),
            warnings = warnings,
        )
    }

    internal fun text(e: JsonElement?): String? = when (e) {
        is JsonPrimitive -> e.contentOrNull?.let(Text::plain)?.ifEmpty { null }
        is JsonArray -> e.firstNotNullOfOrNull { text(it) }
        is JsonObject -> text(e["@value"] ?: e["name"] ?: e["text"])
        else -> null
    }

    internal fun strings(e: JsonElement?): List<String> = when (e) {
        is JsonArray -> e.mapNotNull { text(it) }
        null -> emptyList()
        else -> listOfNotNull(text(e))
    }

    /** Accepts a string, a list of strings, HowToStep objects and HowToSection groups (flattened). */
    internal fun instructions(e: JsonElement?): List<String> = when (e) {
        null -> emptyList()
        is JsonPrimitive -> e.contentOrNull?.let(::splitInstructionText).orEmpty()
        is JsonArray -> e.flatMap { instructions(it) }
        is JsonObject -> when {
            e["itemListElement"] != null -> instructions(e["itemListElement"])
            else -> listOfNotNull(text(e["text"]) ?: text(e["name"]))
        }
    }

    /** Like [text], but keeps line breaks (for descriptions and notes). */
    internal fun multiline(e: JsonElement?): String? = when (e) {
        is JsonPrimitive -> e.contentOrNull?.let(Text::plainLines)?.ifEmpty { null }
        is JsonArray -> e.mapNotNull { multiline(it) }.joinToString("\n\n").ifEmpty { null }
        is JsonObject -> multiline(e["text"] ?: e["@value"])
        else -> null
    }

    private val blockBreak = Regex("<\\s*(br|/p|/li|/div)\\s*/?>|\\n", RegexOption.IGNORE_CASE)
    private val numbering = Regex("^\\d{1,2}[.)]\\s+")

    private fun splitInstructionText(raw: String): List<String> =
        raw.split(blockBreak).map { Text.plain(it).replace(numbering, "") }.filter { it.isNotEmpty() }

    internal fun servings(e: JsonElement?): Int? = when (e) {
        is JsonPrimitive -> e.doubleOrNull?.toInt() ?: e.contentOrNull?.let { Regex("\\d+").find(it)?.value?.toIntOrNull() }
        is JsonArray -> e.firstNotNullOfOrNull { servings(it) }
        else -> null
    }?.takeIf { it in 1..RecipeRules.MAX_SERVINGS }

    internal fun imageUrl(e: JsonElement?): String? = when (e) {
        is JsonPrimitive -> e.contentOrNull?.trim()?.takeIf { it.startsWith("http") }
        is JsonArray -> e.firstNotNullOfOrNull { imageUrl(it) }
        is JsonObject -> imageUrl(e["url"] ?: e["contentUrl"])
        else -> null
    }
}

/** ISO 8601 durations as used by schema.org ("PT1H30M", "P0DT45M", "PT2700S"). */
object Durations {
    private val iso = Regex(
        "^P(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)W)?(?:(\\d+)D)?(?:T(?:(\\d+(?:\\.\\d+)?)H)?(?:(\\d+(?:\\.\\d+)?)M)?(?:(\\d+(?:\\.\\d+)?)S)?)?$",
        RegexOption.IGNORE_CASE,
    )

    // A designator with no number before it, as some exporters write ("PT1HM", "PTM"): dropped.
    private val emptyDesignator = Regex("(?<=[PTHMS])[HMS]", RegexOption.IGNORE_CASE)

    private val hoursMinutes = Regex("^(\\d+)\\s*h(?:oras?|ours?|rs?)?\\.?\\s*(?:(\\d+)\\s*(?:m|min|mins|minutos|minutes)?\\.?)?$", RegexOption.IGNORE_CASE)
    private val onlyMinutes = Regex("^(\\d+)\\s*(?:m|min|mins|minutos|minutes)\\.?$", RegexOption.IGNORE_CASE)
    private val clock = Regex("^(\\d+):(\\d{2})$")

    /**
     * Times as people and apps type them: "30" (minutes), "2 h 40", "1h", "45 min", "1:30", or ISO 8601.
     * Null when blank, not understood, zero or out of range.
     */
    fun loose(text: String?): Int? {
        val s = text?.trim()?.ifEmpty { null } ?: return null
        val minutes = s.toIntOrNull()
            ?: hoursMinutes.matchEntire(s)?.let { it.groupValues[1].toInt() * 60 + (it.groupValues[2].toIntOrNull() ?: 0) }
            ?: onlyMinutes.matchEntire(s)?.groupValues?.get(1)?.toIntOrNull()
            ?: clock.matchEntire(s)?.let { it.groupValues[1].toInt() * 60 + it.groupValues[2].toInt() }
            ?: return minutes(s)
        return minutes.takeIf { it in 1..RecipeRules.MAX_MINUTES }
    }

    /** Whole minutes, rounded; null for blank, malformed, zero or implausible (years/months) values. */
    fun minutes(text: String?): Int? {
        val cleaned = text?.trim()?.replace(emptyDesignator, "") ?: return null
        val m = iso.matchEntire(cleaned) ?: return null
        val g = m.groupValues
        if ((g[1].toLongOrNull() ?: 0) > 0 || (g[2].toLongOrNull() ?: 0) > 0) return null
        val total = (g[3].toDoubleOrNull() ?: 0.0) * 7 * 24 * 60 +
            (g[4].toDoubleOrNull() ?: 0.0) * 24 * 60 +
            (g[5].toDoubleOrNull() ?: 0.0) * 60 +
            (g[6].toDoubleOrNull() ?: 0.0) +
            (g[7].toDoubleOrNull() ?: 0.0) / 60
        val rounded = kotlin.math.round(total).toInt()
        return rounded.takeIf { it in 1..RecipeRules.MAX_MINUTES }
    }
}
