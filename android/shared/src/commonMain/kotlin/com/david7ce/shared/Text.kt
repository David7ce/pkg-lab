package com.david7ce.shared

/**
 * Reverses text by code point (not by UTF-16 char), so it doesn't break emoji or surrogate
 * pairs. Combining marks (e.g. Hindi) can still end up displaced.
 */
fun reverseText(s: String): String {
    val cps = s.codePoints().toArray()
    cps.reverse()
    return String(cps, 0, cps.size)
}
