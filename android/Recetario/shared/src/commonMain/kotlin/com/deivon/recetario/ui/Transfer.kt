package com.david7ce.recetario.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.david7ce.recetario.logic.Backup
import com.david7ce.recetario.logic.ConflictPolicy
import com.david7ce.recetario.logic.ForeignImport
import com.david7ce.recetario.logic.ImageChange
import com.david7ce.recetario.logic.ImportException
import com.david7ce.recetario.logic.ImportFailure
import com.david7ce.recetario.logic.ImportPlan
import com.david7ce.recetario.logic.NoteLabels
import com.david7ce.recetario.logic.PhotoSource
import com.david7ce.recetario.logic.RecipeBoxFormat
import com.david7ce.recetario.logic.RecipeDraft
import com.david7ce.recetario.logic.RecipeJson
import com.david7ce.recetario.logic.RecipePage
import com.david7ce.recetario.logic.RecipeRules
import com.david7ce.recetario.logic.RecipeSageFormat
import com.david7ce.recetario.logic.Text
import com.david7ce.recetario.model.Recipe
import com.david7ce.recetario.platform.WebException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
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

    /**
     * A validated file waiting for the user's confirmation. [images] are already referenced by the
     * recipes (backups); [photos] are fetched and normalised on confirmation (other apps).
     */
    class Review(
        val incoming: List<Recipe>,
        val images: Map<String, ByteArray>,
        val plan: ImportPlan,
        val source: ForeignImport.Source? = null,
        val skipped: Int = 0,
        val photos: Map<String, PhotoSource> = emptyMap(),
    ) {
        /** Photos still to fetch: only for recipes that would be written, and only from the network. */
        val photosToDownload: Int
            get() = (plan.added + plan.conflicts).count { photos[it.id] is PhotoSource.Url }
    }

    /** Photo download progress (done, total) while an import is being confirmed; null otherwise. */
    var photoProgress by mutableStateOf<Pair<Int, Int>?>(null)
        private set
    private var importJob: Job? = null

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
            notes = page.notes,
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
                val labels = s.noteLabels()
                val now = services.clock()
                val read = withContext(Dispatchers.Default) { readFile(bytes, labels, now) }
                val plan = ImportPlan.of(app.recipes.associateBy { it.id }, read.incoming)
                if (plan.isEmpty) app.notify(s.nothingToImport)
                else review = Review(read.incoming, read.images, plan, read.source, read.skipped, read.photos)
            }
        }
    }

    private class Read(
        val incoming: List<Recipe>,
        val images: Map<String, ByteArray>,
        val source: ForeignImport.Source? = null,
        val skipped: Int = 0,
        val photos: Map<String, PhotoSource> = emptyMap(),
    )

    /** Recognises the file by content: Recetario backup or JSON, My RecipeBox .rtk, RecipeSage JSON(-LD). */
    private fun readFile(bytes: ByteArray, labels: NoteLabels, now: Long): Read {
        if (Backup.isZip(bytes)) {
            val entries = services.zip.unzip(bytes, Backup.MAX_UNPACKED_BYTES)
            return when {
                Backup.MANIFEST in entries -> Backup.parse(entries).let { Read(it.recipes, it.images) }
                RecipeBoxFormat.looksLike(entries) -> foreign(RecipeBoxFormat.parse(entries, labels, services.newId, now))
                else -> throw ImportException(ImportFailure.NotRecognized)
            }
        }
        val text = bytes.decodeToString()
        val root = try {
            Json.parseToJsonElement(text)
        } catch (e: SerializationException) {
            throw ImportException(ImportFailure.NotRecognized)
        } catch (e: IllegalArgumentException) {
            throw ImportException(ImportFailure.NotRecognized)
        }
        if (RecipeSageFormat.looksLike(root)) return foreign(RecipeSageFormat.parse(root, labels, services.newId, now))
        return Read(RecipeJson.decode(text), emptyMap())
    }

    private fun foreign(f: ForeignImport) = Read(f.recipes, emptyMap(), f.source, f.skipped, f.photos)

    fun cancelImport() {
        review = null
    }

    /** Stops a confirmed import while photos download; nothing has been written yet at that point. */
    fun cancelPhotoDownload() {
        importJob?.cancel()
    }

    fun confirmImport(policy: ConflictPolicy) {
        val pending = review ?: return
        review = null
        importJob = app.launch(showBusy = true) {
            try {
                val (incoming, images, failedPhotos) = withPhotos(pending)
                photoProgress = null // past this point the import can no longer be cancelled
                val result = withContext(NonCancellable + app.io) {
                    // Re-plan against the library as it is now, inside the serial writer.
                    val existing = app.repo.loadAll().associateBy { it.id }
                    val writes = ImportPlan.of(existing, incoming)
                        .resolve(policy, existing, services.newId, services.clock())
                    app.repo.applyImport(writes, images)
                }
                app.reload()
                val done = s.imported(result.inserted, result.replaced)
                app.notify(if (failedPhotos > 0) "$done. ${s.photosFailed(failedPhotos)}" else done)
            } finally {
                photoProgress = null
            }
        }
    }

    /**
     * Fetches (if remote) and normalises the photos of the recipes that will be written, up to 4 at a
     * time. A photo that fails is counted and its recipe imported without one. Returns the recipes with
     * their image names, the images by name, and the number of failures.
     */
    private suspend fun withPhotos(pending: Review): Triple<List<Recipe>, Map<String, ByteArray>, Int> {
        if (pending.photos.isEmpty()) return Triple(pending.incoming, pending.images, 0)
        val skipIds = pending.plan.identical.map { it.id }.toSet()
        val wanted = pending.incoming.filter { it.id !in skipIds && it.id in pending.photos }
        var done = 0
        var failed = 0
        photoProgress = 0 to wanted.size
        val gate = Semaphore(4)
        val normalised = coroutineScope {
            wanted.map { recipe ->
                async {
                    gate.withPermit {
                        val bytes = when (val source = pending.photos.getValue(recipe.id)) {
                            is PhotoSource.Bytes -> source.bytes
                            is PhotoSource.Url -> try {
                                services.web.getBytes(source.url, MAX_IMAGE_BYTES)
                            } catch (e: WebException) {
                                null // counted below; the recipe is still imported
                            }
                        }
                        val image = bytes?.let { withContext(Dispatchers.Default) { services.codec.normalize(it) } }
                        done++
                        if (image == null) failed++
                        photoProgress = done to wanted.size
                        recipe.id to image
                    }
                }
            }.awaitAll()
        }
        val images = mutableMapOf<String, ByteArray>()
        val names = mutableMapOf<String, String>()
        normalised.forEachIndexed { i, (id, image) ->
            if (image != null) {
                val name = "photo$i.jpg"
                images[name] = image
                names[id] = name
            }
        }
        return Triple(pending.incoming.map { r -> names[r.id]?.let { r.copy(imageFile = it) } ?: r }, images, failed)
    }

    // Export

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
