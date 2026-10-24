package com.david7ce.calistenia.platform

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.david7ce.calistenia.shared.R

private const val REST_CHANNEL_ID = "rest_timer"
private const val REST_CHANNEL_SILENT_ID = "rest_timer_silent"
private const val REST_NOTIFICATION_ID = 1001

/**
 * Two channels, created once: one with sound and vibration, one silent. Android channels cannot
 * change their sound/vibration after creation, so the notification picks the channel that matches
 * the settings (both off = silent; either on = the alert channel).
 */
fun ensureRestChannel(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    val alert = NotificationChannel(
        REST_CHANNEL_ID,
        context.getString(R.string.rest_channel_name),
        NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        description = context.getString(R.string.rest_channel_description)
        enableVibration(true)
        setShowBadge(false)
    }
    val silent = NotificationChannel(
        REST_CHANNEL_SILENT_ID,
        context.getString(R.string.rest_channel_silent_name),
        NotificationManager.IMPORTANCE_LOW,
    ).apply {
        description = context.getString(R.string.rest_channel_description)
        setSound(null, null)
        enableVibration(false)
        setShowBadge(false)
    }
    manager.createNotificationChannels(listOf(alert, silent))
}

/**
 * The rest timer's signal. A delayed job lives as long as the process, so it fires while the app is
 * in the background; if the process is killed, it does not fire at all (documented: this is a training
 * aid, not an alarm). When the app is visible it vibrates/beeps instead of posting a notification.
 */
class AndroidRestNotifier(
    private val context: Context,
    private val isAppVisible: () -> Boolean,
) : RestNotifier {
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null

    override val canNotify = true

    override fun schedule(durationMillis: Long, sound: Boolean, vibrate: Boolean) {
        cancel()
        val runnable = Runnable {
            pending = null
            signal(sound, vibrate)
        }
        pending = runnable
        handler.postDelayed(runnable, durationMillis.coerceAtLeast(0))
    }

    override fun cancel() {
        pending?.let(handler::removeCallbacks)
        pending = null
    }

    private fun signal(sound: Boolean, vibrate: Boolean) {
        if (isAppVisible()) {
            if (vibrate) runCatching { vibrateNow() }
            if (sound) runCatching { playSound() }
        } else {
            runCatching { postNotification(sound, vibrate) }
        }
    }

    private fun postNotification(sound: Boolean, vibrate: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val channel = if (sound || vibrate) REST_CHANNEL_ID else REST_CHANNEL_SILENT_ID
        val notification = Notification.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_rest)
            .setContentTitle(context.getString(R.string.rest_notification_title))
            .setContentText(context.getString(R.string.rest_notification_text))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)?.notify(REST_NOTIFICATION_ID, notification)
    }

    private fun vibrateNow() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        vibrator?.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun playSound() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        RingtoneManager.getRingtone(context, uri)?.play()
    }
}

/** Runtime permission state for the rest notification (Android 13+); true below 13. */
fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
