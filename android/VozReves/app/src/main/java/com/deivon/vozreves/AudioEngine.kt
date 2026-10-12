package com.deivon.vozreves

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

const val SAMPLE_RATE = 44_100

/** Graba PCM 16 bits mono y lo reproduce (o un tramo) normal o al revés. Sin dependencias. */
class AudioEngine {

    var samples = ShortArray(0)
        private set

    val durationSeconds: Float get() = samples.size / SAMPLE_RATE.toFloat()

    @Volatile private var recording = false
    @Volatile private var playing = false
    @Volatile private var track: AudioTrack? = null
    @Volatile private var startPos = 0
    @Volatile private var direction = 1

    fun load(data: ShortArray) { samples = data }

    /** Muestras grabadas hasta ahora (para el contador mientras se graba). */
    @Volatile var recordedSamples = 0
        private set

    /**
     * Graba hasta que se llame a [stopRecording] o se llegue a [maxSeconds].
     * Devuelve false si el micrófono no está disponible (p. ej. lo usa otra app).
     */
    @SuppressLint("MissingPermission") // El permiso se pide en la UI antes de llamar aquí.
    suspend fun record(maxSeconds: Int): Boolean = withContext(Dispatchers.IO) {
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return@withContext false
        }
        val max = SAMPLE_RATE * maxSeconds
        // Empieza con 10 s y crece al doble según haga falta: no reserva 10 MB por si acaso.
        var out = ShortArray(minOf(max, SAMPLE_RATE * 10))
        val chunk = ShortArray(minBuf)
        var total = 0

        recordedSamples = 0
        recording = true
        recorder.startRecording()
        try {
            while (recording && isActive && total < max) {
                val n = recorder.read(chunk, 0, minOf(chunk.size, max - total))
                if (n < 0) break
                if (total + n > out.size) out = out.copyOf(minOf(max, out.size * 2))
                chunk.copyInto(out, total, 0, n)
                total += n
                recordedSamples = total
            }
        } finally {
            recorder.stop()
            recorder.release()
            recording = false
        }
        samples = out.copyOf(total)
        true
    }

    fun stopRecording() { recording = false }

    /** Tramo [from, to) de la grabación, invertido si hace falta. */
    fun slice(from: Int, to: Int, reverse: Boolean): ShortArray =
        samples.copyOfRange(from, to).also { if (reverse) it.reverse() }

    /** Muestra que está sonando ahora (en la grabación original), o -1 si no suena nada. */
    fun position(): Int {
        val t = track ?: return -1
        return runCatching { startPos + direction * t.playbackHeadPosition }.getOrDefault(-1)
    }

    suspend fun play(reverse: Boolean, from: Int, to: Int) = withContext(Dispatchers.IO) {
        val data = slice(from, to, reverse)
        if (data.isEmpty()) return@withContext

        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        startPos = if (reverse) to else from
        direction = if (reverse) -1 else 1
        playing = true
        track = t
        t.play()
        try {
            var offset = 0
            while (playing && isActive && offset < data.size) {
                val n = t.write(data, offset, minOf(4096, data.size - offset))
                if (n <= 0) break
                offset += n
            }
            // Esperar a que suene lo que queda en el buffer (también mientras está en pausa).
            while (playing && isActive && t.playbackHeadPosition < data.size) delay(20)
        } finally {
            track = null
            t.pause()
            t.flush()
            t.release()
            playing = false
        }
    }

    fun pause() { runCatching { track?.pause() } }
    fun resume() { runCatching { track?.play() } }

    fun stopPlayback() {
        playing = false
        // Pausar y vaciar el buffer desbloquea un write() que esté esperando.
        runCatching { track?.run { pause(); flush() } }
    }
}

/** WAV PCM 16 bits mono. */
fun writeWav(file: File, data: ShortArray) {
    val dataBytes = data.size * 2
    val buf = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
    buf.put("RIFF".toByteArray()).putInt(36 + dataBytes).put("WAVE".toByteArray())
    buf.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
        .putInt(SAMPLE_RATE).putInt(SAMPLE_RATE * 2).putShort(2).putShort(16)
    buf.put("data".toByteArray()).putInt(dataBytes)
    buf.asShortBuffer().put(data) // Copia en bloque: mucho más rápido que muestra a muestra.
    file.writeBytes(buf.array())
}

/** Lee los WAV que escribe [writeWav] (cabecera de 44 bytes). */
fun readWav(file: File): ShortArray {
    val bytes = file.readBytes()
    if (bytes.size <= 44) return ShortArray(0)
    val out = ShortArray((bytes.size - 44) / 2)
    ByteBuffer.wrap(bytes, 44, out.size * 2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(out)
    return out
}
