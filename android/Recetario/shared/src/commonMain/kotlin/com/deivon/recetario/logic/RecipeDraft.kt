package com.david7ce.recetario.logic

import com.david7ce.recetario.model.Ingredient
import com.david7ce.recetario.model.Recipe

/** Editor rows carry a [key] so lists keep their identity while rows are moved or removed. */
data class IngredientDraft(
    val key: Long,
    val quantity: String = "",
    val unit: String = "",
    val name: String = "",
    val note: String = "",
) {
    val isBlank: Boolean get() = quantity.isBlank() && unit.isBlank() && name.isBlank() && note.isBlank()
}

data class StepDraft(val key: Long, val text: String = "")

/** What happens to the recipe image when the draft is saved. */
sealed interface ImageChange {
    data object Keep : ImageChange
    data object Remove : ImageChange
    /** Already normalised image bytes; written to disk only when the recipe is saved. */
    class Replace(val bytes: ByteArray) : ImageChange
}

/**
 * The editor's form state: every field as typed. [toRecipe] turns it into a [Recipe] or explains
 * what is wrong. Blank ingredient rows and blank steps are dropped, never reported.
 */
data class RecipeDraft(
    val id: String? = null,
    val title: String = "",
    val existingImage: String? = null,
    val image: ImageChange = ImageChange.Keep,
    val servings: String = "",
    val prepMinutes: String = "",
    val cookMinutes: String = "",
    val tags: String = "",
    val sourceUrl: String = "",
    val notes: String = "",
    /** Not edited in the form (set from the recipe page) but kept through an edit. */
    val rating: Int? = null,
    val favorite: Boolean = false,
    val ingredients: List<IngredientDraft> = emptyList(),
    val steps: List<StepDraft> = emptyList(),
    val createdAt: Long? = null,
    val nextKey: Long = 1,
) {
    /** Image the form currently shows: null for none; [ImageChange.Replace] bytes are shown by the UI. */
    val shownImageFile: String? get() = if (image == ImageChange.Keep) existingImage else null

    fun withIngredient(): RecipeDraft = copy(ingredients = ingredients + IngredientDraft(nextKey), nextKey = nextKey + 1)
    fun withStep(): RecipeDraft = copy(steps = steps + StepDraft(nextKey), nextKey = nextKey + 1)

    sealed interface Result {
        data class Valid(val recipe: Recipe) : Result
        data class Invalid(val problems: List<Problem>) : Result
    }

    /** Validates the form. [id] and [now] are used only for new recipes. */
    fun toRecipe(newId: () -> String, now: Long): Result {
        val problems = mutableListOf<Problem>()
        fun int(text: String, range: IntRange, problem: Problem): Int? {
            if (text.isBlank()) return null
            val v = text.trim().toIntOrNull()
            if (v == null || v !in range) problems += problem
            return v?.takeIf { it in range }
        }
        val servingsValue = int(servings, 1..RecipeRules.MAX_SERVINGS, Problem.ServingsInvalid)
        val prep = int(prepMinutes, 0..RecipeRules.MAX_MINUTES, Problem.PrepTimeInvalid)
        val cook = int(cookMinutes, 0..RecipeRules.MAX_MINUTES, Problem.CookTimeInvalid)

        // Problems refer to rows by their position in the form (blank rows included), as the user sees them.
        val kept = ingredients.withIndex().filterNot { it.value.isBlank }
        val parsedIngredients = kept.map { (formIndex, row) ->
            val qty = if (row.quantity.isBlank()) null else Quantities.parse(row.quantity)
            if (row.quantity.isNotBlank() && qty == null) problems += Problem.QuantityInvalid(formIndex)
            Ingredient(
                name = row.name.trim(),
                quantity = qty,
                unit = row.unit.trim().ifEmpty { null },
                note = row.note.trim().ifEmpty { null },
            )
        }
        val url = sourceUrl.trim().ifEmpty { null }
        val recipe = Recipe(
            id = id ?: newId(),
            title = title.trim(),
            imageFile = existingImage,
            ingredients = parsedIngredients,
            steps = steps.map { it.text.trim() }.filter { it.isNotEmpty() },
            servings = servingsValue,
            prepMinutes = prep,
            cookMinutes = cook,
            tags = RecipeRules.parseTags(tags),
            sourceUrl = url,
            notes = notes.trim().ifEmpty { null },
            rating = rating,
            favorite = favorite,
            createdAt = createdAt ?: now,
            updatedAt = now,
        )
        // Structural checks (title, names, tag lengths, URL…) come from the shared rules; the
        // number fields were already checked above against what was typed.
        problems += RecipeRules.problems(recipe).mapNotNull { p ->
            when (p) {
                is Problem.QuantityInvalid, Problem.ImageNameInvalid, Problem.IdInvalid -> null
                is Problem.IngredientNameMissing -> Problem.IngredientNameMissing(kept[p.index].index)
                else -> p
            }
        }
        return if (problems.isEmpty()) Result.Valid(recipe) else Result.Invalid(problems.distinct())
    }

    companion object {
        fun blank(): RecipeDraft = RecipeDraft().withIngredient().withStep()

        fun from(r: Recipe, keepIdentity: Boolean = true): RecipeDraft {
            var key = 1L
            return RecipeDraft(
                id = if (keepIdentity) r.id else null,
                title = r.title,
                existingImage = if (keepIdentity) r.imageFile else null,
                servings = r.servings?.toString().orEmpty(),
                prepMinutes = r.prepMinutes?.toString().orEmpty(),
                cookMinutes = r.cookMinutes?.toString().orEmpty(),
                tags = r.tags.joinToString(", "),
                sourceUrl = r.sourceUrl.orEmpty(),
                notes = r.notes.orEmpty(),
                rating = if (keepIdentity) r.rating else null,
                favorite = keepIdentity && r.favorite,
                ingredients = r.ingredients.map {
                    IngredientDraft(
                        key = key++,
                        quantity = it.quantity?.let { q -> Quantities.format(q) }.orEmpty(),
                        unit = it.unit.orEmpty(),
                        name = it.name,
                        note = it.note.orEmpty(),
                    )
                },
                steps = r.steps.map { StepDraft(key++, it) },
                createdAt = if (keepIdentity) r.createdAt else null,
                nextKey = key,
            )
        }
    }
}

/** Moves the element at [from] one place up (-1) or down (+1); out-of-range moves return the list unchanged. */
fun <T> List<T>.moved(from: Int, by: Int): List<T> {
    val to = from + by
    if (from !in indices || to !in indices) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}
