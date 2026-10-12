package com.deivon.vozreves

import java.io.File

/** Lista de frases del usuario: un archivo de texto con una frase por línea (privado de la app). */
class Phrases(private val file: File) {

    /** Si es la primera vez, guarda las frases de ejemplo; después la lista es solo del usuario. */
    fun seedIfNew(defaults: List<String>) { if (!file.exists()) write(defaults.map(::clean).filter { it.isNotEmpty() }) }

    fun list(): List<String> =
        if (file.exists()) file.readLines(Charsets.UTF_8).filter { it.isNotBlank() } else emptyList()

    /** Añade al principio. Devuelve false si está vacía, repetida o la lista está llena. */
    fun add(text: String): Boolean {
        val t = clean(text)
        val current = list()
        if (t.isEmpty() || current.any { it.equals(t, ignoreCase = true) } || current.size >= MAX_ITEMS) return false
        write(listOf(t) + current)
        return true
    }

    fun remove(text: String) = write(list().filter { it != text })

    private fun write(lines: List<String>) {
        file.parentFile?.mkdirs()
        file.writeText(lines.joinToString("\n"), Charsets.UTF_8)
    }

    companion object {
        const val MAX_ITEMS = 100
        const val MAX_LENGTH = 120
        fun clean(text: String) = text.replace(Regex("\\s+"), " ").trim().take(MAX_LENGTH)
    }
}
