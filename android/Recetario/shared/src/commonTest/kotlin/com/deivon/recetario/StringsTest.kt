package com.deivon.recetario

import com.deivon.recetario.ui.AppLanguage
import com.deivon.recetario.ui.EnStrings
import com.deivon.recetario.ui.stringsFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class StringsTest {
    @Test
    fun everyLanguageHasItsOwnTexts() {
        val english = EnStrings
        AppLanguage.entries.filter { it.code != null && it.code != "en" }.forEach { language ->
            val s = stringsFor(language.code!!)
            // A sample across every screen: a missing override would fall back to English.
            listOf(
                s.searchHint to english.searchHint, s.addManually to english.addManually,
                s.favoritesOnly to english.favoritesOnly, s.adjustQuantities to english.adjustQuantities,
                s.settings to english.settings, s.languageSystem to english.languageSystem,
                s.startCooking to english.startCooking, s.fileSaved to english.fileSaved,
                s.rateAs(3) to english.rateAs(3), s.scaledBy("2") to english.scaledBy("2"),
            ).forEach { (translated, original) -> assertNotEquals(original, translated, "${language.name}: $translated") }
            assertEquals(',', s.decimalSeparator, language.name)
        }
    }

    @Test
    fun unknownLanguagesFallBackToEnglish() {
        assertSame(EnStrings, stringsFor("ja"))
        assertSame(stringsFor("es"), stringsFor("es-AR"))
        assertEquals("Réglages", stringsFor("fr").settings)
    }
}
