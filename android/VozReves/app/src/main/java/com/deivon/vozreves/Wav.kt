package com.deivon.vozreves

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** WAV PCM 16 bits mono en memoria (cabecera de 44 bytes + muestras little-endian). */
fun wavBytes(data: ShortArray): ByteArray {
    val dataBytes = data.size * 2
    val buf = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
    buf.put("RIFF".toByteArray()).putInt(36 + dataBytes).put("WAVE".toByteArray())
    buf.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
        .putInt(SAMPLE_RATE).putInt(SAMPLE_RATE * 2).putShort(2).putShort(16)
    buf.put("data".toByteArray()).putInt(dataBytes)
    buf.asShortBuffer().put(data) // Copia en bloque: mucho más rápido que muestra a muestra.
    return buf.array()
}

fun writeWav(file: File, data: ShortArray) = file.writeBytes(wavBytes(data))

/** Lee los WAV que escribe [writeWav] (cabecera de 44 bytes). */
fun readWav(file: File): ShortArray {
    val bytes = file.readBytes()
    if (bytes.size <= 44) return ShortArray(0)
    val out = ShortArray((bytes.size - 44) / 2)
    ByteBuffer.wrap(bytes, 44, out.size * 2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(out)
    return out
}
