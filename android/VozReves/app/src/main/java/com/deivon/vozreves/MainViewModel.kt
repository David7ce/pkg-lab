package com.deivon.vozreves

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

enum class Mode { Idle, Recording, PlayingNormal, PlayingReverse }

/** Guarda el estado y la grabación aunque se gire la pantalla. */
class MainViewModel : ViewModel() {

    private val engine = AudioEngine()
    private var job: Job? = null

    var mode by mutableStateOf(Mode.Idle)
        private set
    var duration by mutableFloatStateOf(0f)
        private set

    val hasRecording: Boolean get() = duration > 0f

    fun toggleRecord() {
        if (mode == Mode.Recording) {
            engine.stopRecording()
            return
        }
        run(Mode.Recording) {
            engine.record()
            duration = engine.durationSeconds
        }
    }

    fun play(reverse: Boolean) {
        run(if (reverse) Mode.PlayingReverse else Mode.PlayingNormal) { engine.play(reverse) }
    }

    fun stop() {
        engine.stopRecording()
        engine.stopPlayback()
    }

    suspend fun exportReversed(dir: File): File =
        engine.exportWav(File(dir, "voz_al_reves.wav"), reverse = true)

    /** Para lo que esté sonando o grabando y lanza la nueva acción. */
    private fun run(newMode: Mode, block: suspend () -> Unit) {
        stop()
        val previous = job
        job = viewModelScope.launch {
            previous?.join()
            mode = newMode
            try { block() } finally { mode = Mode.Idle }
        }
    }

    override fun onCleared() = stop()
}
