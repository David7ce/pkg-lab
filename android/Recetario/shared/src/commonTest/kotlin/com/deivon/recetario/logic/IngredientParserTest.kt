package com.deivon.recetario.logic

import com.deivon.recetario.model.Ingredient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IngredientParserTest {
    private fun p(line: String) = IngredientParser.parse(line)

    @Test
    fun splitsQuantityUnitNameAndNote() {
        assertEquals(Ingredient("flour", 200.0, "g"), p("200 g flour"))
        assertEquals(Ingredient("sugar", 1.5, "cups", "sifted"), p("1 1/2 cups of sugar, sifted"))
        assertEquals(Ingredient("sal", 0.5, "cucharadita"), p("½ cucharadita de sal"))
        assertEquals(Ingredient("butter", 100.0, "g"), p("100g butter"))
        assertEquals(Ingredient("olive oil", 4.0, "tbsp"), p("4 tbsp. olive oil"))
        assertEquals(Ingredient("eggs", 2.0), p("2 eggs"))
    }

    @Test
    fun wordsThatStartLikeUnitsAreNotUnits() {
        assertEquals(Ingredient("garlic clove", 1.0), p("1 garlic clove"))
        assertEquals(Ingredient("large egg", 1.0), p("1 large egg"))
    }

    @Test
    fun keepsTheWholeLineWhenThereIsNoPlainQuantity() {
        assertEquals(Ingredient("Salt to taste"), p("Salt to taste"))
        assertEquals(Ingredient("2-3 cloves garlic"), p("2-3 cloves garlic"))
        assertEquals(Ingredient("2 to 3 apples"), p("2 to 3 apples"))
        assertEquals(Ingredient("2"), p("2"))
    }

    @Test
    fun cleansHtmlAndIgnoresBlankLines() {
        assertEquals(Ingredient("tomatoes & basil", 3.0), p("3 <b>tomatoes</b> &amp; basil"))
        assertNull(p("   "))
    }
}
