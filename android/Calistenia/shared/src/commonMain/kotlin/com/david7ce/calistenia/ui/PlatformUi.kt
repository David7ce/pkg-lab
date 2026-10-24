package com.david7ce.calistenia.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * What only the operating system can do. Each platform provides its own; where a capability does not
 * exist (desktop has no rest notification, no keep-screen-on), the settings hide the control instead
 * of showing a dead switch.
 */
interface PlatformUi {
    fun openFile(onResult: (Result<ByteArray>?) -> Unit)
    fun saveFile(fileName: String, mimeType: String, bytes: ByteArray, onResult: (Boolean) -> Unit)

    /** The OS can post the rest notification. */
    val notificationsAvailable: Boolean

    /** Current permission state; Compose observes it on Android. */
    val notificationsGranted: Boolean

    /** Asks the OS once (Android 13+); a no-op elsewhere. */
    fun requestNotificationPermission()

    /** The window can be kept awake during a workout (Android). */
    val canKeepScreenOn: Boolean
}

object NoPlatformUi : PlatformUi {
    override fun openFile(onResult: (Result<ByteArray>?) -> Unit) = onResult(null)
    override fun saveFile(fileName: String, mimeType: String, bytes: ByteArray, onResult: (Boolean) -> Unit) = Unit
    override val notificationsAvailable = false
    override val notificationsGranted = false
    override fun requestNotificationPermission() = Unit
    override val canKeepScreenOn = false
}

val LocalPlatform = staticCompositionLocalOf<PlatformUi> { NoPlatformUi }
