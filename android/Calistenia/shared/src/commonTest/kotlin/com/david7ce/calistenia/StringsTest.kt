package com.david7ce.calistenia

import com.david7ce.calistenia.logic.AppLanguage
import com.david7ce.calistenia.ui.EnStrings
import com.david7ce.calistenia.ui.stringsFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class StringsTest {

    @Test
    fun everyOtherLanguageOverridesTheInterface() {
        val en = EnStrings
        AppLanguage.entries.filter { it.code != null && it.code != "en" }.forEach { language ->
            val s = stringsFor(language.code!!)
            listOf(
                s.settings to en.settings,
                s.save to en.save,
                s.cancel to en.cancel,
                s.delete to en.delete,
                s.train to en.train,
                s.history to en.history,
                s.workout to en.workout,
                s.rest to en.rest,
                s.addSet to en.addSet,
                s.finishWorkout to en.finishWorkout,
                s.duration to en.duration,
                s.bestSet to en.bestSet,
                s.exportBackup to en.exportBackup,
                s.errorSaving to en.errorSaving,
                s.deleteRoutineTitle to en.deleteRoutineTitle,
                s.importFailed to en.importFailed,
                s.themeDark to en.themeDark,
                s.languageSystem to en.languageSystem,
                s.noResults to en.noResults,
                s.exerciseHowTo to en.exerciseHowTo,
                s.descriptionLabel to en.descriptionLabel,
            ).forEach { (translated, original) ->
                assertNotEquals(original, translated, "${language.name}: $translated")
            }
        }
    }

    @Test
    fun placeholdersAreFilledInEveryLanguage() {
        AppLanguage.entries.mapNotNull { it.code }.forEach { code ->
            val s = stringsFor(code)
            assertTrue(s.setsCount(4).contains("4"), "$code setsCount")
            assertTrue(s.setsDone(2, 5).contains("2") && s.setsDone(2, 5).contains("5"), "$code setsDone")
            assertTrue(s.versionLabel("1.0.7").contains("1.0.7"), "$code version")
            assertTrue(s.importConfirmText(3, 7).contains("3") && s.importConfirmText(3, 7).contains("7"), "$code import")
            assertTrue(s.deleteRoutineText("Push").contains("Push"), "$code routine text")
            assertTrue(s.workoutSummary("5:03", 12, 4).contains("5:03"), "$code summary")
        }
    }

    @Test
    fun helpIsTheSameShapeInEveryLanguage() {
        val en = EnStrings
        AppLanguage.entries.mapNotNull { it.code }.forEach { code ->
            val s = stringsFor(code)
            assertEquals(en.helpParagraphs.size, s.helpParagraphs.size, "$code paragraph count")
            s.helpParagraphs.forEachIndexed { index, paragraph ->
                assertEquals(
                    en.helpParagraphs[index].split("**").size,
                    paragraph.split("**").size,
                    "$code paragraph $index bold markers",
                )
            }
        }
    }

    @Test
    fun englishCountsAreSingularWhenTheyShouldBe() {
        assertEquals("1 set", EnStrings.setsCount(1))
        assertEquals("3 sets", EnStrings.setsCount(3))
        assertEquals("1 exercise", EnStrings.exercisesCount(1))
        assertEquals("No matches.", EnStrings.noResults)
    }

    @Test
    fun unknownAndRegionalLanguagesResolveSafely() {
        assertSame(EnStrings, stringsFor("ja"))
        assertSame(EnStrings, stringsFor(""))
        assertSame(stringsFor("pt"), stringsFor("pt-BR"))
        assertSame(stringsFor("es"), stringsFor("ES"))
    }
}
