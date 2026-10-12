package com.deivon.recetario.logic

import com.deivon.recetario.model.Recipe

enum class SortOrder { Name, Modified }

/** What the library grid shows: free-text search, tag filters (all must match) and sort order. */
data class LibraryQuery(
    val text: String = "",
    val tags: Set<String> = emptySet(),
    val sort: SortOrder = SortOrder.Modified,
) {
    val isFiltering: Boolean get() = text.isNotBlank() || tags.isNotEmpty()

    fun apply(recipes: List<Recipe>): List<Recipe> {
        val words = Text.fold(text).split(' ').filter { it.isNotBlank() }
        val wanted = tags.map(Text::fold).toSet()
        val matching = recipes.filter { r ->
            val recipeTags = r.tags.map(Text::fold).toSet()
            recipeTags.containsAll(wanted) && (words.isEmpty() || searchText(r).let { hay -> words.all { hay.contains(it) } })
        }
        return when (sort) {
            SortOrder.Name -> matching.sortedWith(compareBy<Recipe> { Text.fold(it.title) }.thenBy { it.id })
            SortOrder.Modified -> matching.sortedWith(compareByDescending<Recipe> { it.updatedAt }.thenBy { it.id })
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
