package com.deivon.recetario

import com.deivon.recetario.logic.ForeignImport
import com.deivon.recetario.logic.PhotoSource
import com.deivon.recetario.logic.RecipeBoxFormat
import com.deivon.recetario.logic.RecipeRules
import com.deivon.recetario.logic.RecipeSageFormat
import com.deivon.recetario.ui.EsStrings
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The owner's real exports (from the recipesage-to-recipebox repository) must import completely. */
class RealExportsTest {
    private fun resource(name: String): ByteArray =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "missing test resource $name" }.use { it.readBytes() }

    private var n = 0
    private val newId = { "new-${++n}" }

    @Test
    fun recipeSageExport() {
        val root = Json.parseToJsonElement(resource("recipesage-sample.json").decodeToString())
        assertTrue(RecipeSageFormat.looksLike(root))
        val result = RecipeSageFormat.parse(root, EsStrings.noteLabels(), newId, now = 0)
        assertEquals(0, result.skipped)
        assertEquals(108, result.recipes.size)
        assertEquals(108, result.recipes.map { it.id }.toSet().size)
        assertEquals(0, n, "every RecipeSage identifier is kept")
        assertTrue(result.recipes.all { RecipeRules.problems(it).isEmpty() })
        assertTrue(result.recipes.all { it.steps.isNotEmpty() })
        assertEquals(108, result.photos.values.count { it is PhotoSource.Url })
        assertTrue(result.recipes.any { r -> r.tags.any { it.startsWith("🍽") } }, "emoji categories kept as tags")
    }

    @Test
    fun myRecipeBoxExport() {
        val entries = mapOf("recipes_0.json" to resource("myrecipebox-recipes_0.json"))
        val result = RecipeBoxFormat.parse(entries, EsStrings.noteLabels(), newId, now = 0)
        assertEquals(ForeignImport.Source.RecipeBox, result.source)
        assertEquals(0, result.skipped)
        assertEquals(7, result.recipes.size)
        assertTrue(result.recipes.all { RecipeRules.problems(it).isEmpty() })
        val lentejas = result.recipes.single { it.title.startsWith("Cómo hacer lentejas") }
        assertEquals(4, lentejas.servings)
        assertTrue(lentejas.ingredients.any { it.name == "Lentejas" && it.quantity == 250.0 && it.unit == "g" })
        assertTrue(lentejas.ingredients.any { it.name == "Diente de ajo" && it.quantity == 1.0 && it.unit == null })
        assertEquals(listOf("Plato Principal", "Legumbres"), lentejas.tags)
        // Photos are not in this map (only recipes_0.json was given), so the original URL is used.
        assertEquals(7, result.photos.values.count { it is PhotoSource.Url })
    }
}
