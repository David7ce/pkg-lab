package com.david7ce.recetario.logic

import com.david7ce.recetario.model.Recipe

enum class SortOrder { Modified, Name, Rating }

/**
 * What the library grid shows: free-text search, filters (tags — all must match —, favourites only,
 * minimum stars) and sort order.
 */
data class LibraryQuery(
    val text: String = "",
    val tags: Set<String> = emptySet(),
    val favoritesOnly: Boolean = false,
    /** 0 = any; otherwise only recipes rated at least this many stars. */
    val minRating: Int = 0,
    val sort: SortOrder = SortOrder.Modified,
) {
    /** Filters in the filter panel (search and the favourites button are shown on their own). */
    val panelFilterCount: Int get() = tags.size + (if (minRating > 0) 1 else 0)
    val isFiltering: Boolean get() = text.isNotBlank() || tags.isNotEmpty() || favoritesOnly || minRating > 0

    fun cleared(): LibraryQuery = LibraryQuery(sort = sort)

    fun apply(recipes: List<Recipe>): List<Recipe> {
        val words = Text.fold(text).split(' ').filter { it.isNotBlank() }
        val wanted = tags.map(Text::fold).toSet()
        val matching = recipes.filter { r ->
            val recipeTags = r.tags.map(Text::fold).toSet()
            (!favoritesOnly || r.favorite) && (r.rating ?: 0) >= minRating &&
                recipeTags.containsAll(wanted) && (words.isEmpty() || searchText(r).let { hay -> words.all { hay.contains(it) } })
        }
        return when (sort) {
            SortOrder.Name -> matching.sortedWith(compareBy<Recipe> { Text.fold(it.title) }.thenBy { it.id })
            SortOrder.Modified -> matching.sortedWith(compareByDescending<Recipe> { it.updatedAt }.thenBy { it.id })
            SortOrder.Rating -> matching.sortedWith(
                compareByDescending<Recipe> { it.rating ?: 0 }.thenBy { Text.fold(it.title) }.thenBy { it.id },
            )
        }
    }

    companion object {
        /** Title, tags and ingredient names: what a cook would search by. */
        private fun searchText(r: Recipe): String =
            Text.fold(buildString {
                append(r.title)
                r.tags.forEach { append(' ').append(it) }
                r.ingredients.forEach { append(' ').append(it.name) }
            })

        /** Every tag in the library once (case/accents ignored), alphabetically. */
        fun allTags(recipes: List<Recipe>): List<String> =
            recipes.flatMap { it.tags }
                .distinctBy(Text::fold)
                .sortedBy(Text::fold)
    }
}
