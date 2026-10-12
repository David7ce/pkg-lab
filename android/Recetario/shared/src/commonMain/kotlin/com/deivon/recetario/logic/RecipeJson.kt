package com.deivon.recetario.logic

import com.deivon.recetario.model.Recipe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Why a file or page could not be imported. The UI turns each into a sentence. */
sealed interface ImportFailure {
    data object NotRecognized : ImportFailure
    data class UnsupportedVersion(val version: Int) : ImportFailure
    data class Malformed(val detail: String) : ImportFailure
    data class DuplicateIds(val ids: List<String>) : ImportFailure
    data class InvalidRecipe(val title: String, val problems: List<Problem>) : ImportFailure
    data class MissingImage(val name: String) : ImportFailure
    data class UnsafeEntry(val name: String) : ImportFailure
    data object TooLarge : ImportFailure
    data object Empty : ImportFailure
}

class ImportException(val failure: ImportFailure) : Exception(failure.toString())

/**
 * The app's JSON format, used for single-recipe export, library export and inside backups:
 * `{"format": "recetario", "version": 1, "exportedAt": …, "recipes": [ … ]}`.
 * Newer versions are rejected rather than half-read; unknown fields from the same version are ignored.
 */
object RecipeJson {
    const val FORMAT = "recetario"
    const val VERSION = 2 // 2: adds "notes". Version 1 files still import.

    @Serializable
    private data class Header(val format: String? = null, val version: Int? = null)

    @Serializable
    private data class LibraryFile(
        val format: String,
        val version: Int,
        val exportedAt: Long,
        val recipes: List<Recipe>,
        @SerialName("app") val app: String = "Recetario",
    )

    @OptIn(ExperimentalSerializationApi::class)
    internal val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        prettyPrint = true
    }

    /** Encodes [recipes]. Without [keepImageNames] the image references are dropped (plain JSON carries no images). */
    fun encode(recipes: List<Recipe>, exportedAt: Long, keepImageNames: Boolean = false): String =
        json.encodeToString(
            LibraryFile.serializer(),
            LibraryFile(FORMAT, VERSION, exportedAt, if (keepImageNames) recipes else recipes.map { it.copy(imageFile = null) }),
        )

    /**
     * Decodes and validates a whole file. Throws [ImportException]; on success every recipe passed
     * [RecipeRules] and ids are unique within the file.
     */
    fun decode(text: String): List<Recipe> {
        val header = try {
            json.decodeFromString(Header.serializer(), text)
        } catch (e: SerializationException) {
            throw ImportException(ImportFailure.NotRecognized)
        } catch (e: IllegalArgumentException) {
            throw ImportException(ImportFailure.NotRecognized)
        }
        if (header.format != FORMAT) throw ImportException(ImportFailure.NotRecognized)
        val version = header.version ?: throw ImportException(ImportFailure.Malformed("version"))
        if (version > VERSION || version < 1) throw ImportException(ImportFailure.UnsupportedVersion(version))

        val file = try {
            json.decodeFromString(LibraryFile.serializer(), text)
        } catch (e: SerializationException) {
            throw ImportException(ImportFailure.Malformed(e.message?.lineSequence()?.firstOrNull().orEmpty().take(200)))
        } catch (e: IllegalArgumentException) {
            throw ImportException(ImportFailure.Malformed(e.message.orEmpty().take(200)))
        }
        val recipes = file.recipes.map { it.copy(title = it.title.trim()) }
        if (recipes.isEmpty()) throw ImportException(ImportFailure.Empty)
        val duplicates = recipes.groupBy { it.id }.filterValues { it.size > 1 }.keys
        if (duplicates.isNotEmpty()) throw ImportException(ImportFailure.DuplicateIds(duplicates.toList()))
        recipes.forEach { r ->
            val problems = RecipeRules.problems(r)
            if (problems.isNotEmpty()) throw ImportException(ImportFailure.InvalidRecipe(r.title, problems))
        }
        return recipes
    }
}
