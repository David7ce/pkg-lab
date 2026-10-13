package com.david7ce.vozreves

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun headerIsValidPcm16Mono() {
        val bytes = wavBytes(shortArrayOf(1, -1, 32767, -32768))
        assertEquals(44 + 8, bytes.size)
        assertEquals("RIFF", String(bytes, 0, 4))
        assertEquals("WAVE", String(bytes, 8, 4))
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(36 + 8, b.getInt(4))
        assertEquals(1.toShort(), b.getShort(22))   // mono
        assertEquals(SAMPLE_RATE, b.getInt(24))
        assertEquals(16.toShort(), b.getShort(34))  // bits per sample
        assertEquals(8, b.getInt(40))               // data size
    }

    @Test fun writeAndReadRoundTrip() {
        val data = ShortArray(1000) { (it * 37 - 5000).toShort() }
        val file = tmp.newFile("a.wav")
        writeWav(file, data)
        assertArrayEquals(data, readWav(file))
    }

    @Test fun emptyOrTruncatedFileReadsEmpty() {
        assertEquals(0, readWav(tmp.newFile("empty.wav")).size)
    }
}

class HistoryTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun savesListsRenamesAndDeletes() {
        val history = History(tmp.newFolder("rec"))
        history.save(ShortArray(SAMPLE_RATE)) // 1 s
        val item = history.list().single()
        assertEquals(1f, item.seconds, 0.001f)
        assertNull(item.title)

        history.rename(item, "  Canción del revés  ")
        assertEquals("Canción del revés", history.list().single().title)

        history.rename(history.list().single(), "") // empty → back to the date
        assertNull(history.list().single().title)

        history.rename(history.list().single(), "x".repeat(200))
        assertEquals(History.MAX_TITLE, history.list().single().title!!.length)

        history.delete(history.list().single())
        assertEquals(0, history.list().size)
    }
}

class NamesTest {
    @Test fun defaultNameHasNoIllegalCharactersAndIncludesTheDay() {
        val t = java.util.GregorianCalendar(2026, java.util.Calendar.OCTOBER, 12, 14, 30, 5).time
        assertEquals("reverse_voice_2026_10_12_14-30-05.wav", voiceFileName(t, reverse = true))
        assertEquals("normal_voice_2026_10_12_14-30-05.wav", voiceFileName(t, reverse = false))
        assertEquals(false, voiceFileName(t, true).contains(':'))
    }
}

class PhrasesTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun seedsOnceAddsRemovesAndRejectsDuplicates() {
        val phrases = Phrases(java.io.File(tmp.root, "p.txt"))
        phrases.seedIfNew(listOf("Hola", "Adiós"))
        assertEquals(listOf("Hola", "Adiós"), phrases.list())
        phrases.remove("Hola"); phrases.remove("Adiós")
        phrases.seedIfNew(listOf("Hola")) // the user emptied the list: do not bring the examples back
        assertEquals(0, phrases.list().size)

        assertEquals(true, phrases.add("  Buenos   días "))
        assertEquals(false, phrases.add("buenos días"))
        assertEquals(false, phrases.add("   "))
        assertEquals(true, phrases.add("Otra"))
        assertEquals(listOf("Otra", "Buenos días"), phrases.list())
        phrases.remove("Otra")
        assertEquals(listOf("Buenos días"), phrases.list())
        assertEquals(Phrases.MAX_LENGTH, Phrases.clean("x".repeat(500)).length)
    }
}

class DecodeWavTest {
    private fun wav(rate: Int, channels: Int, frames: ShortArray, extraChunk: Boolean = false): ByteArray {
        val extra = if (extraChunk) ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
            .put("LIST".toByteArray()).putInt(4).put("abcd".toByteArray()).array() else ByteArray(0)
        val b = ByteBuffer.allocate(36 + extra.size + 8 + frames.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(0).put("WAVE".toByteArray())
        b.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(channels.toShort())
            .putInt(rate).putInt(rate * 2 * channels).putShort((2 * channels).toShort()).putShort(16)
        b.put(extra).put("data".toByteArray()).putInt(frames.size * 2)
        frames.forEach { b.putShort(it) }
        return b.array()
    }

    @Test fun sameRateIsUnchanged() {
        val data = ShortArray(100) { it.toShort() }
        assertArrayEquals(data, decodeWav(wav(SAMPLE_RATE, 1, data)))
    }

    @Test fun halfRateDoublesLengthAndKeepsEnds() {
        val out = decodeWav(wav(SAMPLE_RATE / 2, 1, ShortArray(1000) { 100 }))
        assertEquals(2000, out.size)
        assertEquals(100.toShort(), out[0])
        assertEquals(100.toShort(), out[1000])
    }

    @Test fun stereoIsMixedToMonoAndExtraChunksAreSkipped() {
        val out = decodeWav(wav(SAMPLE_RATE, 2, shortArrayOf(100, 300, 200, 400), extraChunk = true))
        assertArrayEquals(shortArrayOf(200, 300), out)
    }

    @Test fun garbageGivesEmpty() {
        assertEquals(0, decodeWav(ByteArray(10)).size)
        assertEquals(0, decodeWav(ByteArray(100)).size)
    }
}

class SharedTest {
    @Test fun reverseTextReversesByCodePoints() {
        assertEquals("olleH", com.david7ce.shared.reverseText("Hello"))
        assertEquals("a😀", com.david7ce.shared.reverseText("😀a"))
        assertEquals("", com.david7ce.shared.reverseText(""))
    }

    @Test fun selectionToggleSelectAllClearAndRetain() {
        val sel = com.david7ce.shared.ui.Selection<String>()
        assertEquals(false, sel.active)
        sel.toggle("a"); sel.toggle("b")
        assertEquals(true, "a" in sel)
        sel.toggle("a")
        assertEquals(false, "a" in sel)
        sel.selectAll(listOf("a", "b", "c"))
        assertEquals(3, sel.keys.size)
        sel.retain(listOf("b", "c"))          // "a" desapareció de la lista
        assertEquals(setOf("b", "c"), sel.keys)
        sel.clear()
        assertEquals(false, sel.active)
    }
}
