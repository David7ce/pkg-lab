package com.david7ce.calistenia.logic

/**
 * Languages the app offers. [code] is the ISO 639 code; System follows the device. Names are shown
 * in their own language.
 */
enum class AppLanguage(val code: String?, val nativeName: String) {
    System(null, ""),
    Es("es", "Español"),
    En("en", "English"),
    Fr("fr", "Français"),
    De("de", "Deutsch"),
    Pt("pt", "Português"),
    It("it", "Italiano"),
}

/**
 * The first language of the device list (most preferred first) that the app translates; otherwise the
 * first one, which then falls back to English. A device set to Japanese then Spanish gets Spanish.
 */
fun pickDeviceLanguage(deviceLanguages: List<String>): String {
    val supported = AppLanguage.entries.mapNotNull { it.code }
    val codes = deviceLanguages.map { it.lowercase().take(2) } // "pt-BR" -> "pt"
    return codes.firstOrNull { it in supported } ?: codes.firstOrNull().orEmpty()
}
