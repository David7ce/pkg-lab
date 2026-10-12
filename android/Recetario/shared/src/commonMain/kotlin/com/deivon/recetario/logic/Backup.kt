package com.deivon.recetario.logic

import com.deivon.recetario.model.Recipe
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

/**
 * Full backup = a ZIP with:
 * - `manifest.json`: format, version, counts and the list of image files;
 * - `recipes.json`: the [RecipeJson] library file, with image names kept;
 * - `images/<file>`: each referenced image, byte for byte.
 *
 * This object works on the archive as a name → bytes map, so it is platform-independent and
 * testable; zipping itself is done by [com.deivon.recetario.platform.ZipCodec].
 */
object Backup {
    const val FORMAT = "recetario-backup"
    const val VERSION = 1
    const val MANIFEST = "manifest.json"
    const val DATA = "recipes.json"
    const val IMAGES = "images/"

    /** Restores refuse archives bigger than this once unpacked (protects memory and against zip bombs). */
    const val MAX_UNPACKED_BYTES = 300L * 1024 * 1024
    const val MAX_IMAGE_BYTES = 20L * 1024 * 1024

    @Serializable
    data class Manifest(
        val format: String,
        val version: Int,
        val createdAt: Long,
        val recipeCount: Int,
        val images: List<String>,
        val app: String = "Recetario",
    )

    class Built(val entries: Map<String, ByteArray>, val missingImages: Int)

    /** Recipes whose image file is gone keep their data but lose the image reference (counted in [Built.missingImages]). */
    fun build(recipes: List<Recipe>, readImage: (String) -> ByteArray?, now: Long): Built {
        val images = linkedMapOf<String, ByteArray>()
        var missing = 0
        val kept = recipes.map { r ->
            val name = r.imageFile ?: return@map r
            val bytes = images[name] ?: readImage(name)
            if (bytes == null) {
                missing++
                r.copy(imageFile = null)
            } else {
                images[name] = bytes
                r
            }
        }
        val manifest = Manifest(FORMAT, VERSION, now, kept.size, images.keys.toList())
        val entries = linkedMapOf(
            MANIFEST to RecipeJson.json.encodeToString(Manifest.serializer(), manifest).encodeToByteArray(),
            DATA to RecipeJson.encode(kept, now, keepImageNames = true).encodeToByteArray(),
        )
        images.forEach { (name, bytes) -> entries[IMAGES + name] = bytes }
        return Built(entries, missing)
    }

    class Content(val recipes: List<Recipe>, val images: Map<String, ByteArray>)

    /**
     * Validates everything before anything is written: manifest, recipe data, entry names,
     * and that every referenced image is present and looks like an image. Throws [ImportException].
     */
    fun parse(entries: Map<String, ByteArray>): Content {
        entries.keys.firstOrNull { !isAllowedEntry(it) }?.let { throw ImportException(ImportFailure.UnsafeEntry(it)) }
        val manifestBytes = entries[MANIFEST] ?: throw ImportException(ImportFailure.NotRecognized)
        val manifest = try {
            RecipeJson.json.decodeFromString(Manifest.serializer(), manifestBytes.decodeToString())
        } catch (e: SerializationException) {
            throw ImportException(ImportFailure.NotRecognized)
        } catch (e: IllegalArgumentException) {
            throw ImportException(ImportFailure.NotRecognized)
        }
        if (manifest.format != FORMAT) throw ImportException(ImportFailure.NotRecognized)
        if (manifest.version !in 1..VERSION) throw ImportException(ImportFailure.UnsupportedVersion(manifest.version))

        val data = entries[DATA] ?: throw ImportException(ImportFailure.Malformed(DATA))
        val recipes = RecipeJson.decode(data.decodeToString())
        if (recipes.size != manifest.recipeCount) throw ImportException(ImportFailure.Malformed("recipeCount"))

        val images = mutableMapOf<String, ByteArray>()
        for (name in recipes.mapNotNull { it.imageFile }.distinct()) {
            val bytes = entries[IMAGES + name] ?: throw ImportException(ImportFailure.MissingImage(name))
            if (bytes.size > MAX_IMAGE_BYTES || !looksLikeImage(bytes)) {
                throw ImportException(ImportFailure.Malformed(IMAGES + name))
            }
            images[name] = bytes
        }
        return Content(recipes, images)
    }

    private fun isAllowedEntry(name: String): Boolean =
        name == MANIFEST || name == DATA ||
            (name.startsWith(IMAGES) && RecipeRules.isValidImageName(name.removePrefix(IMAGES)))

    /** JPEG, PNG or WebP signature. */
    fun looksLikeImage(b: ByteArray): Boolean {
        fun at(i: Int) = if (i < b.size) b[i].toInt() and 0xFF else -1
        val jpeg = at(0) == 0xFF && at(1) == 0xD8 && at(2) == 0xFF
        val png = at(0) == 0x89 && at(1) == 'P'.code && at(2) == 'N'.code && at(3) == 'G'.code
        val webp = at(0) == 'R'.code && at(1) == 'I'.code && at(2) == 'F'.code && at(3) == 'F'.code &&
            at(8) == 'W'.code && at(9) == 'E'.code && at(10) == 'B'.code && at(11) == 'P'.code
        return jpeg || png || webp
    }

    /** ZIP files start with "PK\u0003\u0004"; anything else is treated as JSON text. */
    fun isZip(bytes: ByteArray): Boolean =
        bytes.size >= 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() &&
            bytes[2] == 3.toByte() && bytes[3] == 4.toByte()
}
