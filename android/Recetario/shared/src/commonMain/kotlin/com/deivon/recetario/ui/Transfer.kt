package com.deivon.recetario.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.deivon.recetario.logic.Backup
import com.deivon.recetario.logic.ConflictPolicy
import com.deivon.recetario.logic.ImageChange
import com.deivon.recetario.logic.ImportException
import com.deivon.recetario.logic.ImportFailure
import com.deivon.recetario.logic.ImportPlan
import com.deivon.recetario.logic.RecipeDraft
import com.deivon.recetario.logic.RecipeJson
import com.deivon.recetario.logic.RecipePage
import com.deivon.recetario.logic.RecipeRules
import com.deivon.recetario.logic.Text
import com.deivon.recetario.model.Recipe
import com.deivon.recetario.platform.WebException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

/** Import (web page, JSON, backup) and export (JSON, backup, PDF) flows. */
class Transfer(private val app: AppState) {
    private val s get() = app.strings
    private val services get() = app.services

    class UrlImport {
        var url by mutableStateOf("")
        var loading by mutableStateOf(false)
        var error by mutableStateOf<String?>(null)
        internal var job: Job? = null
    }

    /** A validated file waiting for the user's confirmation. */
    class Review(val incoming: List<Recipe>, val images: Map<String, ByteArray>, val plan: ImportPlan)

    var urlImport by mutableStateOf<UrlImport?>(null)
        private set
    var review by mutableStateOf<Review?>(null)
        private set

    val canExportPdf: Boolean get() = services.pdf != null

    // Web page

    fun startUrlImport() {
        urlImport = UrlImport()
    }

    fun cancelUrlImport() {
        urlImport?.job?.cancel()
        urlImport = null
    }

    fun importUrl(dialog: UrlImport) {
        val url = normalizeUrl(dialog.url)
        if (url == null) {
            dialog.error = s.web(WebException(WebException.Kind.InvalidUrl))
            return
        }
        dialog.loading = true
        dialog.error = null
        dialog.job = app.launch {
            try {
                val html = services.web.getText(url, MAX_PAGE_BYTES)
                val page = withContext(Dispatchers.Default) { RecipePage.parse(html) }
                if (page == null) {
                    dialog.error = s.noRecipeOnPage
                } else {
                    val image = page.imageUrl?.let { downloadImage(it) }
                    urlImport = null
                    app.review(
                        EditorState(
                            initial = draftFrom(page, url, image),
                            importedFrom = url.substringAfter("://").substringBefore('/'),
                            importNotes = page.warnings.map { s.warning(it, page.totalMinutes) },
                        ),
                    )
                }
            } catch (e: WebException) {
                dialog.error = s.web(e)
            } finally {
                dialog.loading = false
            }
        }
    }

    /** The photo is optional: if it can't be fetched or read, the recipe is imported without one. */
    private suspend fun downloadImage(url: String): ByteArray? = try {
        val bytes = services.web.getBytes(url, MAX_IMAGE_BYTES)
        withContext(Dispatchers.Default) { services.codec.normalize(bytes) }
    } catch (e: WebException) {
        null
    }

    private fun draftFrom(page: RecipePage.Imported, url: String, image: ByteArray?): RecipeDraft {
        val asRecipe = Recipe(
            id = "import",
            title = page.title,
            ingredients = page.ingredients,
            steps = page.steps,
            servings = page.servings,
            prepMinutes = page.prepMinutes,
            cookMinutes = page.cookMinutes,
            tags = page.tags,
            sourceUrl = url,
            createdAt = 0,
            updatedAt = 0,
        )
        val draft = RecipeDraft.from(asRecipe, keepIdentity = false)
        return if (image != null) draft.copy(image = ImageChange.Replace(image)) else draft
    }

    // Files

    fun importFile(platform: PlatformUi) {
        platform.openFile { result ->
            if (result != null) app.launch(showBusy = true) {
                val bytes = result.getOrNull()
                if (bytes == null) {
                    app.notify(s.fileNotRead)
                    return@launch
                }
                if (bytes.size > Backup.MAX_UNPACKED_BYTES) throw ImportException(ImportFailure.TooLarge)
                val content = withContext(Dispatchers.Default) {
                    if (Backup.isZip(bytes)) Backup.parse(services.zip.unzip(bytes, Backup.MAX_UNPACKED_BYTES))
                    else Backup.Content(RecipeJson.decode(bytes.decodeToString()), emptyMap())
                }
                val plan = ImportPlan.of(app.recipes.associateBy { it.id }, content.recipes)
                if (plan.isEmpty) app.notify(s.nothingToImport)
                else review = Review(content.recipes, content.images, plan)
            }
        }
    }

    fun cancelImport() {
        review = null
    }

    fun confirmImport(policy: ConflictPolicy) {
        val pending = review ?: return
        review = null
        app.launch(showBusy = true) {
            val result = withContext(app.io) {
                // Re-plan against the library as it is now, inside the serial writer.
                val existing = app.repo.loadAll().associateBy { it.id }
                val writes = ImportPlan.of(existing, pending.incoming)
                    .resolve(policy, existing, services.newId, services.clock())
                app.repo.applyImport(writes, pending.images)
            }
            app.reload()
            app.notify(s.imported(result.inserted, result.replaced))
        }
    }

    // Export

    fun exportRecipe(recipe: Recipe, platform: PlatformUi) =
        saveFile(platform, "${Text.slug(recipe.title)}.json", JSON) {
            RecipeJson.encode(listOf(recipe), services.clock()).encodeToByteArray() to null
        }

    fun exportRecipePdf(recipe: Recipe, platform: PlatformUi) {
        val pdf = services.pdf ?: return
        saveFile(platform, "${Text.slug(recipe.title)}.pdf", PDF) {
            pdf.export(listOf(recipe), services.images::read, s, cookbookTitle = null) to null
        }
    }

    fun exportLibrary(platform: PlatformUi) {
        if (app.recipes.isEmpty()) return app.notify(s.noRecipesToExport)
        saveFile(platform, "recetario-${Text.isoDate(services.clock())}.json", JSON) {
            RecipeJson.encode(app.repo.loadAll(), services.clock()).encodeToByteArray() to null
        }
    }

    fun backup(platform: PlatformUi) {
        if (app.recipes.isEmpty()) return app.notify(s.noRecipesToExport)
        saveFile(platform, "recetario-backup-${Text.isoDate(services.clock())}.zip", ZIP) {
            val built = Backup.build(app.repo.loadAll(), services.images::read, services.clock())
            services.zip.zip(built.entries) to built.missingImages.takeIf { it > 0 }?.let { s.backupMissingImages(it) }
        }
    }

    fun cookbook(platform: PlatformUi) {
        val pdf = services.pdf ?: return
        if (app.recipes.isEmpty()) return app.notify(s.noRecipesToExport)
        saveFile(platform, "recetario-${Text.isoDate(services.clock())}.pdf", PDF) {
            val all = app.query.copy(text = "", tags = emptySet()).apply(app.repo.loadAll())
            pdf.export(all, services.images::read, s, cookbookTitle = s.appName) to null
        }
    }

    /** Builds the bytes on the serial worker, then asks the platform where to save. [produce] may add a note to the success message. */
    private fun saveFile(platform: PlatformUi, name: String, mime: String, produce: () -> Pair<ByteArray, String?>) {
        app.launch(showBusy = true) {
            val (bytes, note) = withContext(app.io) { produce() }
            platform.saveFile(name, mime, bytes) { ok -> app.notify(if (!ok) s.fileNotSaved else note ?: s.fileSaved) }
        }
    }

    companion object {
        const val JSON = "application/json"
        const val ZIP = "application/zip"
        const val PDF = "application/pdf"
        const val MAX_PAGE_BYTES = 5 * 1024 * 1024
        const val MAX_IMAGE_BYTES = 15 * 1024 * 1024

        /** Adds https:// when no scheme is typed; null when the result is not a usable web address. */
        fun normalizeUrl(input: String): String? {
            val trimmed = input.trim()
            val url = if ("://" in trimmed) trimmed else "https://$trimmed"
            return url.takeIf { RecipeRules.isValidUrl(it) && '.' in it.substringAfter("://").substringBefore('/') }
        }
    }
}
