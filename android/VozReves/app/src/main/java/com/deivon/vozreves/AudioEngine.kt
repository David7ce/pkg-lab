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

/** Graba PCM 16 bits mono, lo reproduce normal o al revés y lo exporta a WAV. Sin dependencias. */
class AudioEngine {

    companion object {
        const val SAMPLE_RATE = 44_100
        const val MAX_SECONDS = 60
    }

    var samples = ShortArray(0)
        private set

    val durationSeconds: Float get() = samples.size / SAMPLE_RATE.toFloat()

    @Volatile private var recording = false
    @Volatile private var playing = false

    /** Graba hasta que se llame a [stopRecording] o se llegue a [MAX_SECONDS]. */
    @SuppressLint("MissingPermission") // El permiso se pide en la UI antes de llamar aquí.
    suspend fun record() = withContext(Dispatchers.IO) {
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2
        )
        val out = ShortArray(SAMPLE_RATE * MAX_SECONDS)
        val chunk = ShortArray(minBuf)
        var total = 0

        recording = true
        recorder.startRecording()
        try {
            while (recording && isActive && total < out.size) {
                val n = recorder.read(chunk, 0, minOf(chunk.size, out.size - total))
                if (n < 0) break
                chunk.copyInto(out, total, 0, n)
                total += n
            }
        } finally {
            recorder.stop()
            recorder.release()
            recording = false
        }
        samples = out.copyOf(total)
    }

    fun stopRecording() { recording = false }

    /** Reproduce la grabación; invertir es simplemente dar la vuelta al array de muestras. */
    suspend fun play(reverse: Boolean) = withContext(Dispatchers.IO) {
        val data = if (reverse) samples.reversedArray() else samples
        if (data.isEmpty()) return@withContext

        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
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

        playing = true
        track.play()
        try {
            var offset = 0
            while (playing && isActive && offset < data.size) {
                val n = track.write(data, offset, minOf(4096, data.size - offset))
                if (n <= 0) break
                offset += n
            }
            // Esperar a que suene lo que queda en el buffer antes de soltar el AudioTrack.
            while (playing && isActive && track.playbackHeadPosition < data.size) delay(20)
        } finally {
            track.pause()
            track.flush()
            track.release()
            playing = false
        }
    }

    fun stopPlayback() { playing = false }

    /** Escribe la grabación (normal o invertida) como WAV PCM 16 bits mono. */
    suspend fun exportWav(file: File, reverse: Boolean): File = withContext(Dispatchers.IO) {
        val data = if (reverse) samples.reversedArray() else samples
        val dataBytes = data.size * 2
        val buf = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray()).putInt(36 + dataBytes).put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(SAMPLE_RATE).putInt(SAMPLE_RATE * 2).putShort(2).putShort(16)
        buf.put("data".toByteArray()).putInt(dataBytes)
        data.forEach { buf.putShort(it) }
        file.writeBytes(buf.array())
        file
    }
}
