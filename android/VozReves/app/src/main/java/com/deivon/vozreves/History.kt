package com.deivon.vozreves

import java.io.File
import java.util.Date
import java.util.Properties

/**
 * Histórico de grabaciones: un WAV por grabación en la carpeta privada de la app.
 * Los nombres que pone el usuario van en titles.properties (nombre de archivo → título), así el
 * archivo conserva su fecha en el nombre y el orden no depende de lo que escriba el usuario.
 */
class History(private val dir: File) {

    data class Item(val file: File, val seconds: Float, val title: String?)

    private val titlesFile get() = File(dir, "titles.properties")

    private fun titles(): Properties = Properties().apply {
        if (titlesFile.exists()) titlesFile.reader(Charsets.UTF_8).use { load(it) }
    }

    private fun saveTitles(p: Properties) {
        dir.mkdirs()
        titlesFile.writer(Charsets.UTF_8).use { p.store(it, null) }
    }

    fun list(): List<Item> {
        val names = titles()
        return dir.listFiles { f -> f.extension == "wav" }.orEmpty()
            .sortedWith(compareByDescending<File> { it.lastModified() }.thenByDescending { it.name })
            .map { Item(it, (it.length() - 44) / 2f / SAMPLE_RATE, names.getProperty(it.name)?.takeIf(String::isNotBlank)) }
    }

    fun save(data: ShortArray) {
        if (data.isEmpty()) return
        dir.mkdirs()
        // reverse_voice_AAAA_MM_DD_HH-mm-ss.wav; si ya existe (dos en el mismo segundo) se añade un sufijo.
        var file = File(dir, voiceFileName(Date(), reverse = true))
        var n = 1
        while (file.exists()) file = File(dir, voiceFileName(Date(), true).removeSuffix(".wav") + "_${n++}.wav")
        writeWav(file, data)
        list().drop(MAX_ITEMS).forEach(::delete) // Mantener el espacio acotado.
    }

    fun load(item: Item): ShortArray = readWav(item.file)

    /** Título vacío = volver a mostrar la fecha. */
    fun rename(item: Item, title: String) {
        val p = titles()
        val t = title.trim().take(MAX_TITLE)
        if (t.isEmpty()) p.remove(item.file.name) else p.setProperty(item.file.name, t)
        saveTitles(p)
    }

    fun delete(item: Item) {
        item.file.delete()
        val p = titles()
        if (p.remove(item.file.name) != null) saveTitles(p)
    }

    fun clear() { dir.listFiles()?.forEach { it.delete() } }

    companion object {
        const val MAX_ITEMS = 50
        const val MAX_TITLE = 60
    }
}
