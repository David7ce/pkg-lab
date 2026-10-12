package com.deivon.vozreves

import org.junit.Assert.assertArrayEquals
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
