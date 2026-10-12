package com.deivon.vozreves

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
import java.io.File

enum class Mode { Idle, Recording, Playing, Paused }

/** Estado de la app; sobrevive a rotaciones y al cambio de idioma. */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val engine = AudioEngine()
    private val history = History(File(app.filesDir, "recordings"))
    val settings = Settings(app)
    private var job: Job? = null

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

    var reverse by mutableStateOf(true)
    /** Tramo a reproducir/compartir, como fracción 0..1 de la grabación. */
    var selection by mutableStateOf(0f..1f)

    val hasRecording: Boolean get() = duration > 0f
    val isPlaying: Boolean get() = mode == Mode.Playing || mode == Mode.Paused

    init { viewModelScope.launch { refreshHistory() } }

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

    fun togglePlay() {
        when (mode) {
            Mode.Playing -> { engine.pause(); mode = Mode.Paused }
            Mode.Paused -> { engine.resume(); mode = Mode.Playing }
            Mode.Recording -> Unit
            Mode.Idle -> launchExclusive {
                val (from, to) = range() ?: return@launchExclusive
                val total = engine.samples.size.toFloat()
                mode = Mode.Playing
                withTicker(33, { engine.position().takeIf { it >= 0 }?.let { progress = it / total } }) {
                    engine.play(reverse, from, to)
                }
            }
        }
    }

    fun stop() = engine.stopPlayback()

    /** Al salir de la app: Android no deja grabar en segundo plano, así que se para y se guarda. */
    fun onBackground() {
        engine.stopRecording()
        if (mode == Mode.Playing) { engine.pause(); mode = Mode.Paused }
    }

    /** WAV del tramo seleccionado en el sentido elegido, listo para compartir. */
    suspend fun exportSelection(): File? = withContext(Dispatchers.IO) {
        val (from, to) = range() ?: return@withContext null
        val name = if (reverse) "reversed.wav" else "voice.wav"
        File(getApplication<Application>().cacheDir, name).also { writeWav(it, engine.slice(from, to, reverse)) }
    }

    fun loadRecording(item: History.Item) = launchExclusive {
        engine.load(withContext(Dispatchers.IO) { history.load(item) })
        onNewAudio()
    }

    fun deleteRecording(item: History.Item) = viewModelScope.launch {
        withContext(Dispatchers.IO) { history.delete(item) }
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
    private fun launchExclusive(block: suspend () -> Unit) {
        engine.stopPlayback()
        val previous = job
        job = viewModelScope.launch {
            previous?.join()
            try { block() } finally { mode = Mode.Idle; progress = -1f }
        }
    }

    override fun onCleared() {
        engine.stopRecording()
        engine.stopPlayback()
    }
}
