package com.deivon.vozreves

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Histórico de grabaciones: un WAV por grabación en la carpeta privada de la app. */
class History(private val dir: File) {

    data class Item(val file: File, val seconds: Float)

    fun list(): List<Item> = dir.listFiles { f -> f.extension == "wav" }.orEmpty()
        .sortedByDescending { it.name }
        .map { Item(it, (it.length() - 44) / 2f / SAMPLE_RATE) }

    fun save(data: ShortArray) {
        if (data.isEmpty()) return
        dir.mkdirs()
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        writeWav(File(dir, "rec_$stamp.wav"), data)
        list().drop(MAX_ITEMS).forEach { it.file.delete() } // Mantener el espacio acotado.
    }

    fun load(item: Item): ShortArray = readWav(item.file)
    fun delete(item: Item) { item.file.delete() }
    fun clear() { dir.listFiles()?.forEach { it.delete() } }

    companion object { const val MAX_ITEMS = 50 }
}
