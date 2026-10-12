package com.david7ce.recetario

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.david7ce.recetario.data.RecipeRepository
import com.david7ce.recetario.db.RecipeDatabase
import com.david7ce.recetario.logic.ConflictPolicy
import com.david7ce.recetario.logic.ImageChange
import com.david7ce.recetario.logic.ImportPlan
import com.david7ce.recetario.model.Ingredient
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Runs the repository against real SQLite (JDBC driver) with the shipped schema. */
class RecipeRepositoryTest {
    private val images = MemoryImageStore()
    private var ids = 0
    private val repo = newRepository(images) { "id-${++ids}" }

    @Test
    fun savesAndLoadsEverythingInOrder() {
        val r = recipe("a", tags = listOf("Cena", "Rápida")).copy(
            ingredients = listOf(Ingredient("b", 2.5, "kg", "pelado"), Ingredient("a"), Ingredient("c", 1.0)),
            steps = listOf("uno", "dos", "tres"),
            sourceUrl = "https://example.com/r",
            notes = "Mejor recién hecha.\n\nAguanta dos días.",
        )
        repo.save(r, ImageChange.Keep)
        assertEquals(listOf(r), repo.loadAll())
        assertEquals(r, repo.get("a"))
        assertNull(repo.get("missing"))
    }

    @Test
    fun imageFilesFollowTheRecipe() {
        val saved = repo.save(recipe("a"), ImageChange.Replace(fakeJpeg(1)))
        val first = assertNotNull(saved.imageFile)
        assertTrue(first in images.files)

        val replaced = repo.save(saved, ImageChange.Replace(fakeJpeg(2)))
        assertNotEquals(first, replaced.imageFile)
        assertEquals(listOf(replaced.imageFile), images.files.keys.toList())

        val kept = repo.save(replaced.copy(title = "Otra"), ImageChange.Keep)
        assertEquals(replaced.imageFile, kept.imageFile)

        val removed = repo.save(kept, ImageChange.Remove)
        assertNull(removed.imageFile)
        assertTrue(images.files.isEmpty())
    }

    @Test
    fun deleteRemovesRowsAndImage() {
        repo.save(recipe("a"), ImageChange.Replace(fakeJpeg(1)))
        repo.save(recipe("b"), ImageChange.Keep)
        repo.delete("a")
        assertEquals(listOf("b"), repo.loadAll().map { it.id })
        assertTrue(images.files.isEmpty())
    }

    @Test
    fun duplicateCopiesTheImageFile() {
        val original = repo.save(recipe("a"), ImageChange.Replace(fakeJpeg(7)))
        val copy = assertNotNull(repo.duplicate("a", "Tortilla (copia)"))
        assertEquals("id-1", copy.id)
        assertEquals("Tortilla (copia)", copy.title)
        assertNotEquals(original.imageFile, copy.imageFile)
        assertContentEquals(images.read(original.imageFile!!), images.read(copy.imageFile!!))

        repo.delete("a")
        assertNotNull(images.read(copy.imageFile!!), "deleting the original must not delete the copy's photo")
        assertEquals(original.ingredients, repo.get(copy.id)?.ingredients)
    }

    @Test
    fun importReplacesOnlyWhenAskedAndRenamesImages() {
        val mine = repo.save(recipe("a", title = "Mía"), ImageChange.Replace(fakeJpeg(1)))
        val incoming = listOf(recipe("a", title = "Suya", imageFile = "img1.jpg"), recipe("b", imageFile = "img2.jpg"))
        val archive = mapOf("img1.jpg" to fakeJpeg(8), "img2.jpg" to fakeJpeg(9))
        val existing = repo.loadAll().associateBy { it.id }
        val plan = ImportPlan.of(existing, incoming)

        repo.applyImport(plan.resolve(ConflictPolicy.KeepMine, existing, { "x" }, 5), archive)
        assertEquals("Mía", repo.get("a")?.title)
        assertEquals(mine.imageFile, repo.get("a")?.imageFile)
        val b = assertNotNull(repo.get("b"))
        assertContentEquals(fakeJpeg(9), images.read(b.imageFile!!))

        val again = repo.loadAll().associateBy { it.id }
        repo.applyImport(ImportPlan.of(again, incoming).resolve(ConflictPolicy.ReplaceMine, again, { "x" }, 6), archive)
        val a = assertNotNull(repo.get("a"))
        assertEquals("Suya", a.title)
        assertContentEquals(fakeJpeg(8), images.read(a.imageFile!!))
        assertNull(images.read(mine.imageFile!!), "the replaced recipe's photo is deleted")
        assertEquals(2, images.files.size)
    }

    @Test
    fun orphanImagesAreCleanedUp() {
        repo.save(recipe("a"), ImageChange.Replace(fakeJpeg(1)))
        images.write(fakeJpeg(2))
        assertEquals(1, repo.deleteOrphanImages())
        assertEquals(1, images.files.size)
    }

    @Test
    fun favouriteAndRatingAreQuickTogglesThatSurviveEdits() {
        repo.save(recipe("a"), ImageChange.Keep)
        repo.setFavorite("a", true)
        repo.setRating("a", 3)
        repo.setRating("b-missing", 5)
        val r = repo.get("a")!!
        assertEquals(true, r.favorite)
        assertEquals(3, r.rating)
        assertEquals(2L, r.updatedAt, "toggles are not edits")
        repo.setRating("a", 9) // out of range clears
        assertNull(repo.get("a")!!.rating)
    }

    @Test
    fun settings() {
        assertNull(repo.setting("theme"))
        repo.setSetting("theme", "Dark")
        repo.setSetting("theme", "Light")
        assertEquals("Light", repo.setting("theme"))
    }
}

fun newRepository(images: MemoryImageStore, newId: () -> String): RecipeRepository {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    RecipeDatabase.Schema.create(driver)
    return RecipeRepository(RecipeDatabase(driver), images, { 1000L }, newId)
}
