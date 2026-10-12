package com.deivon.recetario.data

import com.deivon.recetario.db.RecipeDatabase
import com.deivon.recetario.logic.ImageChange
import com.deivon.recetario.logic.ImportPlan
import com.deivon.recetario.model.Ingredient
import com.deivon.recetario.model.Recipe
import com.deivon.recetario.platform.ImageStore

/**
 * All reads and writes of recipes and their image files.
 *
 * Image files and rows can't share one transaction, so writes follow one order:
 * new image files are written first, then the rows change in a single transaction, and only
 * after it commits are replaced image files deleted. If the transaction fails, the new files are
 * deleted again. A crash between the steps can at worst leave an unreferenced file, which
 * [deleteOrphanImages] removes at the next start; it can never leave a row pointing at a missing file.
 *
 * Blocking: call from a background dispatcher.
 */
class RecipeRepository(
    private val db: RecipeDatabase,
    private val images: ImageStore,
    private val clock: () -> Long,
    private val newId: () -> String,
) {
    private val q get() = db.recipeQueries

    fun loadAll(): List<Recipe> {
        val ingredients = q.selectAllIngredients().executeAsList().groupBy({ it.recipe_id }) {
            Ingredient(it.name, it.quantity, it.unit, it.note)
        }
        val steps = q.selectAllSteps().executeAsList().groupBy({ it.recipe_id }) { it.body }
        val tags = q.selectAllTags().executeAsList().groupBy({ it.recipe_id }) { it.name }
        return q.selectAllRecipes().executeAsList().map { row ->
            Recipe(
                id = row.id,
                title = row.title,
                imageFile = row.image_file,
                ingredients = ingredients[row.id].orEmpty(),
                steps = steps[row.id].orEmpty(),
                servings = row.servings?.toInt(),
                prepMinutes = row.prep_minutes?.toInt(),
                cookMinutes = row.cook_minutes?.toInt(),
                tags = tags[row.id].orEmpty(),
                sourceUrl = row.source_url,
                createdAt = row.created_at,
                updatedAt = row.updated_at,
            )
        }
    }

    fun get(id: String): Recipe? {
        val row = q.selectRecipe(id).executeAsOneOrNull() ?: return null
        return Recipe(
            id = row.id,
            title = row.title,
            imageFile = row.image_file,
            ingredients = q.selectIngredients(id).executeAsList().map { Ingredient(it.name, it.quantity, it.unit, it.note) },
            steps = q.selectSteps(id).executeAsList().map { it.body },
            servings = row.servings?.toInt(),
            prepMinutes = row.prep_minutes?.toInt(),
            cookMinutes = row.cook_minutes?.toInt(),
            tags = q.selectTags(id).executeAsList().map { it.name },
            sourceUrl = row.source_url,
            createdAt = row.created_at,
            updatedAt = row.updated_at,
        )
    }

    /** Saves [recipe] (insert or update) with the image change from the editor. Returns what was stored. */
    fun save(recipe: Recipe, image: ImageChange): Recipe {
        val previousImage = q.selectRecipe(recipe.id).executeAsOneOrNull()?.image_file
        val newFile = (image as? ImageChange.Replace)?.let { images.write(it.bytes) }
        val stored = recipe.copy(
            imageFile = when (image) {
                ImageChange.Keep -> previousImage
                ImageChange.Remove -> null
                is ImageChange.Replace -> newFile
            },
        )
        writeOrUndo(listOfNotNull(newFile)) { db.transaction { writeRows(stored) } }
        if (previousImage != null && previousImage != stored.imageFile) images.delete(previousImage)
        return stored
    }

    fun delete(id: String) {
        val image = q.selectRecipe(id).executeAsOneOrNull()?.image_file
        db.transaction { deleteRows(id) }
        if (image != null) images.delete(image)
    }

    /** Copy with a new id, its own copy of the image file, and [title]. */
    fun duplicate(id: String, title: String): Recipe? {
        val original = get(id) ?: return null
        val now = clock()
        val copiedImage = original.imageFile?.let(images::read)?.let { images.write(it, extensionOf(original.imageFile)) }
        val copy = original.copy(id = newId(), title = title, imageFile = copiedImage, createdAt = now, updatedAt = now)
        writeOrUndo(listOfNotNull(copiedImage)) { db.transaction { writeRows(copy) } }
        return copy
    }

    class ImportResult(val inserted: Int, val replaced: Int)

    /**
     * Applies resolved import writes in one transaction. [archiveImages] maps the image names used in
     * the incoming recipes to their bytes (empty for plain JSON, whose recipes carry no image).
     * Imported images always get new local names, so they can never clash with existing files.
     */
    fun applyImport(writes: ImportPlan.Writes, archiveImages: Map<String, ByteArray>): ImportResult {
        val written = mutableListOf<String>()
        val replacedImages = mutableListOf<String>()
        try {
            val prepared = writes.all.map { r ->
                val bytes = r.imageFile?.let { archiveImages[it] }
                val local = bytes?.let { images.write(it, extensionOf(r.imageFile)).also { name -> written += name } }
                r.copy(imageFile = local)
            }
            db.transaction {
                for (r in prepared) {
                    q.selectRecipe(r.id).executeAsOneOrNull()?.image_file?.let { replacedImages += it }
                    deleteRows(r.id)
                    writeRows(r)
                }
            }
        } catch (e: Exception) {
            written.forEach { images.delete(it) }
            throw e
        }
        replacedImages.forEach { images.delete(it) }
        return ImportResult(writes.insert.size, writes.replace.size)
    }

    /** Removes image files no recipe refers to (left by a crash mid-write). Returns how many. */
    fun deleteOrphanImages(): Int {
        val referenced = q.selectImageFiles().executeAsList().mapNotNull { it }.toSet()
        return images.list().filter { it !in referenced }.count { images.delete(it) }
    }

    fun setting(name: String): String? = q.selectSetting(name).executeAsOneOrNull()
    fun setSetting(name: String, value: String) = q.upsertSetting(name, value)

    private fun writeRows(r: Recipe) {
        q.upsertRecipe(
            r.id, r.title, r.imageFile, r.servings?.toLong(), r.prepMinutes?.toLong(), r.cookMinutes?.toLong(),
            r.sourceUrl, r.createdAt, r.updatedAt,
        )
        q.deleteIngredients(r.id)
        q.deleteSteps(r.id)
        q.deleteTags(r.id)
        r.ingredients.forEachIndexed { i, ing -> q.insertIngredient(r.id, i.toLong(), ing.name, ing.quantity, ing.unit, ing.note) }
        r.steps.forEachIndexed { i, s -> q.insertStep(r.id, i.toLong(), s) }
        r.tags.forEachIndexed { i, t -> q.insertTag(r.id, i.toLong(), t) }
    }

    private fun deleteRows(id: String) {
        q.deleteIngredients(id)
        q.deleteSteps(id)
        q.deleteTags(id)
        q.deleteRecipe(id)
    }

    private inline fun writeOrUndo(newFiles: List<String>, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            newFiles.forEach { images.delete(it) }
            throw e
        }
    }

    private fun extensionOf(name: String?): String = name?.substringAfterLast('.', "jpg") ?: "jpg"
}
