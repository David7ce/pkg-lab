package com.deivon.recetario.logic

import kotlin.test.Test
import kotlin.test.assertEquals

class TextTest {
    @Test
    fun foldsCaseAndAccents() = assertEquals("noquis con creme", Text.fold("Ñoquis con Crème"))

    @Test
    fun slugsAreFileSafe() {
        assertEquals("tortilla-de-patatas", Text.slug("Tortilla de Patatas!"))
        assertEquals("recipe", Text.slug("¿¡!?"))
    }

    @Test
    fun decodesEntities() = assertEquals("'&é&unknown;", Text.decodeEntities("&#x27;&amp;&#233;&unknown;"))

    @Test
    fun plainTextFromHtml() = assertEquals("Mix the flour & salt.", Text.plain("<p>Mix   the <b>flour</b> &amp; salt.</p>"))

    @Test
    fun parsesDateTimes() {
        assertEquals(1_747_121_454_009, Text.parseDateTime("2025-05-13T07:30:54.009Z"))
        assertEquals(1_772_456_006_000, Text.parseDateTime("2026-03-02 12:53:26"))
        assertEquals(1_709_164_800_000, Text.parseDateTime("2024-02-29"))
        assertEquals(1_791_792_000_000, Text.parseDateTime("2026-10-12T10:00:00+02:00"))
        assertEquals(null, Text.parseDateTime("ayer"))
    }

    @Test
    fun stripsSimpleMarkdown() =
        assertEquals("Freír bien con aceite.", Text.stripMarkdown("Freír **bien** con [aceite](https://x.es/a)."))

    @Test
    fun isoDates() {
        assertEquals("1970-01-01", Text.isoDate(0))
        assertEquals("2025-10-12", Text.isoDate(1_760_227_200_000))
        assertEquals("2000-02-29", Text.isoDate(951_782_400_000))
        assertEquals("1969-12-31", Text.isoDate(-86_400_000))
    }
}
