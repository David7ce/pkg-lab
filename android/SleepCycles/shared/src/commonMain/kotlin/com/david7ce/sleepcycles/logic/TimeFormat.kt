package com.david7ce.sleepcycles.logic

enum class ClockFormat { H24, H12 }

/** What the hour and minute fields show. [pm] is only meaningful in 12-hour format. */
data class TimeParts(val hour: String, val minute: String, val pm: Boolean)

private fun Int.pad2() = toString().padStart(2, '0')

fun timeParts(minutesOfDay: Int, format: ClockFormat): TimeParts {
    val m = wrapDay(minutesOfDay)
    val hour24 = m / 60
    val shown = if (format == ClockFormat.H24) hour24 else (hour24 % 12).let { if (it == 0) 12 else it }
    return TimeParts(shown.pad2(), (m % 60).pad2(), pm = hour24 >= 12)
}

/** Minutes of the day, or null while the text is not a complete, valid time (the caller keeps the last valid one). */
fun parseTime(hourText: String, minuteText: String, pm: Boolean, format: ClockFormat): Int? {
    val h = hourText.toIntOrNull() ?: return null
    val m = minuteText.toIntOrNull() ?: return null
    if (m !in 0..59) return null
    val hour24 = when (format) {
        ClockFormat.H24 -> if (h in 0..23) h else return null
        ClockFormat.H12 -> if (h in 1..12) (h % 12) + (if (pm) 12 else 0) else return null
    }
    return hour24 * 60 + m
}

fun formatTime(minutesOfDay: Int, format: ClockFormat, am: String = "AM", pm: String = "PM"): String {
    val p = timeParts(minutesOfDay, format)
    return if (format == ClockFormat.H24) "${p.hour}:${p.minute}"
    else "${p.hour.trimStart('0')}:${p.minute} ${if (p.pm) pm else am}"
}

/** Whole or half hours ("9", "7,5"); every cycle count is a multiple of 30 minutes. */
fun formatHours(minutes: Int, separator: Char): String {
    val whole = minutes / 60
    return if (minutes % 60 == 0) "$whole" else "$whole${separator}5"
}

/** The PWA field: empty, negative or non-numeric counts as 0; above its maximum is capped. */
fun parseLatency(text: String): Int =
    text.trim().toLongOrNull()?.coerceIn(0L, MAX_LATENCY_MINUTES.toLong())?.toInt() ?: 0

/**
 * What the latency field shows after an edit, so the text always matches the value in use: "500" becomes
 * "120", a pasted "-5" becomes "0", and letters are dropped. Empty stays empty (it counts as 0).
 */
fun latencyText(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(3)
    return when {
        raw.trim().startsWith("-") -> "0"
        digits.isEmpty() -> ""
        else -> parseLatency(digits).toString()
    }
}
