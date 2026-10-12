package com.david7ce.recetario.logic

import com.david7ce.recetario.fakeJpeg
import com.david7ce.recetario.model.Ingredient
import com.david7ce.recetario.ui.Strings
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Inputs have the shape of real exports (RecipeSage JSON-LD, My RecipeBox .rtk). */
class ForeignFormatsTest {
    private val labels = Strings().noteLabels()
    private fun ids(): () -> String {
        var n = 0
        return { "new-${++n}" }
    }

    private val recipeSage = """
        {"recipes":[
          {"@context":"http://schema.org","@type":"Recipe","identifier":"0a94789f-43b8-4815-82a8-a3bc1201ca2c",
           "datePublished":"2025-05-13T07:30:54.009Z","description":"","image":["https://example.com/a"],
           "name":"Acelgas guisadas","prepTime":"","recipeIngredient":["Acelgas","3 dientes de ajo","Huevos"],
           "recipeInstructions":[{"@type":"HowToStep","text":"Picar las acelgas."},{"@type":"HowToSection","text":"[Salsa]"},
                                 {"@type":"HowToStep","text":"Freír el ajo."}],
           "recipeYield":"Para 4 Personas","totalTime":"PT1HM",
           "recipeCategory":["🍛 verduras","🏷️ casero","🍽️ principal"],"creditText":"x",
           "isBasedOn":"https://example.com/acelgas",
           "comment":[{"@type":"Comment","name":"Author Notes","text":"Mejor del día anterior.\nCon pan."}],
           "aggregateRating":{"@type":"AggregateRating","ratingValue":"4"}},
          {"@type":"Recipe","identifier":"x","name":"","recipeIngredient":[]},
          {"@type":"Recipe","identifier":"0a94789f-43b8-4815-82a8-a3bc1201ca2c","name":"Duplicada",
           "prepTime":"PT20M","totalTime":"PT30M","description":"Una <b>descripción</b>."}
        ]}
    """.trimIndent()

    @Test
    fun recognisesRecipeSageExports() {
        assertTrue(RecipeSageFormat.looksLike(Json.parseToJsonElement(recipeSage)))
        assertTrue(!RecipeSageFormat.looksLike(Json.parseToJsonElement("""{"format":"recetario","version":2,"recipes":[]}""")))
        assertTrue(!RecipeSageFormat.looksLike(Json.parseToJsonElement("""{"recipes":[{"name":"x"}]}""")))
    }

    @Test
    fun readsRecipeSageKeepingWhatHasNoField() {
        val result = RecipeSageFormat.parse(Json.parseToJsonElement(recipeSage), labels, ids(), now = 77)
        assertEquals(ForeignImport.Source.RecipeSage, result.source)
        assertEquals(1, result.skipped)
        val (acelgas, duplicada) = result.recipes

        assertEquals("0a94789f-43b8-4815-82a8-a3bc1201ca2c", acelgas.id)
        assertEquals("Acelgas guisadas", acelgas.title)
        assertEquals(listOf(Ingredient("Acelgas"), Ingredient("ajo", 3.0, "dientes"), Ingredient("Huevos")), acelgas.ingredients)
        assertEquals(listOf("Picar las acelgas.", "[Salsa]", "Freír el ajo."), acelgas.steps)
        assertEquals(4, acelgas.servings)
        assertNull(acelgas.prepMinutes)
        assertEquals(listOf("🍛 verduras", "🏷️ casero", "🍽️ principal"), acelgas.tags)
        assertEquals("https://example.com/acelgas", acelgas.sourceUrl)
        assertEquals("Mejor del día anterior.\nCon pan.\n\nTotal: 1 h", acelgas.notes)
        assertEquals(1_747_121_454_009, acelgas.createdAt)
        assertEquals(4, acelgas.rating)
        assertEquals(false, acelgas.favorite)
        assertNull(acelgas.imageFile)
        assertEquals("https://example.com/a", assertIs<PhotoSource.Url>(result.photos[acelgas.id]).url)

        assertEquals("new-1", duplicada.id, "a repeated id gets a new one instead of overwriting")
        assertEquals(20, duplicada.prepMinutes)
        assertEquals("Una descripción.\n\nTotal: 30 min", duplicada.notes)
        assertEquals(77, duplicada.updatedAt)
        assertNull(result.photos[duplicada.id])
    }

    private val rtkRecipes = """
        [{"title":"Gazpacho","description":"Fresquito.","preparationTime":"15","cookingTime":"","inactiveTime":"2 h",
          "totalTime":"2 h 15","quantity":"Para 6",
          "ingredients":"1kg Tomate pera\n1count Pepino\n2count Dientes de ajo\nSal",
          "instructions":"Lavar **bien** los tomates.\n\nTriturar todo con [la batidora](https://x.com/b).\n\nEnfriar.",
          "pictures":["/data/user/0/fr.recettetek/files/Pictures/abc-1.png"],"url":"https://example.com/gazpacho",
          "video":"","notes":"","cookware":"Batidora","nutrition":"calories : 120",
          "lastModifiedDate":"2026-03-02 12:53:26","uuid":"0adf2701-b0f0-4d42-b571-5489975e03fc",
          "originalPicture":"https://example.com/g.jpg","categories":[{"title":"Plato Principal"}],
          "tags":[{"title":"Verano"}],"rating":5.0,"favorite":true},
         {"title":"Sin foto local","ingredients":"","instructions":"Un solo paso\nOtro paso","pictures":[],
          "originalPicture":"https://example.com/o.jpg","uuid":"x y","lastModifiedDate":1764089501252},
         {"title":"  ","ingredients":"x"}]
    """.trimIndent()

    private val rtk = mapOf(
        "recipes_0.json" to rtkRecipes.encodeToByteArray(),
        "categories.json" to "[]".encodeToByteArray(),
        "abc-1.png" to fakeJpeg(5),
    )

    @Test
    fun readsMyRecipeBoxArchives() {
        assertTrue(RecipeBoxFormat.looksLike(rtk))
        assertTrue(!RecipeBoxFormat.looksLike(mapOf("manifest.json" to ByteArray(1))))

        val result = RecipeBoxFormat.parse(rtk, labels, ids(), now = 1)
        assertEquals(ForeignImport.Source.RecipeBox, result.source)
        assertEquals(1, result.skipped)
        val (gazpacho, other) = result.recipes

        assertEquals("0adf2701-b0f0-4d42-b571-5489975e03fc", gazpacho.id)
        assertEquals(
            listOf(
                Ingredient("Tomate pera", 1.0, "kg"),
                Ingredient("Pepino", 1.0),
                Ingredient("Dientes de ajo", 2.0),
                Ingredient("Sal"),
            ),
            gazpacho.ingredients,
        )
        assertEquals(listOf("Lavar bien los tomates.", "Triturar todo con la batidora.", "Enfriar."), gazpacho.steps)
        assertEquals(6, gazpacho.servings)
        assertEquals(15, gazpacho.prepMinutes)
        assertNull(gazpacho.cookMinutes)
        assertEquals(listOf("Plato Principal", "Verano"), gazpacho.tags)
        assertEquals(
            "Fresquito.\n\nTotal: 2 h 15 min\n\nResting time: 2 h\n\nNutrition: calories : 120\n\nCookware: Batidora",
            gazpacho.notes,
        )
        assertEquals(1_772_456_006_000, gazpacho.updatedAt)
        assertEquals(5, gazpacho.rating)
        assertEquals(true, gazpacho.favorite)
        assertContentEquals(fakeJpeg(5), assertIs<PhotoSource.Bytes>(result.photos[gazpacho.id]).bytes)

        assertEquals("new-1", other.id)
        assertTrue(other.ingredients.isEmpty())
        assertEquals(listOf("Un solo paso", "Otro paso"), other.steps)
        assertEquals(1_764_089_501_252, other.createdAt)
        assertEquals("https://example.com/o.jpg", assertIs<PhotoSource.Url>(result.photos[other.id]).url)
    }

    @Test
    fun brokenRecipeFilesAreReported() {
        val failure = assertFailsWith<ImportException> {
            RecipeBoxFormat.parse(mapOf("recipes_0.json" to "{not json".encodeToByteArray()), labels, ids(), 1)
        }.failure
        assertEquals(ImportFailure.Malformed("recipes_0.json"), failure)
    }
}
