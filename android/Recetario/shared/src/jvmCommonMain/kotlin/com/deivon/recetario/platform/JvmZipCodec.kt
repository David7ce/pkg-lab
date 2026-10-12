package com.deivon.recetario.platform

import com.deivon.recetario.logic.ImportException
import com.deivon.recetario.logic.ImportFailure
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** java.util.zip: same on Android and on desktop JVMs. */
class JvmZipCodec : ZipCodec {
    override fun zip(entries: Map<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    override fun unzip(bytes: ByteArray, maxTotalBytes: Long): Map<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        var total = 0L
        val chunk = ByteArray(64 * 1024)
        val zip = ZipInputStream(ByteArrayInputStream(bytes))
        try {
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    if (entry.name in result) throw ImportException(ImportFailure.Malformed("duplicate entry ${entry.name}"))
                    val buffer = ByteArrayOutputStream()
                    var n = zip.read(chunk)
                    while (n >= 0) {
                        total += n
                        if (total > maxTotalBytes) throw ImportException(ImportFailure.TooLarge)
                        buffer.write(chunk, 0, n)
                        n = zip.read(chunk)
                    }
                    result[entry.name] = buffer.toByteArray()
                }
                entry = zip.nextEntry
            }
        } catch (e: IOException) {
            throw ImportException(ImportFailure.Malformed("zip: ${e.message}"))
        } finally {
            zip.close()
        }
        if (result.isEmpty()) throw ImportException(ImportFailure.NotRecognized)
        return result
    }
}
