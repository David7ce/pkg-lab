package com.david7ce.recetario.logic

import com.david7ce.recetario.model.Ingredient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecipePageTest {
    private val page = """
        <html><head>
        <script type="application/ld+json">{ this block is broken </script>
        <script type='application/ld+json'>
        {"@context":"https://schema.org","@graph":[
          {"@type":"WebPage","name":"Página"},
          {"@type":["Recipe","NewsArticle"],
           "name":"Tortilla &amp; patatas",
           "recipeYield":["4","4 raciones"],
           "prepTime":"PT15M","cookTime":"PT1H",
           "recipeCategory":"Plato principal","recipeCuisine":["Española"],
           "image":{"@type":"ImageObject","url":"https://example.com/t.jpg"},
           "recipeIngredient":["4 huevos","500 g de patatas","Sal al gusto"],
           "recipeInstructions":[
             {"@type":"HowToSection","name":"Preparación","itemListElement":[{"@type":"HowToStep","text":"Pelar las patatas."}]},
             {"@type":"HowToStep","text":"Freír y <b>cuajar</b>."}
           ]}
        ]}
        </script></head><body>…</body></html>
    """.trimIndent()

    @Test
    fun readsARecipeFromJsonLdGraph() {
        val r = assertNotNull(RecipePage.parse(page))
        assertEquals("Tortilla & patatas", r.title)
        assertEquals(4, r.servings)
        assertEquals(15, r.prepMinutes)
        assertEquals(60, r.cookMinutes)
        assertEquals(listOf("Plato principal", "Española"), r.tags)
        assertEquals("https://example.com/t.jpg", r.imageUrl)
        assertEquals(
            listOf(Ingredient("huevos", 4.0), Ingredient("patatas", 500.0, "g"), Ingredient("Sal al gusto")),
            r.ingredients,
        )
        assertEquals(listOf("Pelar las patatas.", "Freír y cuajar."), r.steps)
        assertTrue(r.warnings.isEmpty())
    }

    @Test
    fun splitsPlainInstructionTextAndReportsGaps() {
        val html = """<script type="application/ld+json">
            {"@type":"Recipe","name":"Pan","totalTime":"PT2H","recipeInstructions":"1. Mezclar.\n2. Hornear.<br/>3. Enfriar."}
            </script>"""
        val r = assertNotNull(RecipePage.parse(html))
        assertEquals(listOf("Mezclar.", "Hornear.", "Enfriar."), r.steps)
        assertNull(r.prepMinutes)
        assertEquals(120, r.totalMinutes)
        assertEquals(listOf(RecipePage.Warning.NoIngredients, RecipePage.Warning.OnlyTotalTime), r.warnings)
    }

    @Test
    fun pagesWithoutARecipeGiveNothing() {
        assertNull(RecipePage.parse("<html><body>No structured data</body></html>"))
        assertNull(RecipePage.parse("""<script type="application/ld+json">{"@type":"Article","name":"x"}</script>"""))
    }

    @Test
    fun isoDurations() {
        assertEquals(90, Durations.minutes("PT1H30M"))
        assertEquals(45, Durations.minutes("P0DT45M"))
        assertEquals(45, Durations.minutes("PT2700S"))
        assertEquals(20, Durations.minutes("pt20m"))
        assertEquals(60, Durations.minutes("PT1HM"), "empty designators, as RecipeSage writes them")
        assertNull(Durations.minutes("PTM"))
        assertNull(Durations.minutes("PT0M"))
        assertNull(Durations.minutes("P1Y"))
        assertNull(Durations.minutes("20 minutes"))
        assertNull(Durations.minutes(null))
    }

    @Test
    fun looseTimes() {
        assertEquals(30, Durations.loose("30"))
        assertEquals(160, Durations.loose("2 h 40"))
        assertEquals(60, Durations.loose("1h"))
        assertEquals(45, Durations.loose("45 min"))
        assertEquals(90, Durations.loose("1:30"))
        assertEquals(90, Durations.loose("PT1H30M"))
        assertNull(Durations.loose(""))
        assertNull(Durations.loose("0"))
        assertNull(Durations.loose("un rato"))
    }
}
