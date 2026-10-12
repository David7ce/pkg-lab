package com.david7ce.recetario.logic

import com.david7ce.recetario.model.Ingredient
import com.david7ce.recetario.model.Recipe

/** Things that can be wrong with a recipe, whether typed in the editor or read from a file. */
sealed interface Problem {
    data object TitleMissing : Problem
    data object TitleTooLong : Problem
    data object ServingsInvalid : Problem
    data object PrepTimeInvalid : Problem
    data object CookTimeInvalid : Problem
    data class IngredientNameMissing(val index: Int) : Problem
    data class QuantityInvalid(val index: Int) : Problem
    data class TagTooLong(val tag: String) : Problem
    data object TooManyTags : Problem
    data object SourceUrlInvalid : Problem
    data object IdInvalid : Problem
    data object ImageNameInvalid : Problem
    data object StepEmpty : Problem
    data object TooLarge : Problem
    data object RatingInvalid : Problem
}

/** Limits and checks shared by the editor, JSON import and backup restore. */
object RecipeRules {
    const val MAX_TITLE = 200
    const val MAX_SERVINGS = 999
    const val MAX_MINUTES = 10_000
    const val MAX_TAG = 40
    const val MAX_TAGS = 30
    const val MAX_ITEMS = 500       // ingredients or steps per recipe
    const val MAX_TEXT = 5_000      // one step, one ingredient field, the source URL
    const val MAX_NOTES = 20_000

    private val idPattern = Regex("[A-Za-z0-9_-]{1,64}")
    private val imagePattern = Regex("[A-Za-z0-9_-]{1,64}\\.(jpg|png|webp)")

    fun isValidId(id: String) = idPattern.matches(id)
    fun isValidImageName(name: String) = imagePattern.matches(name)
    fun isValidUrl(url: String) =
        url.length <= MAX_TEXT && (url.startsWith("https://") || url.startsWith("http://")) && url.length > 8 &&
            url.none { it.isWhitespace() }

    /** Problems in an already-structured recipe (from a file or the editor). Empty = valid. */
    fun problems(r: Recipe): List<Problem> = buildList {
        if (!isValidId(r.id)) add(Problem.IdInvalid)
        if (r.title.isBlank()) add(Problem.TitleMissing)
        if (r.title.length > MAX_TITLE) add(Problem.TitleTooLong)
        r.imageFile?.let { if (!isValidImageName(it)) add(Problem.ImageNameInvalid) }
        r.servings?.let { if (it !in 1..MAX_SERVINGS) add(Problem.ServingsInvalid) }
        r.prepMinutes?.let { if (it !in 0..MAX_MINUTES) add(Problem.PrepTimeInvalid) }
        r.cookMinutes?.let { if (it !in 0..MAX_MINUTES) add(Problem.CookTimeInvalid) }
        if (r.ingredients.size > MAX_ITEMS || r.steps.size > MAX_ITEMS) add(Problem.TooLarge)
        r.ingredients.forEachIndexed { i, ing -> addAll(problems(ing, i)) }
        if (r.steps.any { it.isBlank() }) add(Problem.StepEmpty)
        if (r.steps.any { it.length > MAX_TEXT }) add(Problem.TooLarge)
        addAll(tagProblems(r.tags))
        r.sourceUrl?.let { if (!isValidUrl(it)) add(Problem.SourceUrlInvalid) }
        if ((r.notes?.length ?: 0) > MAX_NOTES) add(Problem.TooLarge)
        r.rating?.let { if (it !in 1..5) add(Problem.RatingInvalid) }
    }

    private fun problems(ing: Ingredient, index: Int): List<Problem> = buildList {
        if (ing.name.isBlank()) add(Problem.IngredientNameMissing(index))
        ing.quantity?.let { if (!it.isFinite() || it <= 0 || it > 1e9) add(Problem.QuantityInvalid(index)) }
        if (listOfNotNull(ing.name, ing.unit, ing.note).any { it.length > MAX_TEXT }) add(Problem.TooLarge)
    }

    fun tagProblems(tags: List<String>): List<Problem> = buildList {
        if (tags.size > MAX_TAGS) add(Problem.TooManyTags)
        tags.firstOrNull { it.length > MAX_TAG }?.let { add(Problem.TagTooLong(it)) }
    }

    /** "Dessert, quick ,dessert" → ["Dessert", "quick"]: trimmed, blanks dropped, duplicates (case/accents) removed. */
    fun parseTags(text: String): List<String> =
        text.split(',').map { it.trim().replace(Regex("\\s+"), " ") }.filter { it.isNotEmpty() }.distinctBy(Text::fold)
}
