package com.deivon.recetario.logic

import com.deivon.recetario.fakeJpeg
import com.deivon.recetario.recipe
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackupTest {
    private val photo = fakeJpeg(1)
    private val recipes = listOf(recipe("a", imageFile = "img1.jpg"), recipe("b", title = "Sin foto"))

    private fun built() = Backup.build(recipes, { if (it == "img1.jpg") photo else null }, now = 5)

    @Test
    fun buildsManifestDataAndImages() {
        val b = built()
        assertEquals(setOf(Backup.MANIFEST, Backup.DATA, "images/img1.jpg"), b.entries.keys)
        assertEquals(0, b.missingImages)
        val content = Backup.parse(b.entries)
        assertEquals(recipes, content.recipes)
        assertContentEquals(photo, content.images["img1.jpg"])
    }

    @Test
    fun missingPhotosAreDroppedAndCounted() {
        val b = Backup.build(recipes, { null }, now = 5)
        assertEquals(1, b.missingImages)
        assertNull(Backup.parse(b.entries).recipes.first().imageFile)
    }

    private fun failure(entries: Map<String, ByteArray>) = assertFailsWith<ImportException> { Backup.parse(entries) }.failure

    @Test
    fun rejectsIncompleteOrUnsafeArchives() {
        val good = built().entries
        assertEquals(ImportFailure.MissingImage("img1.jpg"), failure(good - "images/img1.jpg"))
        assertEquals(ImportFailure.UnsafeEntry("../evil.jpg"), failure(good + ("../evil.jpg" to photo)))
        assertEquals(ImportFailure.UnsafeEntry("images/x.exe"), failure(good + ("images/x.exe" to photo)))
        assertEquals(ImportFailure.NotRecognized, failure(good - Backup.MANIFEST))
        assertIs<ImportFailure.Malformed>(failure(good - Backup.DATA))
        assertIs<ImportFailure.Malformed>(failure(good + ("images/img1.jpg" to "not an image".encodeToByteArray())))
    }

    @Test
    fun rejectsNewerBackups() {
        val entries = built().entries.toMutableMap()
        entries[Backup.MANIFEST] = entries.getValue(Backup.MANIFEST).decodeToString()
            .replace(Regex("\"version\"\\s*:\\s*1"), "\"version\": 7").encodeToByteArray()
        assertEquals(ImportFailure.UnsupportedVersion(7), failure(entries))
    }

    @Test
    fun recognisesFileKinds() {
        assertTrue(Backup.isZip(byteArrayOf(0x50, 0x4B, 3, 4, 0)))
        assertTrue(!Backup.isZip("{\"format\"".encodeToByteArray()))
        assertTrue(Backup.looksLikeImage(photo))
        assertTrue(!Backup.looksLikeImage(byteArrayOf(1, 2, 3)))
    }
}
