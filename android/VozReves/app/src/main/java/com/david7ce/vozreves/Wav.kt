package com.david7ce.vozreves

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

/**
 * Decodifica el WAV PCM 16 bits que genera el motor de voz del sistema (cualquier frecuencia,
 * mono o estéreo, con otros bloques antes de "data") a mono a [SAMPLE_RATE]. Vacío si no entiende el archivo.
 */
fun decodeWav(bytes: ByteArray): ShortArray {
    if (bytes.size < 44 || String(bytes, 0, 4) != "RIFF" || String(bytes, 8, 4) != "WAVE") return ShortArray(0)
    val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    var channels = 0
    var rate = 0
    var bits = 0
    var pos = 12
    while (pos + 8 <= bytes.size) {
        val id = String(bytes, pos, 4)
        var size = b.getInt(pos + 4)
        val body = pos + 8
        when (id) {
            "fmt " -> if (body + 16 <= bytes.size) {
                channels = b.getShort(body + 2).toInt()
                rate = b.getInt(body + 4)
                bits = b.getShort(body + 14).toInt()
            }
            "data" -> {
                // Algunos motores dejan el tamaño en 0 o desbordado: se usa lo que haya hasta el final.
                if (size <= 0 || body + size > bytes.size) size = bytes.size - body
                if (channels !in 1..2 || rate <= 0 || bits != 16) return ShortArray(0)
                val frames = size / (2 * channels)
                val mono = ShortArray(frames)
                for (i in 0 until frames) {
                    var sum = 0
                    for (c in 0 until channels) sum += b.getShort(body + (i * channels + c) * 2)
                    mono[i] = (sum / channels).toShort()
                }
                return resample(mono, rate, SAMPLE_RATE)
            }
        }
        if (size < 0) return ShortArray(0)
        pos = body + size + (size and 1) // Los bloques se alinean a 2 bytes.
    }
    return ShortArray(0)
}

/** Cambio de frecuencia por interpolación lineal (suficiente para voz). */
fun resample(data: ShortArray, from: Int, to: Int): ShortArray {
    if (from == to || data.isEmpty()) return data
    val n = (data.size.toLong() * to / from).toInt()
    val ratio = from.toDouble() / to
    return ShortArray(n) { i ->
        val x = i * ratio
        val i0 = x.toInt().coerceAtMost(data.size - 1)
        val i1 = (i0 + 1).coerceAtMost(data.size - 1)
        val f = x - i0
        (data[i0] * (1 - f) + data[i1] * f).toInt().toShort()
    }
}
