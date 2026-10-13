package com.david7ce.sleepcycles.platform

import android.app.Activity
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.AlarmClock
import android.widget.Toast
import com.david7ce.sleepcycles.logic.ClockStyle
import com.david7ce.sleepcycles.shared.R
import com.david7ce.sleepcycles.ui.PlatformUi

/** The system time picker and the system clock app, no permission beyond SET_ALARM (normal, install-time). */
class AndroidPlatformUi(
    private val activity: Activity,
    private val noClockApp: () -> String,
) : PlatformUi {
    override val canPickTime = true

    /**
     * Analog is the clock dial; Digital is the spinner of scrolling numbers (styles.xml). Both are the
     * framework dialog, and the dial also has its own keyboard toggle for typing numbers.
     */
    override fun pickTime(
        initialMinutes: Int, is24Hour: Boolean, clockStyle: ClockStyle, dark: Boolean, onPicked: (Int) -> Unit,
    ) {
        val theme = when (clockStyle) {
            ClockStyle.Analog -> if (dark) R.style.SleepCyclesPickerClockDark else R.style.SleepCyclesPickerClockLight
            ClockStyle.Digital -> if (dark) R.style.SleepCyclesPickerSpinnerDark else R.style.SleepCyclesPickerSpinnerLight
        }
        TimePickerDialog(
            activity,
            theme,
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
