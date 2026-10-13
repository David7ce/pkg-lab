package com.david7ce.recetario.ui

import com.david7ce.shared.ui.ThemeMode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import com.david7ce.recetario.data.RecipeRepository
import com.david7ce.recetario.logic.ImageChange
import com.david7ce.recetario.logic.ImportException
import com.david7ce.recetario.logic.LibraryQuery
import com.david7ce.recetario.logic.MAX_RELEASE_JSON_BYTES
import com.david7ce.recetario.logic.Problem
import com.david7ce.recetario.logic.RecipeDraft
import com.david7ce.recetario.logic.UpdateInfo
import com.david7ce.recetario.logic.parseUpdateCheck
import com.david7ce.recetario.model.Recipe
import com.david7ce.recetario.platform.Services
import com.david7ce.recetario.platform.WebException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the platform does for the shared UI: pickers, saving files, opening links. */
interface PlatformUi {
    /** Null result = the user cancelled. */
    fun pickImage(onResult: (Result<ByteArray>?) -> Unit)
    fun openFile(onResult: (Result<ByteArray>?) -> Unit)
    fun saveFile(fileName: String, mimeType: String, bytes: ByteArray, onResult: (Boolean) -> Unit)
    fun openUrl(url: String)
}

val LocalPlatform = staticCompositionLocalOf<PlatformUi> { error("PlatformUi not provided") }

sealed interface Screen {
    data object Library : Screen
    data object Settings : Screen

    class Detail(val id: String) : Screen {
        /** Multiplier applied to every numeric quantity ("Adjust quantities"); 1 = as written. */
        var factor by mutableDoubleStateOf(1.0)
        var showAdjust by mutableStateOf(false)
        var confirmDelete by mutableStateOf(false)
    }

    class Editor(val state: EditorState) : Screen

    class Cooking(val id: String, val factor: Double) : Screen {
        var step by mutableIntStateOf(0)
        var showIngredients by mutableStateOf(false)
    }
}

/** The editor's state. Kept outside composition so it survives rotation. */
class EditorState(
    initial: RecipeDraft,
    /** Set when the draft comes from a web page: shown as a banner, and makes it "changed" from the start. */
    val importedFrom: String? = null,
    val importNotes: List<String> = emptyList(),
) {
    private val original = initial
    var draft by mutableStateOf(initial)
        private set
    var problems by mutableStateOf<List<Problem>>(emptyList())
    var confirmDiscard by mutableStateOf(false)
    var saving by mutableStateOf(false)

    val isNew: Boolean get() = original.id == null
    val hasChanges: Boolean get() = importedFrom != null || draft != original

    fun update(change: (RecipeDraft) -> RecipeDraft) {
        draft = change(draft)
        // After a failed save, keep the error list in step with what is typed.
        if (problems.isNotEmpty()) {
            problems = (draft.toRecipe({ "draft" }, 0) as? RecipeDraft.Result.Invalid)?.problems.orEmpty()
        }
    }
}

/**
 * App state and actions, shared by every platform. Lives as long as the process (not the screen),
 * so rotation keeps navigation and unsaved edits. Database and file work runs on [io], one task at a
 * time, so writes never interleave.
 */
class AppState(
    internal val services: Services,
    internal val repo: RecipeRepository,
    private val scope: CoroutineScope,
    systemLanguage: String,
) {
    /** The device language (ISO 639 code), updated by the platform; used when [language] is System. */
    var systemLanguage by mutableStateOf(systemLanguage)
    var language by mutableStateOf(AppLanguage.System)
        private set
    val strings: Strings get() = stringsFor(language.code ?: systemLanguage)

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val io: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1)

    var recipes by mutableStateOf<List<Recipe>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set
    var query by mutableStateOf(LibraryQuery())
    var themeMode by mutableStateOf(ThemeMode.System)
        private set
    var busy by mutableStateOf(false)
        private set
    var updateInfo by mutableStateOf<UpdateInfo?>(null)
        private set

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> get() = _messages

    private val stack = mutableStateListOf<Screen>(Screen.Library)
    val screen: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    val transfer = Transfer(this)

    init {
        launch {
            try {
                val (list, settings) = withContext(io) {
                    repo.deleteOrphanImages()
                    repo.loadAll() to (repo.setting(THEME) to repo.setting(LANGUAGE))
                }
                recipes = list
                themeMode = ThemeMode.entries.firstOrNull { it.name == settings.first } ?: ThemeMode.System
                language = AppLanguage.entries.firstOrNull { it.name == settings.second } ?: AppLanguage.System
            } finally {
                loaded = true
            }
        }
    }

    fun recipe(id: String): Recipe? = recipes.firstOrNull { it.id == id }

    // Navigation

    fun open(id: String) { stack.add(Screen.Detail(id)) }
    fun openSettings() { stack.add(Screen.Settings) }
    fun newRecipe() { stack.add(Screen.Editor(EditorState(RecipeDraft.blank()))) }
    fun edit(id: String) { recipe(id)?.let { stack.add(Screen.Editor(EditorState(RecipeDraft.from(it)))) } }
    fun newFromUrl() { transfer.startUrlImport() }
    fun startCooking(id: String, factor: Double) { stack.add(Screen.Cooking(id, factor)) }
    internal fun review(state: EditorState) { stack.add(Screen.Editor(state)) }

    /** System back / toolbar back. An editor with changes asks before leaving. */
    fun back() {
        val s = screen
        if (s is Screen.Editor && s.state.hasChanges && !s.state.saving) {
            s.state.confirmDiscard = true
            return
        }
        pop()
    }

    fun discard(editor: EditorState) {
        editor.confirmDiscard = false
        if ((screen as? Screen.Editor)?.state === editor) pop()
    }

    private fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    // Recipes

    fun save(editor: EditorState) {
        if (editor.saving) return
        when (val result = editor.draft.toRecipe(services.newId, services.clock())) {
            is RecipeDraft.Result.Invalid -> {
                editor.problems = result.problems
                notify(strings.fixErrors)
            }
            is RecipeDraft.Result.Valid -> {
                editor.saving = true
                launch {
                    try {
                        val saved = withContext(io) { repo.save(result.recipe, editor.draft.image) }
                        reload()
                        val at = stack.indexOfLast { it is Screen.Editor && it.state === editor }
                        if (at >= 0) {
                            stack.removeAt(at)
                            val below = stack.getOrNull(at - 1)
                            if (!(below is Screen.Detail && below.id == saved.id)) stack.add(at, Screen.Detail(saved.id))
                        }
                        notify(strings.saved)
                    } finally {
                        editor.saving = false
                    }
                }
            }
        }
    }

    fun delete(id: String) {
        launch {
            withContext(io) { repo.delete(id) }
            reload()
            stack.removeAll { (it is Screen.Detail && it.id == id) || (it is Screen.Cooking && it.id == id) }
            if (stack.isEmpty()) stack.add(Screen.Library)
            notify(strings.deleted)
        }
    }

    fun duplicate(id: String) {
        launch {
            val original = recipe(id) ?: return@launch
            val copy = withContext(io) { repo.duplicate(id, strings.copyTitle(original.title)) } ?: return@launch
            reload()
            stack.add(Screen.Detail(copy.id))
            notify(strings.duplicated)
        }
    }

    fun setTheme(mode: ThemeMode) {
        themeMode = mode
        launch { withContext(io) { repo.setSetting(THEME, mode.name) } }
    }

    fun chooseLanguage(value: AppLanguage) {
        language = value
        launch { withContext(io) { repo.setSetting(LANGUAGE, value.name) } }
    }

    /**
     * Checks GitHub Releases (no token: public repo, public releases) for a build newer than
     * [currentVersion] under [releaseTag] ("android-Recetario-latest" or "desktop-recetario-latest").
     * Silent on failure (offline, rate limit): this is a background check, not a user action.
     */
    fun checkForUpdates(releaseTag: String, currentVersion: String) {
        launch {
            val url = "https://api.github.com/repos/David7ce/pkg-lab/releases/tags/$releaseTag"
            val json = try {
                services.web.getText(url, MAX_RELEASE_JSON_BYTES)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return@launch
            }
            updateInfo = parseUpdateCheck(json, currentVersion)
        }
    }

    fun dismissUpdate() { updateInfo = null }

    /** Quick toggles from the recipe page: the list updates at once, the database right after. */
    fun toggleFavorite(id: String) {
        val r = recipe(id) ?: return
        replaceLocal(r.copy(favorite = !r.favorite))
        launch { withContext(io) { repo.setFavorite(id, !r.favorite) } }
    }

    /** Tapping the current rating again clears it. */
    fun rate(id: String, stars: Int) {
        val r = recipe(id) ?: return
        val value = if (r.rating == stars) null else stars.coerceIn(1, 5)
        replaceLocal(r.copy(rating = value))
        launch { withContext(io) { repo.setRating(id, value) } }
    }

    private fun replaceLocal(updated: Recipe) {
        recipes = recipes.map { if (it.id == updated.id) updated else it }
    }

    fun pickImage(editor: EditorState, platform: PlatformUi) {
        platform.pickImage { result ->
            if (result != null) launch {
                val bytes = result.getOrNull()
                if (bytes == null) {
                    notify(strings.fileNotRead)
                    return@launch
                }
                val normalized = withContext(Dispatchers.Default) { services.codec.normalize(bytes) }
                if (normalized == null) notify(strings.imageUnreadable)
                else editor.update { it.copy(image = ImageChange.Replace(normalized)) }
            }
        }
    }

    // Images: decoded off the main thread, small in-memory cache keyed by file name and size.

    private val imageCache = LinkedHashMap<String, ImageBitmap>()

    suspend fun image(name: String, maxSize: Int): ImageBitmap? {
        val key = "$name@$maxSize"
        imageCache[key]?.let { return it }
        val bitmap = withContext(Dispatchers.Default) {
            services.images.read(name)?.let { services.codec.decode(it, maxSize) }
        } ?: return null
        imageCache[key] = bitmap
        if (imageCache.size > 60) imageCache.remove(imageCache.keys.first())
        return bitmap
    }

    suspend fun decode(bytes: ByteArray, maxSize: Int): ImageBitmap? =
        withContext(Dispatchers.Default) { services.codec.decode(bytes, maxSize) }

    // Plumbing

    internal suspend fun reload() {
        recipes = withContext(io) { repo.loadAll() }
    }

    internal fun notify(message: String) {
        _messages.tryEmit(message)
    }

    /** Runs [block] on the main scope; failures become a message instead of a crash. */
    internal fun launch(showBusy: Boolean = false, block: suspend () -> Unit): Job =
        scope.launch {
            if (showBusy) busy = true
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ImportException) {
                notify(strings.failure(e.failure))
            } catch (e: WebException) {
                notify(strings.web(e))
            } catch (e: Exception) {
                notify(strings.unexpected(e.message))
            } finally {
                if (showBusy) busy = false
            }
        }

    private companion object {
        const val THEME = "theme"
        const val LANGUAGE = "language"
    }
}
