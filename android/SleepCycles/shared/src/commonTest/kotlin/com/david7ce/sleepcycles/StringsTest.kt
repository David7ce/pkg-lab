package com.david7ce.sleepcycles

import com.david7ce.sleepcycles.logic.AgeRange
import com.david7ce.sleepcycles.logic.AppLanguage
import com.david7ce.sleepcycles.ui.EnStrings
import com.david7ce.sleepcycles.ui.stringsFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class StringsTest {
    @Test
    fun everyLanguageHasItsOwnTexts() {
        val en = EnStrings
        AppLanguage.entries.filter { it.code != null && it.code != "en" }.forEach { language ->
            val s = stringsFor(language.code!!)
            listOf(
                s.wakeUpAt to en.wakeUpAt, s.goToBedAt to en.goToBedAt, s.sleepNow to en.sleepNow,
                s.resultsWake to en.resultsWake, s.resultsBed to en.resultsBed,
                s.hint(15) to en.hint(15), s.cycles(3, "4.5") to en.cycles(3, "4.5"),
                s.ratingGood to en.ratingGood, s.ratingOk to en.ratingOk, s.ratingLow to en.ratingLow,
                s.ageLabel(AgeRange.Adult) to en.ageLabel(AgeRange.Adult),
                s.settings to en.settings, s.ageRange to en.ageRange, s.fallAsleepTime to en.fallAsleepTime,
                s.timeFormat to en.timeFormat, s.format12 to en.format12, s.theme to en.theme,
                s.themeDark to en.themeDark, s.language to en.language, s.languageSystem to en.languageSystem,
                s.howItWorks to en.howItWorks, s.howItWorksBody to en.howItWorksBody, s.gotIt to en.gotIt,
                s.back to en.back, s.close to en.close, s.cancel to en.cancel,
            ).forEach { (translated, original) -> assertNotEquals(original, translated, "${language.name}: $translated") }
            assertEquals(',', s.decimalSeparator, language.name)
        }
        assertEquals('.', en.decimalSeparator)
    }

    @Test
    fun placeholdersAreFilled() {
        AppLanguage.entries.mapNotNull { it.code }.forEach { code ->
            val s = stringsFor(code)
            assertTrue(s.hint(25).contains("25"), "$code hint")
            assertTrue(s.cycles(6, "9").contains("6") && s.cycles(6, "9").contains("9"), "$code cycles")
            assertTrue(s.versionLabel("1.0.7").contains("1.0.7"), "$code version")
        }
    }

    @Test
    fun oneCycleIsSingular() {
        assertEquals("1 cycle · 1.5 h", EnStrings.cycles(1, "1.5"))
        assertEquals("6 cycles · 9 h", EnStrings.cycles(6, "9"))
    }

    @Test
    fun unknownOrRegionalLanguagesResolveSafely() {
        assertSame(EnStrings, stringsFor("ja"))
        assertSame(EnStrings, stringsFor("ar"))
        assertSame(EnStrings, stringsFor(""))
        assertSame(stringsFor("pt"), stringsFor("pt-BR"))
        assertSame(stringsFor("es"), stringsFor("ES"))
    }
}
