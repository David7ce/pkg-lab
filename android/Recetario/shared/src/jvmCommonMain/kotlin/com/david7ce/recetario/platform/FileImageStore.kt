package com.david7ce.recetario.platform

import com.david7ce.recetario.logic.RecipeRules
import java.io.File
import java.io.IOException

/** Images as files in one private folder. Writes go to a temp file first, so a file is either complete or absent. */
class FileImageStore(private val dir: File) : ImageStore {
    init {
        dir.mkdirs()
        dir.listFiles { f -> f.name.endsWith(".tmp") }?.forEach { it.delete() }
    }

    override fun write(bytes: ByteArray, extension: String): String {
        val ext = extension.lowercase().takeIf { it in setOf("jpg", "png", "webp") } ?: "jpg"
        val name = randomId().replace("-", "") + "." + ext
        val tmp = File(dir, "$name.tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(File(dir, name))) {
            tmp.delete()
            throw IOException("Could not store image $name")
        }
        return name
    }

    override fun read(name: String): ByteArray? = fileFor(name)?.takeIf { it.isFile }?.readBytes()

    override fun delete(name: String): Boolean = fileFor(name)?.delete() ?: false

    override fun list(): List<String> =
        dir.listFiles()?.filter { it.isFile && RecipeRules.isValidImageName(it.name) }?.map { it.name }.orEmpty()

    /** Only names the app itself produces are accepted, so a stored name can never escape the folder. */
    private fun fileFor(name: String): File? = if (RecipeRules.isValidImageName(name)) File(dir, name) else null
}
