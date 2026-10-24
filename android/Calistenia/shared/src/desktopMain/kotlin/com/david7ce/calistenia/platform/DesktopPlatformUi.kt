package com.david7ce.calistenia.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.david7ce.calistenia.ui.PlatformUi
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * The system's own file dialogs (Explorer on Windows, GTK on most Linux desktops). Each dialog is
 * modal, so it blocks until the user chooses or cancels. No notifications and no keep-screen-on here:
 * the settings hide those controls.
 */
class DesktopPlatformUi : PlatformUi {
    override fun openFile(onResult: (Result<ByteArray>?) -> Unit) {
        val dialog = FileDialog(null as Frame?, "Open", FileDialog.LOAD)
        dialog.setFilenameFilter { _, name -> name.lowercase().endsWith(".json") }
        dialog.isVisible = true
        val name = dialog.file
        if (name == null) {
            onResult(null)
            return
        }
        onResult(runCatching { File(dialog.directory, name).readBytes() })
    }

    override fun saveFile(fileName: String, mimeType: String, bytes: ByteArray, onResult: (Boolean) -> Unit) {
        val dialog = FileDialog(null as Frame?, "Save", FileDialog.SAVE)
        dialog.file = fileName
        dialog.isVisible = true
        val name = dialog.file ?: return // cancelled: nothing to report, same as Android
        onResult(runCatching { File(dialog.directory, name).writeBytes(bytes) }.isSuccess)
    }

    override val notificationsAvailable = false
    override val notificationsGranted = false
    override fun requestNotificationPermission() = Unit
    override val canKeepScreenOn = false
}

@Composable
fun rememberPlatformUi(): PlatformUi = remember { DesktopPlatformUi() }
