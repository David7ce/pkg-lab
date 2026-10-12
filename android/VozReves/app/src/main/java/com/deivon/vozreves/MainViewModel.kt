package com.deivon.vozreves

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class Mode { Idle, Countdown, Recording, Playing, Paused }

/** Estado de la app; sobrevive a rotaciones y al cambio de idioma. */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val engine = AudioEngine()
    private val history = History(File(app.filesDir, "recordings"))
    val settings = Settings(app)
    private var job: Job? = null

    var mode by mutableStateOf(Mode.Idle)
        private set
    var countdown by mutableIntStateOf(0)
        private set
    var duration by mutableFloatStateOf(0f)
        private set
    var peaks by mutableStateOf(FloatArray(0))
        private set
    /** Posición de reproducción 0..1, o -1 si no suena nada. */
    var progress by mutableFloatStateOf(-1f)
        private set
    var recordings by mutableStateOf(emptyList<History.Item>())
        private set

    var reverse by mutableStateOf(true)
    /** Tramo a reproducir/compartir, como fracción 0..1 de la grabación. */
    var selection by mutableStateOf(0f..1f)

    val hasRecording: Boolean get() = duration > 0f

    init { refreshHistory() }

    /** Botón grande: empieza (con cuenta atrás), para la grabación o cancela la cuenta atrás. */
    fun onRecordButton() {
        when (mode) {
            Mode.Recording -> engine.stopRecording()
            Mode.Countdown -> job?.cancel()
            else -> launchExclusive {
                for (i in settings.countdown downTo 1) {
                    mode = Mode.Countdown
                    countdown = i
                    delay(1000)
                }
                mode = Mode.Recording
                engine.record(settings.maxSeconds)
                onNewAudio()
                withContext(Dispatchers.IO) { history.save(engine.samples) }
                refreshHistory()
            }
        }
    }

    fun togglePlay() {
        when (mode) {
            Mode.Playing -> { engine.pause(); mode = Mode.Paused }
            Mode.Paused -> { engine.resume(); mode = Mode.Playing }
            else -> launchExclusive {
                val (from, to) = range() ?: return@launchExclusive
                val total = engine.samples.size.toFloat()
                mode = Mode.Playing
                coroutineScope {
                    val ticker = launch {
                        while (true) {
                            engine.position().takeIf { it >= 0 }?.let { progress = it / total }
                            delay(33)
                        }
                    }
                    try { engine.play(reverse, from, to) } finally { ticker.cancel() }
                }
            }
        }
    }

    fun stop() {
        engine.stopPlayback()
        if (mode == Mode.Countdown) job?.cancel()
    }

    /** WAV del tramo seleccionado en el sentido elegido, listo para compartir. */
    suspend fun exportSelection(): File? = withContext(Dispatchers.IO) {
        val (from, to) = range() ?: return@withContext null
        File(getApplication<Application>().cacheDir, "voz.wav").also { writeWav(it, engine.slice(from, to, reverse)) }
    }

    fun loadRecording(item: History.Item) = launchExclusive {
        engine.load(withContext(Dispatchers.IO) { history.load(item) })
        onNewAudio()
    }

    fun deleteRecording(item: History.Item) { history.delete(item); refreshHistory() }
    fun clearHistory() { history.clear(); refreshHistory() }

    private fun refreshHistory() { recordings = history.list() }

    private fun onNewAudio() {
        duration = engine.durationSeconds
        selection = 0f..1f
        peaks = peaks(engine.samples)
    }

    private fun range(): Pair<Int, Int>? {
        val n = engine.samples.size
        if (n == 0) return null
        val from = (selection.start * n).toInt().coerceIn(0, n - 1)
        val to = (selection.endInclusive * n).toInt().coerceIn(from + 1, n)
        return from to to
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
