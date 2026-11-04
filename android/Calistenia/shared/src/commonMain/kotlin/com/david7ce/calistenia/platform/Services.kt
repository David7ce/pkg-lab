package com.david7ce.calistenia.platform

/** Local date/time text: java.time on both Android and desktop (JVM), localized. */
interface TimeFormat {
    /** "13 Oct 2026, 18:30" in the device locale. */
    fun dateTime(epochMillis: Long): String

    /** "13 Oct 2026" in the device locale. */
    fun shortDate(epochMillis: Long): String

    /** "18:30" in the device locale (24 h or AM/PM as the device prefers). */
    fun time(epochMillis: Long): String

    /** "2026-10-24", for file names and exports (not localized). Same local day as the calendar's `dayOf`. */
    fun isoDate(epochMillis: Long): String

    /** The zone's offset from UTC at [epochMillis], DST included; what `dayOf` needs to find the local day. */
    fun utcOffsetMillis(epochMillis: Long): Long
}

/**
 * The rest timer's "time is up" signal. On Android it posts a notification when the app is not in the
 * foreground (and vibrates/beeps when it is); on desktop the in-app timer is the only signal.
 * [canNotify] is false where there is no system notification to offer, so the settings hide it.
 */
interface RestNotifier {
    val canNotify: Boolean

    /** Replaces any pending signal. [durationMillis] from now. */
    fun schedule(durationMillis: Long, sound: Boolean, vibrate: Boolean)

    /** Cancels a pending signal (timer paused, skipped, or replaced). */
    fun cancel()
}

object NoRestNotifier : RestNotifier {
    override val canNotify = false
    override fun schedule(durationMillis: Long, sound: Boolean, vibrate: Boolean) = Unit
    override fun cancel() = Unit
}

/** Everything the shared code needs from the platform. */
class Services(
    /** Epoch millis, injectable so tests and screens can be deterministic. */
    val clock: () -> Long,
    val newId: () -> String = ::randomId,
    val timeFormat: TimeFormat,
    val restNotifier: RestNotifier = NoRestNotifier,
)

/** 128 random bits as a UUID-shaped string (version 4 layout). */
fun randomId(): String {
    val b = kotlin.random.Random.nextBytes(16)
    b[6] = ((b[6].toInt() and 0x0F) or 0x40).toByte()
    b[8] = ((b[8].toInt() and 0x3F) or 0x80).toByte()
    val hex = b.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}"
}
