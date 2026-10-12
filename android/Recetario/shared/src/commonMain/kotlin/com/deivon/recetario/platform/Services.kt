package com.deivon.recetario.platform

import androidx.compose.ui.graphics.ImageBitmap
import com.deivon.recetario.model.Recipe
import com.deivon.recetario.ui.Strings
import kotlin.random.Random

/**
 * Everything the shared code needs from the platform, as small interfaces.
 * Android implementations live in androidMain; desktop ones will live in the future desktop source set.
 */
class Services(
    val images: ImageStore,
    val codec: ImageCodec,
    val zip: ZipCodec,
    val web: WebClient,
    /** Null where PDF export is not available. */
    val pdf: PdfExporter?,
    val clock: () -> Long,
    val newId: () -> String = ::randomId,
)

/** Recipe images as files in one private folder. Names are opaque: "<random>.jpg". */
interface ImageStore {
    /** Writes [bytes] under a new unique name and returns that name. */
    fun write(bytes: ByteArray, extension: String = "jpg"): String
    fun read(name: String): ByteArray?
    /** Returns false when the file was not there. */
    fun delete(name: String): Boolean
    fun list(): List<String>
}

interface ImageCodec {
    /** Decodes any common image, applies its rotation, downsizes it for a phone and re-encodes it as JPEG. Null if not an image. */
    fun normalize(bytes: ByteArray): ByteArray?
    /** For display; [maxSize] is the longest side in pixels. Null if the bytes cannot be decoded. */
    fun decode(bytes: ByteArray, maxSize: Int): ImageBitmap?
}

interface ZipCodec {
    fun zip(entries: Map<String, ByteArray>): ByteArray
    /** Throws [com.deivon.recetario.logic.ImportException] when not a ZIP or when it unpacks to more than [maxTotalBytes]. */
    fun unzip(bytes: ByteArray, maxTotalBytes: Long): Map<String, ByteArray>
}

class WebException(val kind: Kind, val httpStatus: Int? = null, cause: Throwable? = null) : Exception(kind.name, cause) {
    enum class Kind { InvalidUrl, Network, Http, TooLarge }
}

interface WebClient {
    /** GET over http(s), following redirects. Throws [WebException]. */
    suspend fun getText(url: String, maxBytes: Int): String
    suspend fun getBytes(url: String, maxBytes: Int): ByteArray
}

interface PdfExporter {
    /** One recipe per page group; a cover page when [cookbookTitle] is given. */
    fun export(recipes: List<Recipe>, image: (String) -> ByteArray?, strings: Strings, cookbookTitle: String?): ByteArray
}

/** 128 random bits as a UUID-shaped string (version 4 layout). */
fun randomId(): String {
    val b = Random.nextBytes(16)
    b[6] = ((b[6].toInt() and 0x0F) or 0x40).toByte()
    b[8] = ((b[8].toInt() and 0x3F) or 0x80).toByte()
    val hex = b.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}"
}
