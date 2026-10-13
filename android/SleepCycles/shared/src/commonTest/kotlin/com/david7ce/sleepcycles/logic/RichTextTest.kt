package com.david7ce.sleepcycles.logic

import kotlin.test.Test
import kotlin.test.assertEquals

class RichTextTest {
    @Test
    fun plainTextIsOneRun() {
        assertEquals(listOf("hello" to false), splitBold("hello"))
        assertEquals(emptyList(), splitBold(""))
    }

    @Test
    fun boldRunsAreFlagged() {
        assertEquals(
            listOf("a " to false, "b" to true, " c " to false, "d" to true, "." to false),
            splitBold("a **b** c **d**."),
        )
        assertEquals(listOf("only" to true), splitBold("**only**"))
    }

    @Test
    fun unmatchedMarkerKeepsTheTextVisible() {
        assertEquals(listOf("a **b" to false), splitBold("a **b"))
    }
}
