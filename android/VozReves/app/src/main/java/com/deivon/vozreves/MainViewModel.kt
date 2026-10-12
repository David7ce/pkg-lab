package com.david7ce.vozreves

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.util.Date

enum class Mode { Idle, Recording, Preparing, Playing, Paused }

/** Estado de la app; sobrevive a rotaciones y al cambio de idioma. */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val engine = AudioEngine()
    private val history = History(File(app.filesDir, "recordings"))
    private val phrasesStore = Phrases(File(app.filesDir, "phrases.txt")).also {
        it.seedIfNew(com.david7ce.vozreves.Settings.wrap(app).resources.getStringArray(R.array.default_phrases).toList())
    }
    val settings = com.david7ce.vozreves.Settings(app)
    private var job: Job? = null
    private var jobIsPlayback = false

    var mode by mutableStateOf(Mode.Idle)
        private set
    /** Segundos grabados mientras se graba. */
    var recorded by mutableFloatStateOf(0f)
        private set
    var duration by mutableFloatStateOf(0f)
        private set
    var peaks by mutableStateOf(FloatArray(0))
        private set
    /** Posición de reproducción 0..1 en la grabación completa, o -1 si no suena nada. */
    var progress by mutableFloatStateOf(-1f)
        private set
    var recordings by mutableStateOf(emptyList<History.Item>())
        private set
    /** El micrófono estaba ocupado o no se pudo abrir. */
    var micError by mutableStateOf(false)
        private set
    /** No se pudo generar la voz de una frase (sin motor de texto a voz o sin ese idioma). */
    var ttsError by mutableStateOf(false)
        private set
    var phrases by mutableStateOf(emptyList<String>())
        private set

    var reverse by mutableStateOf(true)
    /** Tramo a reproducir/compartir, como fracción 0..1 de la grabación. */
    var selection by mutableStateOf(0f..1f)

    val hasRecording: Boolean get() = duration > 0f
    val isPlaying: Boolean get() = mode == Mode.Playing || mode == Mode.Paused

    init {
        viewModelScope.launch {
            refreshHistory()
            phrases = withContext(Dispatchers.IO) { phrasesStore.list() }
        }
    }

    /** Botón grande: empieza o para la grabación (al parar se guarda en el historial). */
    fun onRecordButton() {
        if (mode == Mode.Recording) { engine.stopRecording(); return }
        launchExclusive {
            micError = false
            recorded = 0f
            mode = Mode.Recording
            val ok = withTicker(100, { recorded = engine.recordedSamples / SAMPLE_RATE.toFloat() }) {
                engine.record(settings.maxSeconds)
            }
            if (!ok) { micError = true; return@launchExclusive }
            onNewAudio()
            withContext(Dispatchers.IO) { history.save(engine.samples) }
            refreshHistory()
        }
    }

    /**
     * Play/pausa. El estado cambia al instante en el hilo principal (antes de lanzar nada), así que
     * pulsaciones rápidas solo alternan pausa/reanudar y nunca arrancan dos reproducciones.
     */
    fun togglePlay() {
        when (mode) {
            Mode.Playing -> { mode = Mode.Paused; engine.pause() }
            Mode.Paused -> { mode = Mode.Playing; engine.resume() }
            Mode.Recording, Mode.Preparing -> Unit
            Mode.Idle -> {
                val (from, to) = range() ?: return
                mode = Mode.Playing
                launchExclusive(playback = true) { playRange(from, to) }
            }
        }
    }

    private suspend fun playRange(from: Int, to: Int) {
        val total = engine.samples.size.toFloat()
        mode = Mode.Playing
        withTicker(33, { engine.position().takeIf { it >= 0 }?.let { progress = it / total } }) {
            engine.play(reverse, from, to)
        }
    }

    /** Genera la voz de la frase, la deja como audio actual y la reproduce en el sentido elegido. */
    fun playPhrase(text: String) {
        if (mode == Mode.Recording) return
        ttsError = false
        mode = Mode.Preparing
        launchExclusive(playback = true) {
            mode = Mode.Preparing
            val audio = synthesizePhrase(getApplication(), text, java.util.Locale.getDefault())
            if (audio == null) { ttsError = true; return@launchExclusive }
            engine.load(audio)
            onNewAudio()
            val (from, to) = range() ?: return@launchExclusive
            playRange(from, to)
        }
    }

    fun addPhrase(text: String) = viewModelScope.launch {
        withContext(Dispatchers.IO) { phrasesStore.add(text) }
        phrases = withContext(Dispatchers.IO) { phrasesStore.list() }
    }

    fun removePhrases(texts: Collection<String>) = viewModelScope.launch {
        withContext(Dispatchers.IO) { phrasesStore.removeAll(texts) }
        phrases = withContext(Dispatchers.IO) { phrasesStore.list() }
    }

    /** Cancela también una reproducción que aún no ha empezado (pulsación muy rápida tras Play). */
    fun stop() { job?.cancel(); engine.stopPlayback() }

    /** Al salir de la app: Android no deja grabar en segundo plano, así que se para y se guarda. */
    fun onBackground() {
        engine.stopRecording()
        if (mode == Mode.Playing) { mode = Mode.Paused; engine.pause() }
    }

    /**
     * Guarda el tramo seleccionado en Música/VozReves (Android 10+), visible en apps de música y
     * archivos sin pedir permisos. Devuelve false si falla.
     */
    suspend fun saveToMusic(): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext false
        val (from, to) = range() ?: return@withContext false
        val resolver = getApplication<Application>().contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, exportName())
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/VozReves")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: return@withContext false
        runCatching {
            resolver.openOutputStream(uri)!!.use { it.write(wavBytes(engine.slice(from, to, reverse))) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
        }.onFailure { resolver.delete(uri, null, null) }.isSuccess
    }

    /** Android 8–9: el usuario elige dónde guardar con el selector del sistema. */
    suspend fun writeSelection(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val (from, to) = range() ?: return@withContext false
        runCatching {
            getApplication<Application>().contentResolver.openOutputStream(uri, "wt")!!
                .use { it.write(wavBytes(engine.slice(from, to, reverse))) }
        }.isSuccess
    }

    fun exportName(): String = voiceFileName(Date(), reverse)

    fun renameRecording(item: History.Item, title: String) = viewModelScope.launch {
        withContext(Dispatchers.IO) { history.rename(item, title) }
        refreshHistory()
    }

    /** WAV del tramo seleccionado en el sentido elegido, listo para compartir. */
    suspend fun exportSelection(): File? = withContext(Dispatchers.IO) {
        val (from, to) = range() ?: return@withContext null
        // Solo para compartir: la caché es temporal a propósito; el guardado permanente es "Guardar en Música".
        val cache = getApplication<Application>().cacheDir
        cache.listFiles { f -> "_voice_" in f.name && f.extension == "wav" }?.forEach { it.delete() }
        File(cache, exportName()).also { writeWav(it, engine.slice(from, to, reverse)) }
    }

    fun loadRecording(item: History.Item) = launchExclusive {
        engine.load(withContext(Dispatchers.IO) { history.load(item) })
        onNewAudio()
    }

    fun deleteRecording(item: History.Item) = viewModelScope.launch {
        withContext(Dispatchers.IO) { history.delete(item) }
        refreshHistory()
    }

    fun deleteRecordings(items: List<History.Item>) = viewModelScope.launch {
        withContext(Dispatchers.IO) { items.forEach(history::delete) }
        refreshHistory()
    }

    fun clearHistory() = viewModelScope.launch {
        withContext(Dispatchers.IO) { history.clear() }
        refreshHistory()
    }

    private suspend fun refreshHistory() {
        recordings = withContext(Dispatchers.IO) { history.list() }
    }

    private suspend fun onNewAudio() {
        duration = engine.durationSeconds
        selection = 0f..1f
        peaks = withContext(Dispatchers.Default) { peaks(engine.samples) } // Fuera del hilo de la UI.
    }

    private fun range(): Pair<Int, Int>? {
        val n = engine.samples.size
        if (n == 0) return null
        val from = (selection.start * n).toInt().coerceIn(0, n - 1)
        val to = (selection.endInclusive * n).toInt().coerceIn(from + 1, n)
        return from to to
    }

    /** Ejecuta [block] actualizando el estado cada [periodMs] mientras dura. */
    private suspend fun <T> withTicker(periodMs: Long, tick: () -> Unit, block: suspend CoroutineScope.() -> T): T =
        coroutineScope {
            val ticker = launch { while (true) { tick(); delay(periodMs) } }
            try { block() } finally { ticker.cancel() }
        }

    /** Para lo que esté sonando y lanza la nueva acción cuando la anterior haya terminado. */
    private fun launchExclusive(playback: Boolean = false, block: suspend () -> Unit) {
        engine.stopPlayback()
        val previous = job
        // Una reproducción pendiente se cancela (no se encadena); una grabación en curso se espera.
        if (jobIsPlayback) previous?.cancel()
        jobIsPlayback = playback
        job = viewModelScope.launch {
            try {
                previous?.join()
                block()
            } finally { mode = Mode.Idle; progress = -1f }
        }
    }

    override fun onCleared() {
        engine.stopRecording()
        engine.stopPlayback()
    }
}
