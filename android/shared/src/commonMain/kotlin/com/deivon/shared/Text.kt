package com.deivon.shared

/**
 * Invierte un texto por puntos de código (no por caracteres UTF-16), para no romper emojis ni
 * pares sustitutos. Las marcas combinadas (p. ej. hindi) siguen pudiendo quedar desplazadas.
 */
fun reverseText(s: String): String {
    val cps = s.codePoints().toArray()
    cps.reverse()
    return String(cps, 0, cps.size)
}
