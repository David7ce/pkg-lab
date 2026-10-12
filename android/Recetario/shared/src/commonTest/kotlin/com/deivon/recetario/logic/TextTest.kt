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
    fun isoDates() {
        assertEquals("1970-01-01", Text.isoDate(0))
        assertEquals("2025-10-12", Text.isoDate(1_760_227_200_000))
        assertEquals("2000-02-29", Text.isoDate(951_782_400_000))
        assertEquals("1969-12-31", Text.isoDate(-86_400_000))
    }
}
