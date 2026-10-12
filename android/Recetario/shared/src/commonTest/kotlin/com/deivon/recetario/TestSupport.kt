package com.deivon.recetario

import com.deivon.recetario.model.Ingredient
import com.deivon.recetario.model.Recipe
import com.deivon.recetario.platform.ImageStore

/** In-memory image folder for tests. */
class MemoryImageStore : ImageStore {
    val files = linkedMapOf<String, ByteArray>()
    private var counter = 0

    override fun write(bytes: ByteArray, extension: String): String {
        val name = "img${++counter}.$extension"
        files[name] = bytes
        return name
    }

    override fun read(name: String): ByteArray? = files[name]
    override fun delete(name: String): Boolean = files.remove(name) != null
    override fun list(): List<String> = files.keys.toList()
}

/** JPEG signature followed by a marker byte: enough for [com.deivon.recetario.logic.Backup.looksLikeImage]. */
fun fakeJpeg(marker: Int): ByteArray = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), marker.toByte())

fun recipe(
    id: String = "r1",
    title: String = "Tortilla",
    imageFile: String? = null,
    updatedAt: Long = 2,
    tags: List<String> = emptyList(),
) = Recipe(
    id = id,
    title = title,
    imageFile = imageFile,
    ingredients = listOf(Ingredient("huevos", 4.0), Ingredient("sal", note = "al gusto")),
    steps = listOf("Batir.", "Cuajar."),
    servings = 4,
    prepMinutes = 10,
    cookMinutes = 20,
    tags = tags,
    createdAt = 1,
    updatedAt = updatedAt,
)
