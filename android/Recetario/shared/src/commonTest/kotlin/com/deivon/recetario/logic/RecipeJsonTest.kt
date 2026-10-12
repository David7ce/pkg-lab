package com.deivon.recetario.logic

import com.deivon.recetario.recipe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecipeJsonTest {
    private fun failure(text: String) = assertFailsWith<ImportException> { RecipeJson.decode(text) }.failure

    @Test
    fun roundTripsWithoutImageReferences() {
        val original = listOf(recipe("a", imageFile = "img1.jpg"), recipe("b", title = "Gazpacho", tags = listOf("verano")))
        val decoded = RecipeJson.decode(RecipeJson.encode(original, exportedAt = 9))
        assertEquals(original.map { it.copy(imageFile = null) }, decoded)
    }

    @Test
    fun backupsKeepImageNames() {
        val decoded = RecipeJson.decode(RecipeJson.encode(listOf(recipe(imageFile = "img1.jpg")), 9, keepImageNames = true))
        assertEquals("img1.jpg", decoded.single().imageFile)
    }

    @Test
    fun ignoresUnknownFieldsOfTheSameVersion() {
        val text = """{"format":"recetario","version":1,"exportedAt":1,"future":true,
            "recipes":[{"id":"a","title":"T","createdAt":1,"updatedAt":2,"rating":5}]}"""
        val r = RecipeJson.decode(text).single()
        assertEquals("T", r.title)
        assertNull(r.servings)
        assertTrue(r.ingredients.isEmpty())
    }

    @Test
    fun rejectsOtherFilesAndNewerVersions() {
        assertEquals(ImportFailure.NotRecognized, failure("not json at all"))
        assertEquals(ImportFailure.NotRecognized, failure("""{"format":"other","version":1}"""))
        assertEquals(ImportFailure.UnsupportedVersion(4), failure("""{"format":"recetario","version":4,"exportedAt":1,"recipes":[]}"""))
        assertEquals(ImportFailure.Empty, failure("""{"format":"recetario","version":1,"exportedAt":1,"recipes":[]}"""))
        assertIs<ImportFailure.Malformed>(failure("""{"format":"recetario","version":1,"exportedAt":1,"recipes":[{"id":"a"}]}"""))
    }

    @Test
    fun rejectsDuplicateIdsAndInvalidRecipes() {
        val dup = RecipeJson.encode(listOf(recipe("a"), recipe("a", title = "Otra")), 1)
        assertEquals(ImportFailure.DuplicateIds(listOf("a")), failure(dup))

        val blankTitle = RecipeJson.encode(listOf(recipe("a").copy(title = " ")), 1)
        assertIs<ImportFailure.InvalidRecipe>(failure(blankTitle))

        val badImage = RecipeJson.encode(listOf(recipe("a", imageFile = "../../etc/passwd")), 1, keepImageNames = true)
        val invalid = assertIs<ImportFailure.InvalidRecipe>(failure(badImage))
        assertTrue(Problem.ImageNameInvalid in invalid.problems)
    }
}
