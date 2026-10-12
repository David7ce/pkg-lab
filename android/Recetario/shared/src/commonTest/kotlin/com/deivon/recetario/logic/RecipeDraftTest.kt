package com.david7ce.recetario.logic

import com.david7ce.recetario.model.Ingredient
import com.david7ce.recetario.recipe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RecipeDraftTest {
    private fun RecipeDraft.check() = toRecipe({ "new-id" }, now = 50)

    @Test
    fun blankDraftNeedsATitle() {
        val result = assertIs<RecipeDraft.Result.Invalid>(RecipeDraft.blank().check())
        assertEquals(listOf<Problem>(Problem.TitleMissing), result.problems)
    }

    @Test
    fun validDraftBecomesARecipeDroppingBlankRows() {
        val draft = RecipeDraft.blank().copy(title = "  Gazpacho ", servings = "4", tags = "Verano, frío ,verano")
            .withIngredient()
            .let { d ->
                d.copy(
                    ingredients = listOf(
                        IngredientDraft(1, quantity = "1 1/2", unit = "kg", name = "tomates"),
                        IngredientDraft(2),
                        IngredientDraft(3, name = "sal", note = "al gusto"),
                    ),
                    steps = listOf(StepDraft(4, "Triturar."), StepDraft(5, "  ")),
                )
            }
        val r = assertIs<RecipeDraft.Result.Valid>(draft.check()).recipe
        assertEquals("new-id", r.id)
        assertEquals("Gazpacho", r.title)
        assertEquals(4, r.servings)
        assertEquals(listOf("Verano", "frío"), r.tags)
        assertEquals(listOf(Ingredient("tomates", 1.5, "kg"), Ingredient("sal", note = "al gusto")), r.ingredients)
        assertEquals(listOf("Triturar."), r.steps)
        assertEquals(50, r.createdAt)
        assertEquals(50, r.updatedAt)
    }

    @Test
    fun reportsEachBadFieldWithItsFormPosition() {
        val draft = RecipeDraft(
            title = "X",
            servings = "0",
            prepMinutes = "99999",
            sourceUrl = "ftp://x",
            ingredients = listOf(
                IngredientDraft(1),
                IngredientDraft(2, quantity = "mucho", name = "azúcar"),
                IngredientDraft(3, quantity = "2"),
            ),
        )
        val problems = assertIs<RecipeDraft.Result.Invalid>(draft.check()).problems
        assertTrue(Problem.ServingsInvalid in problems)
        assertTrue(Problem.PrepTimeInvalid in problems)
        assertTrue(Problem.SourceUrlInvalid in problems)
        assertTrue(Problem.QuantityInvalid(1) in problems)
        assertTrue(Problem.IngredientNameMissing(2) in problems)
    }

    @Test
    fun editingKeepsIdentityAndContent() {
        val original = recipe()
        val back = assertIs<RecipeDraft.Result.Valid>(RecipeDraft.from(original).check()).recipe
        assertEquals(original.id, back.id)
        assertEquals(original.createdAt, back.createdAt)
        assertTrue(original.sameContentAs(back))
    }

    @Test
    fun movingRows() {
        assertEquals(listOf("b", "a", "c"), listOf("a", "b", "c").moved(1, -1))
        assertEquals(listOf("a", "b", "c"), listOf("a", "b", "c").moved(0, -1))
    }
}
