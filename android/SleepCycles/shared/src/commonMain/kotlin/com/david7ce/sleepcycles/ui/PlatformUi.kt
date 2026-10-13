package com.david7ce.sleepcycles.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * What only the operating system can do. Each platform provides its own; where a capability does not
 * exist (desktop has no clock-app alarm), the UI hides the control instead of showing a dead button.
 */
interface PlatformUi {
    /** The OS offers its own clock-style time picker. */
    val canPickTime: Boolean

    /** Shows the OS time picker; [onPicked] receives minutes of the day. */
    fun pickTime(initialMinutes: Int, is24Hour: Boolean, onPicked: (Int) -> Unit)

    /** An alarm can be handed to the OS clock app. */
    val canSaveAlarm: Boolean

    /** Opens the clock app on a new alarm at [minutesOfDay], to be saved by the user there. */
    fun saveAlarm(minutesOfDay: Int, label: String)
}

object NoPlatformUi : PlatformUi {
    override val canPickTime = false
    override fun pickTime(initialMinutes: Int, is24Hour: Boolean, onPicked: (Int) -> Unit) = Unit
    override val canSaveAlarm = false
    override fun saveAlarm(minutesOfDay: Int, label: String) = Unit
}

val LocalPlatform = staticCompositionLocalOf<PlatformUi> { NoPlatformUi }
