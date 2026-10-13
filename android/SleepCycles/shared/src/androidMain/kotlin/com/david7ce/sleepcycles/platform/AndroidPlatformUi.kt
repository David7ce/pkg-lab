package com.david7ce.sleepcycles.platform

import android.app.Activity
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.AlarmClock
import android.widget.Toast
import com.david7ce.sleepcycles.ui.PlatformUi

/** The system time picker and the system clock app, no permission beyond SET_ALARM (normal, install-time). */
class AndroidPlatformUi(
    private val activity: Activity,
    private val noClockApp: () -> String,
) : PlatformUi {
    override val canPickTime = true

    override fun pickTime(initialMinutes: Int, is24Hour: Boolean, onPicked: (Int) -> Unit) {
        TimePickerDialog(
            activity,
            { _, hour, minute -> onPicked(hour * 60 + minute) },
            initialMinutes / 60,
            initialMinutes % 60,
            is24Hour,
        ).show()
    }

    override val canSaveAlarm = true

    override fun saveAlarm(minutesOfDay: Int, label: String) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR, minutesOfDay / 60)
            .putExtra(AlarmClock.EXTRA_MINUTES, minutesOfDay % 60)
            .putExtra(AlarmClock.EXTRA_MESSAGE, label)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, false) // the user confirms and saves it in the clock app
        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, noClockApp(), Toast.LENGTH_SHORT).show()
        }
    }
}
