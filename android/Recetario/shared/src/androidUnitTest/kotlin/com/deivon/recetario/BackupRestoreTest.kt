package com.deivon.recetario

import com.deivon.recetario.logic.Backup
import com.deivon.recetario.logic.ConflictPolicy
import com.deivon.recetario.logic.ImageChange
import com.deivon.recetario.logic.ImportException
import com.deivon.recetario.logic.ImportFailure
import com.deivon.recetario.logic.ImportPlan
import com.deivon.recetario.platform.JvmZipCodec
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Full path: library → ZIP bytes → validated restore into an empty library, photos included. */
class BackupRestoreTest {
    private val zip = JvmZipCodec()

    @Test
    fun restoresRecipesAndPhotosIntoAnEmptyLibrary() {
        val sourceImages = MemoryImageStore()
        val source = newRepository(sourceImages) { "unused" }
        val withPhoto = source.save(recipe("a"), ImageChange.Replace(fakeJpeg(3)))
        source.save(recipe("b", title = "Gazpacho"), ImageChange.Keep)

        val built = Backup.build(source.loadAll(), sourceImages::read, now = 10)
        val bytes = zip.zip(built.entries)
        assertTrue(Backup.isZip(bytes))

        val content = Backup.parse(zip.unzip(bytes, Backup.MAX_UNPACKED_BYTES))
        val targetImages = MemoryImageStore()
        val target = newRepository(targetImages) { "unused" }
        val plan = ImportPlan.of(emptyMap(), content.recipes)
        val result = target.applyImport(plan.resolve(ConflictPolicy.KeepMine, emptyMap(), { "unused" }, 11), content.images)

        assertEquals(2, result.inserted)
        val restored = target.loadAll().associateBy { it.id }
        assertTrue(source.loadAll().all { original -> restored.getValue(original.id).sameContentAs(original) })
        val photo = restored.getValue("a").imageFile!!
        assertContentEquals(sourceImages.read(withPhoto.imageFile!!), targetImages.read(photo))
    }

    @Test
    fun refusesArchivesThatUnpackTooLarge() {
        val bytes = zip.zip(mapOf("manifest.json" to ByteArray(10_000)))
        val failure = assertFailsWith<ImportException> { zip.unzip(bytes, maxTotalBytes = 1_000) }.failure
        assertEquals(ImportFailure.TooLarge, failure)
    }

    @Test
    fun refusesWhatIsNotAZip() {
        val failure = assertFailsWith<ImportException> { zip.unzip("hello".encodeToByteArray(), 1_000) }.failure
        assertTrue(failure == ImportFailure.NotRecognized || failure is ImportFailure.Malformed)
    }

    @Test
    fun aRestoreThatFailsValidationWritesNothing() {
        val images = MemoryImageStore()
        val repo = newRepository(images) { "unused" }
        val built = Backup.build(listOf(recipe("a", imageFile = "img1.jpg")), { fakeJpeg(1) }, now = 1)
        val broken = zip.zip(built.entries - "images/img1.jpg")
        val failure = assertFailsWith<ImportException> { Backup.parse(zip.unzip(broken, Backup.MAX_UNPACKED_BYTES)) }.failure
        assertIs<ImportFailure.MissingImage>(failure)
        assertTrue(repo.loadAll().isEmpty())
        assertTrue(images.files.isEmpty())
    }
}
