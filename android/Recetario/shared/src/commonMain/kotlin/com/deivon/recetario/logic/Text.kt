package com.deivon.recetario.logic

/** Small text helpers shared by search, import and file naming. */
object Text {
    private val accents = mapOf(
        'á' to 'a', 'à' to 'a', 'ä' to 'a', 'â' to 'a', 'ã' to 'a', 'å' to 'a',
        'é' to 'e', 'è' to 'e', 'ë' to 'e', 'ê' to 'e',
        'í' to 'i', 'ì' to 'i', 'ï' to 'i', 'î' to 'i',
        'ó' to 'o', 'ò' to 'o', 'ö' to 'o', 'ô' to 'o', 'õ' to 'o',
        'ú' to 'u', 'ù' to 'u', 'ü' to 'u', 'û' to 'u',
        'ñ' to 'n', 'ç' to 'c',
    )

    /** Lower case without accents, so "Ñoquis" matches "noquis" and "crème" matches "creme". */
    fun fold(s: String): String = buildString(s.length) {
        for (c in s.lowercase()) append(accents[c] ?: c)
    }

    /** ASCII file-name stem: "Tortilla de patatas" → "tortilla-de-patatas". */
    fun slug(s: String, fallback: String = "recipe"): String =
        fold(s).replace(Regex("[^a-z0-9]+"), "-").trim('-').take(60).ifEmpty { fallback }

    private val entity = Regex("&(#[0-9]+|#[xX][0-9a-fA-F]+|[a-zA-Z]+);")
    private val named = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
        "frac12" to "½", "frac14" to "¼", "frac34" to "¾", "deg" to "°", "ndash" to "–", "mdash" to "—",
        "hellip" to "…", "rsquo" to "’", "lsquo" to "‘", "rdquo" to "”", "ldquo" to "“",
    )

    /** Decodes HTML entities; unknown named entities are left as they are. */
    fun decodeEntities(s: String): String = entity.replace(s) { m ->
        val body = m.groupValues[1]
        when {
            body.startsWith("#x") || body.startsWith("#X") ->
                body.drop(2).toIntOrNull(16)?.let(::codePointToString) ?: m.value
            body.startsWith("#") -> body.drop(1).toIntOrNull()?.let(::codePointToString) ?: m.value
            else -> named[body] ?: m.value
        }
    }

    private fun codePointToString(cp: Int): String? = when {
        cp in 0..0xD7FF || cp in 0xE000..0xFFFF -> Char(cp).toString()
        cp in 0x10000..0x10FFFF -> {
            val v = cp - 0x10000
            charArrayOf(Char(0xD800 + (v shr 10)), Char(0xDC00 + (v and 0x3FF))).concatToString()
        }
        else -> null
    }

    private val tags = Regex("<[^>]*>")
    private val spaces = Regex("\\s+")

    private val spaceBeforePunctuation = Regex(" ([.,;:!?)])")

    /** Plain text from an HTML fragment: tags removed, entities decoded, whitespace collapsed. */
    fun plain(html: String): String =
        decodeEntities(tags.replace(html, " ")).replace(spaces, " ").replace(spaceBeforePunctuation, "$1").trim()

    /** Epoch millis → "YYYY-MM-DD" (UTC), for file names. */
    fun isoDate(epochMillis: Long): String {
        // Days-to-civil conversion (H. Hinnant), valid for the whole Long-day range we can meet.
        val z = epochMillis.floorDiv(86_400_000L) + 719_468
        val era = z.floorDiv(146_097L)
        val doe = z - era * 146_097
        val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val day = doy - (153 * mp + 2) / 5 + 1
        val month = if (mp < 10) mp + 3 else mp - 9
        val year = yoe + era * 400 + if (month <= 2) 1 else 0
        return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }
}
