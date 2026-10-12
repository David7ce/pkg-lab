package com.deivon.recetario.logic

import com.deivon.recetario.model.Ingredient
import com.deivon.recetario.model.Recipe
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

/** Where a recipe's photo comes from when importing from another app. */
sealed interface PhotoSource {
    class Bytes(val bytes: ByteArray) : PhotoSource
    class Url(val url: String) : PhotoSource
}

/**
 * Recipes read from another app, ready to be compared with the library. Their [Recipe.imageFile] is
 * always null: photos are in [photos] (by recipe id) and are fetched/normalised only for the recipes
 * actually written. [skipped] counts entries that could not be read as a recipe (e.g. no title).
 */
class ForeignImport(
    val source: Source,
    val recipes: List<Recipe>,
    val photos: Map<String, PhotoSource>,
    val skipped: Int,
) {
    enum class Source { RecipeSage, RecipeBox }
}

/**
 * Labels for information that has no field in Recetario and is kept, labelled, in the notes
 * (in the user's language). Nothing from the source is dropped silently except ratings and favourites.
 */
class NoteLabels(
    val prep: String,
    val cook: String,
    val total: String,
    val inactive: String,
    val nutrition: String,
    val cookware: String,
    val video: String,
    val duration: (Int) -> String,
)

/** A time as written in the source and as understood (null if not understood). */
internal class Timed(val raw: String?, val minutes: Int?)

/**
 * RecipeSage export: `{"recipes": [ schema.org Recipe, … ]}` (also accepted: a bare array).
 * Photos are remote URLs; categories (with their emoji prefixes) become tags as written.
 */
object RecipeSageFormat {
    private fun list(root: JsonElement): JsonArray? = (root as? JsonObject)?.get("recipes") as? JsonArray ?: root as? JsonArray

    fun looksLike(root: JsonElement): Boolean =
        (list(root)?.firstOrNull() as? JsonObject)?.let(RecipePage::isRecipe) ?: false

    fun parse(root: JsonElement, labels: NoteLabels, newId: () -> String, now: Long): ForeignImport {
        val items = list(root) ?: throw ImportException(ImportFailure.NotRecognized)
        val builder = ForeignRecipeBuilder(newId, now)
        val photos = mutableMapOf<String, PhotoSource>()
        val recipes = items.mapNotNull { element ->
            val o = element as? JsonObject
            if (o == null || !RecipePage.isRecipe(o)) {
                builder.skip()
                return@mapNotNull null
            }
            val prep = Timed(RecipePage.text(o["prepTime"]), Durations.minutes(RecipePage.text(o["prepTime"])))
            val cook = Timed(RecipePage.text(o["cookTime"]), Durations.minutes(RecipePage.text(o["cookTime"])))
            val total = Timed(RecipePage.text(o["totalTime"]), Durations.minutes(RecipePage.text(o["totalTime"])))
            val notes = buildList {
                RecipePage.multiline(o["description"])?.let(::add)
                when (val comments = o["comment"]) {
                    is JsonArray -> comments.forEach { c -> RecipePage.multiline((c as? JsonObject)?.get("text"))?.let(::add) }
                    is JsonObject -> RecipePage.multiline(comments["text"])?.let(::add)
                    else -> Unit
                }
                addAll(timeNotes(labels, prep, cook, total))
            }
            builder.build(
                id = RecipePage.text(o["identifier"]),
                title = RecipePage.text(o["name"]),
                notes = notes,
                ingredients = RecipePage.strings(o["recipeIngredient"] ?: o["ingredients"]).mapNotNull(IngredientParser::parse),
                steps = RecipePage.instructions(o["recipeInstructions"]),
                servings = RecipePage.servings(o["recipeYield"]),
                prepMinutes = prep.minutes,
                cookMinutes = cook.minutes,
                tags = RecipePage.strings(o["recipeCategory"]) + RecipePage.strings(o["recipeCuisine"]),
                sourceUrl = RecipePage.text(o["isBasedOn"]) ?: RecipePage.text(o["url"]),
                timestamp = Text.parseDateTime(RecipePage.text(o["dateModified"]) ?: RecipePage.text(o["datePublished"])),
            )?.also { r -> RecipePage.imageUrl(o["image"])?.let { photos[r.id] = PhotoSource.Url(it) } }
        }
        if (recipes.isEmpty()) throw ImportException(ImportFailure.Empty)
        return ForeignImport(ForeignImport.Source.RecipeSage, recipes, photos, builder.skipped)
    }
}

/**
 * My RecipeBox (RecetteTek) `.rtk`: a ZIP with `recipes_N.json` (arrays of recipes), photos as files
 * referenced by name from `pictures`, plus categories/tags/calendar/shopping files (only the recipes'
 * own category and tag titles are used). Ingredients and instructions are plain text blocks.
 */
object RecipeBoxFormat {
    private val recipesFile = Regex("recipes(?:_(\\d+))?\\.json")
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun looksLike(entries: Map<String, ByteArray>): Boolean =
        entries.keys.any { recipesFile.matches(it.substringAfterLast('/')) }

    fun parse(entries: Map<String, ByteArray>, labels: NoteLabels, newId: () -> String, now: Long): ForeignImport {
        val byName = entries.mapKeys { it.key.substringAfterLast('/') }
        val files = entries.keys
            .filter { recipesFile.matches(it.substringAfterLast('/')) }
            .sortedBy { recipesFile.matchEntire(it.substringAfterLast('/'))?.groupValues?.get(1)?.toIntOrNull() ?: 0 }
        val items = files.flatMap { name ->
            val root = try {
                json.parseToJsonElement(entries.getValue(name).decodeToString())
            } catch (e: SerializationException) {
                throw ImportException(ImportFailure.Malformed(name.substringAfterLast('/')))
            } catch (e: IllegalArgumentException) {
                throw ImportException(ImportFailure.Malformed(name.substringAfterLast('/')))
            }
            root as? JsonArray ?: throw ImportException(ImportFailure.Malformed(name.substringAfterLast('/')))
        }

        val builder = ForeignRecipeBuilder(newId, now)
        val photos = mutableMapOf<String, PhotoSource>()
        val recipes = items.mapNotNull { element ->
            val o = element as? JsonObject
            if (o == null) {
                builder.skip()
                return@mapNotNull null
            }
            fun str(key: String): String? = (o[key] as? JsonPrimitive)?.contentOrNull?.trim()?.ifEmpty { null }

            val prep = Timed(str("preparationTime"), Durations.loose(str("preparationTime")))
            val cook = Timed(str("cookingTime"), Durations.loose(str("cookingTime")))
            val total = Timed(str("totalTime"), Durations.loose(str("totalTime")))
            val inactive = Timed(str("inactiveTime"), Durations.loose(str("inactiveTime")))
            val notes = buildList {
                str("description")?.let { add(Text.stripMarkdown(it)) }
                str("notes")?.let { add(Text.stripMarkdown(it)) }
                addAll(timeNotes(labels, prep, cook, total, inactive))
                str("nutrition")?.let { add("${labels.nutrition}: $it") }
                str("cookware")?.let { add("${labels.cookware}: $it") }
                str("video")?.let { add("${labels.video}: $it") }
            }
            builder.build(
                id = str("uuid"),
                title = str("title"),
                notes = notes,
                ingredients = str("ingredients").orEmpty().lines().mapNotNull { IngredientParser.parse(Text.stripMarkdown(it)) },
                steps = paragraphs(str("instructions").orEmpty()).map(Text::stripMarkdown),
                servings = str("quantity")?.let { q -> Regex("\\d+").find(q)?.value?.toIntOrNull() },
                prepMinutes = prep.minutes,
                cookMinutes = cook.minutes,
                tags = titles(o["categories"]) + titles(o["tags"]),
                sourceUrl = str("url"),
                timestamp = Text.parseDateTime(str("lastModifiedDate")) ?: (o["lastModifiedDate"] as? JsonPrimitive)?.longOrNull,
            )?.also { r ->
                val local = (o["pictures"] as? JsonArray)?.firstNotNullOfOrNull { p ->
                    (p as? JsonPrimitive)?.contentOrNull?.substringAfterLast('/')?.let { byName[it] }
                }
                val remote = str("originalPicture")?.takeIf { it.startsWith("http") }
                val photo = local?.let(PhotoSource::Bytes) ?: remote?.let(PhotoSource::Url)
                if (photo != null) photos.put(r.id, photo)
            }
        }
        if (recipes.isEmpty()) throw ImportException(if (builder.skipped > 0) ImportFailure.Malformed("recipes") else ImportFailure.Empty)
        return ForeignImport(ForeignImport.Source.RecipeBox, recipes, photos, builder.skipped)
    }

    private fun titles(e: JsonElement?): List<String> =
        (e as? JsonArray)?.mapNotNull { ((it as? JsonObject)?.get("title") as? JsonPrimitive)?.contentOrNull } ?: emptyList()

    /** Blank-line separated paragraphs; a text without blank lines is split by line. */
    private fun paragraphs(text: String): List<String> {
        val blocks = text.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }
        return if (blocks.size > 1) blocks else text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    }
}

/** Times without a field of their own, or not understood, as labelled note lines. */
internal fun timeNotes(labels: NoteLabels, prep: Timed, cook: Timed, total: Timed, inactive: Timed? = null): List<String> = buildList {
    fun unread(label: String, t: Timed) {
        if (t.minutes == null && t.raw != null && t.raw.any { it.isDigit() }) add("$label: ${t.raw}")
    }
    unread(labels.prep, prep)
    unread(labels.cook, cook)
    unread(labels.total, total)
    // A total adds information only when it is not just prep + cook.
    total.minutes?.let { minutes ->
        val sum = if (prep.minutes == null && cook.minutes == null) null else (prep.minutes ?: 0) + (cook.minutes ?: 0)
        if (sum != minutes) add("${labels.total}: ${labels.duration(minutes)}")
    }
    if (inactive != null) {
        unread(labels.inactive, inactive)
        inactive.minutes?.let { add("${labels.inactive}: ${labels.duration(it)}") }
    }
}

/** Turns loosely-typed source data into valid recipes: trims and caps sizes, never invents content. */
internal class ForeignRecipeBuilder(private val newId: () -> String, private val now: Long) {
    private val seen = mutableSetOf<String>()
    var skipped = 0
        private set

    fun skip() {
        skipped++
    }

    fun build(
        id: String?,
        title: String?,
        notes: List<String>,
        ingredients: List<Ingredient>,
        steps: List<String>,
        servings: Int?,
        prepMinutes: Int?,
        cookMinutes: Int?,
        tags: List<String>,
        sourceUrl: String?,
        timestamp: Long?,
    ): Recipe? {
        val cleanTitle = title?.trim()?.take(RecipeRules.MAX_TITLE).orEmpty()
        if (cleanTitle.isEmpty()) {
            skip()
            return null
        }
        // Keep the source id (so re-importing the same file is recognised), unless unusable or repeated.
        val finalId = id?.trim()?.takeIf { RecipeRules.isValidId(it) && it !in seen } ?: newId()
        val recipe = Recipe(
            id = finalId,
            title = cleanTitle,
            ingredients = ingredients
                .filter { it.name.isNotBlank() }
                .map {
                    it.copy(
                        name = it.name.take(RecipeRules.MAX_TEXT),
                        quantity = it.quantity?.takeIf { q -> q.isFinite() && q > 0 && q <= 1e9 },
                        unit = it.unit?.take(RecipeRules.MAX_TEXT),
                        note = it.note?.take(RecipeRules.MAX_TEXT),
                    )
                }
                .take(RecipeRules.MAX_ITEMS),
            steps = steps.map { it.trim().take(RecipeRules.MAX_TEXT) }.filter { it.isNotEmpty() }.take(RecipeRules.MAX_ITEMS),
            servings = servings?.takeIf { it in 1..RecipeRules.MAX_SERVINGS },
            prepMinutes = prepMinutes?.takeIf { it in 0..RecipeRules.MAX_MINUTES },
            cookMinutes = cookMinutes?.takeIf { it in 0..RecipeRules.MAX_MINUTES },
            tags = tags.map { it.trim().replace(Regex("\\s+"), " ").take(RecipeRules.MAX_TAG) }
                .filter { it.isNotEmpty() }
                .distinctBy(Text::fold)
                .take(RecipeRules.MAX_TAGS),
            sourceUrl = sourceUrl?.trim()?.takeIf(RecipeRules::isValidUrl),
            notes = notes.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                .joinToString("\n\n").take(RecipeRules.MAX_NOTES).ifEmpty { null },
            createdAt = timestamp ?: now,
            updatedAt = timestamp ?: now,
        )
        if (RecipeRules.problems(recipe).isNotEmpty()) {
            skip()
            return null
        }
        seen += finalId
        return recipe
    }
}
