package com.deivon.recetario.logic

import com.deivon.recetario.model.Ingredient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class QuantitiesTest {
    @Test
    fun parsesTheFormsPeopleWrite() {
        assertEquals(2.0, Quantities.parse("2"))
        assertEquals(1.5, Quantities.parse("1.5"))
        assertEquals(1.5, Quantities.parse("1,5"))
        assertEquals(0.5, Quantities.parse("1/2"))
        assertEquals(1.5, Quantities.parse("1 1/2"))
        assertEquals(5.5, Quantities.parse("11/2"))
        assertEquals(0.5, Quantities.parse("½"))
        assertEquals(1.5, Quantities.parse("1½"))
        assertEquals(1.5, Quantities.parse(" 1 ½ "))
    }

    @Test
    fun rejectsWhatIsNotOneQuantity() {
        assertNull(Quantities.parse(""))
        assertNull(Quantities.parse("abc"))
        assertNull(Quantities.parse("1/0"))
        assertNull(Quantities.parse("0"))
        assertNull(Quantities.parse("2-3"))
        assertNull(Quantities.parse("1 2"))
    }

    @Test
    fun formatsForCooks() {
        assertEquals("2", Quantities.format(2.0))
        assertEquals("1½", Quantities.format(1.5))
        assertEquals("½", Quantities.format(0.5))
        assertEquals("⅓", Quantities.format(1.0 / 3))
        assertEquals("2¾", Quantities.format(2.75))
        assertEquals("12.5", Quantities.format(12.5))
        assertEquals("12,5", Quantities.format(12.5, ','))
        assertEquals("1.23", Quantities.format(1.234))
        assertEquals("0.1", Quantities.format(0.1))
        assertEquals("1000", Quantities.format(1000.0))
    }

    @Test
    fun scalesOnlyNumericQuantities() {
        val ingredients = listOf(
            Ingredient("harina", 200.0, "g"),
            Ingredient("sal", note = "al gusto"),
            Ingredient("huevos", 3.0),
        )
        val scaled = Quantities.scale(ingredients, fromServings = 4, toServings = 2)
        assertEquals(100.0, scaled[0].quantity)
        assertEquals("g", scaled[0].unit)
        assertEquals(ingredients[1], scaled[1])
        assertEquals(1.5, scaled[2].quantity)
        assertEquals(6.0, Quantities.scale(ingredients, 2, 4)[2].quantity)
    }

    @Test
    fun scalesByAnyFactor() {
        val ingredients = listOf(Ingredient("harina", 200.0, "g"), Ingredient("sal", note = "al gusto"))
        val scaled = Quantities.scale(ingredients, 1.5)
        assertEquals(300.0, scaled[0].quantity)
        assertEquals(ingredients[1], scaled[1])
        assertSame(ingredients, Quantities.scale(ingredients, 1.0))
        assertSame(ingredients, Quantities.scale(ingredients, 0.0))
    }

    @Test
    fun scalingToTheSameOrInvalidServingsChangesNothing() {
        val ingredients = listOf(Ingredient("harina", 200.0, "g"))
        assertSame(ingredients, Quantities.scale(ingredients, 4, 4))
        assertSame(ingredients, Quantities.scale(ingredients, 0, 4))
    }
}
