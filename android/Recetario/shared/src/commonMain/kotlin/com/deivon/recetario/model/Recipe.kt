package com.david7ce.recetario.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One ingredient line. [quantity] is null when the recipe gives no number ("salt to taste"). */
@Serializable
data class Ingredient(
    val name: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val note: String? = null,
)

/**
 * A recipe as stored and exported. This class is also the JSON shape of the export format
 * (see [com.david7ce.recetario.logic.RecipeJson]); renaming a property is a format change.
 */
@Serializable
data class Recipe(
    val id: String,
    val title: String,
    /** File name inside the app's image folder (never a path); null = no image. */
    @SerialName("image") val imageFile: String? = null,
    val ingredients: List<Ingredient> = emptyList(),
    val steps: List<String> = emptyList(),
    val servings: Int? = null,
    val prepMinutes: Int? = null,
    val cookMinutes: Int? = null,
    val tags: List<String> = emptyList(),
    val sourceUrl: String? = null,
    /** Free text: description, tips, anything that has no field of its own. */
    val notes: String? = null,
    /** 1–5 stars; null = not rated. */
    val rating: Int? = null,
    val favorite: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
) {
    /** Prep + cook when at least one is known; null when neither is. */
    val totalMinutes: Int?
        get() = if (prepMinutes == null && cookMinutes == null) null else (prepMinutes ?: 0) + (cookMinutes ?: 0)

    /** Same content, ignoring identity-free details (timestamps and the local image file name). */
    fun sameContentAs(other: Recipe): Boolean =
        copy(imageFile = null, createdAt = 0, updatedAt = 0) ==
            other.copy(id = id, imageFile = null, createdAt = 0, updatedAt = 0)
}
