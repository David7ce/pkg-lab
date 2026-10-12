package com.david7ce.recetario.logic

import com.david7ce.recetario.model.Ingredient
import com.david7ce.recetario.recipe
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryQueryTest {
    private val noquis = recipe("1", title = "Ñoquis caseros", updatedAt = 1, tags = listOf("Pasta", "Rápida"))
    private val gazpacho = recipe("2", title = "Gazpacho", updatedAt = 3, tags = listOf("Verano", "rápida"))
        .copy(ingredients = listOf(Ingredient("tomate", 1.0, "kg")))
    private val arroz = recipe("3", title = "arroz con leche", updatedAt = 2, tags = listOf("Postre"))
    private val all = listOf(noquis, gazpacho, arroz)

    private fun ids(q: LibraryQuery) = q.apply(all).map { it.id }

    @Test
    fun searchesTitleTagsAndIngredientsIgnoringAccents() {
        assertEquals(listOf("1"), ids(LibraryQuery(text = "noquis")))
        assertEquals(listOf("2"), ids(LibraryQuery(text = "TOMATE")))
        assertEquals(listOf("3"), ids(LibraryQuery(text = "postre leche")))
        assertEquals(emptyList(), ids(LibraryQuery(text = "pizza")))
    }

    @Test
    fun tagFiltersMustAllMatch() {
        assertEquals(listOf("2", "1"), ids(LibraryQuery(tags = setOf("RAPIDA"))))
        assertEquals(listOf("1"), ids(LibraryQuery(tags = setOf("rápida", "pasta"))))
    }

    @Test
    fun sortsByNameOrModification() {
        assertEquals(listOf("2", "3", "1"), ids(LibraryQuery(sort = SortOrder.Modified)))
        assertEquals(listOf("3", "2", "1"), ids(LibraryQuery(sort = SortOrder.Name)))
    }

    @Test
    fun favouritesMinimumRatingAndRatingOrder() {
        val rated = listOf(noquis.copy(rating = 5, favorite = true), gazpacho.copy(rating = 3), arroz)
        fun ids(q: LibraryQuery) = q.apply(rated).map { it.id }
        assertEquals(listOf("1"), ids(LibraryQuery(favoritesOnly = true)))
        assertEquals(listOf("2", "1"), ids(LibraryQuery(minRating = 3)))
        assertEquals(listOf("1"), ids(LibraryQuery(minRating = 4)))
        assertEquals(listOf("1", "2", "3"), ids(LibraryQuery(sort = SortOrder.Rating)))
        assertEquals(1, LibraryQuery(tags = setOf("x"), favoritesOnly = true).panelFilterCount)
        assertEquals(LibraryQuery(sort = SortOrder.Name), LibraryQuery(text = "a", minRating = 2, sort = SortOrder.Name).cleared())
    }

    @Test
    fun listsEachTagOnce() {
        assertEquals(listOf("Pasta", "Postre", "Rápida", "Verano"), LibraryQuery.allTags(all))
    }
}
