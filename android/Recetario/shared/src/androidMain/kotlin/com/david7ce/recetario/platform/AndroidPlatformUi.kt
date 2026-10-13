package com.david7ce.recetario.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.david7ce.recetario.ui.PlatformUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * System pickers through the Activity Result API: no storage permission is needed, the user picks
 * each file. Reading and writing happen off the main thread; callbacks run on it.
 */
@Composable
fun rememberPlatformUi(): PlatformUi {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pending = remember { Pending() }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        pending.read(context, scope, uri)
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pending.read(context, scope, uri)
    }
    val jsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        pending.write(context, scope, uri)
    }
    val zipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        pending.write(context, scope, uri)
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        pending.write(context, scope, uri)
    }

    return remember(imageLauncher, openLauncher, jsonLauncher, zipLauncher, pdfLauncher) {
        object : PlatformUi {
            override fun pickImage(onResult: (Result<ByteArray>?) -> Unit) {
                pending.onRead = onResult
                imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }

            override fun openFile(onResult: (Result<ByteArray>?) -> Unit) {
                pending.onRead = onResult
                // Any type: providers label JSON and ZIP files inconsistently. The content is checked after reading.
                openLauncher.launch(arrayOf("*/*"))
            }

            override fun saveFile(fileName: String, mimeType: String, bytes: ByteArray, onResult: (Boolean) -> Unit) {
                pending.toWrite = bytes
                pending.onWritten = onResult
                when (mimeType) {
                    "application/zip" -> zipLauncher.launch(fileName)
                    "application/pdf" -> pdfLauncher.launch(fileName)
                    else -> jsonLauncher.launch(fileName)
                }
            }

            override fun openUrl(url: String) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: ActivityNotFoundException) {
                    Log.w("Recetario", "No app can open $url", e)
                }
            }
        }
    }
}

/** The callback waiting for the picker that is currently open (there is only ever one). */
private class Pending {
    var onRead: ((Result<ByteArray>?) -> Unit)? = null
    var toWrite: ByteArray? = null
    var onWritten: ((Boolean) -> Unit)? = null

    fun read(context: Context, scope: CoroutineScope, uri: Uri?) {
        val callback = onRead ?: return
        onRead = null
        if (uri == null) return callback(null)
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("No stream for $uri")
                    Result.success(stream.use { readLimited(it) })
                } catch (e: IOException) {
                    Result.failure(e)
                } catch (e: SecurityException) {
                    Result.failure(e)
                }
            }
            callback(result)
        }
    }

    fun write(context: Context, scope: CoroutineScope, uri: Uri?) {
        val bytes = toWrite
        val callback = onWritten
        toWrite = null
        onWritten = null
        if (bytes == null || callback == null) return
        if (uri == null) return // cancelled: nothing to report
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    val stream = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("No stream for $uri")
                    stream.use { it.write(bytes) }
                    true
                } catch (e: IOException) {
                    Log.w("Recetario", "Could not write $uri", e)
                    false
                } catch (e: SecurityException) {
                    Log.w("Recetario", "Could not write $uri", e)
                    false
                }
            }
            callback(ok)
        }
    }

    private fun readLimited(input: java.io.InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val chunk = ByteArray(64 * 1024)
        var n = input.read(chunk)
        while (n >= 0) {
            if (out.size() + n > MAX_FILE_BYTES) throw IOException("File too large")
            out.write(chunk, 0, n)
            n = input.read(chunk)
        }
        return out.toByteArray()
    }

    private companion object {
        const val MAX_FILE_BYTES = 300 * 1024 * 1024
    }
}
