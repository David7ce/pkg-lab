package com.deivon.recetario.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.deivon.recetario.ui.PlatformUi
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.URI

/**
 * The system's own file dialogs (AWT FileDialog: Explorer on Windows, GTK on most Linux desktops).
 * Each dialog is modal, so it blocks until the user chooses or cancels.
 */
class DesktopPlatformUi : PlatformUi {
    override fun pickImage(onResult: (Result<ByteArray>?) -> Unit) = open(onResult, IMAGE_EXTENSIONS)

    override fun openFile(onResult: (Result<ByteArray>?) -> Unit) = open(onResult, null)

    override fun saveFile(fileName: String, mimeType: String, bytes: ByteArray, onResult: (Boolean) -> Unit) {
        val dialog = FileDialog(null as Frame?, "Guardar", FileDialog.SAVE)
        dialog.file = fileName
        dialog.isVisible = true
        val name = dialog.file ?: return // cancelled: nothing to report, same as Android
        onResult(runCatching { File(dialog.directory, name).writeBytes(bytes) }.isSuccess)
    }

    override fun openUrl(url: String) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            runCatching { Desktop.getDesktop().browse(URI(url)) }
        }
    }

    private fun open(onResult: (Result<ByteArray>?) -> Unit, extensions: List<String>?) {
        val dialog = FileDialog(null as Frame?, "Abrir", FileDialog.LOAD)
        if (extensions != null) {
            dialog.setFilenameFilter { _, name -> extensions.any { name.lowercase().endsWith(it) } }
        }
        dialog.isVisible = true
        val name = dialog.file
        if (name == null) {
            onResult(null)
            return
        }
        onResult(runCatching { File(dialog.directory, name).readBytes() })
    }

    private companion object {
        val IMAGE_EXTENSIONS = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp")
    }
}

@Composable
fun rememberPlatformUi(): PlatformUi = remember { DesktopPlatformUi() }
