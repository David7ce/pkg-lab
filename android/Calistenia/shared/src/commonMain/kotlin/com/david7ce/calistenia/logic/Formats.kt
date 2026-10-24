package com.david7ce.calistenia.logic

private fun Long.pad2(): String = toString().padStart(2, '0')

/** "5:03" under an hour, "1:05:03" above. Used for the running workout and history. */
fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "$hours:${minutes.pad2()}:${seconds.pad2()}" else "$minutes:${seconds.pad2()}"
}

/** "1:30" for a rest countdown; never shows a negative value. */
fun formatCountdown(millis: Long): String = formatDuration(millis.coerceAtLeast(0))

/** "8" or "30 s"; the suffix is translated by the caller. */
fun setValueLabel(reps: Int?, seconds: Int?, secondsUnit: String): String = when {
    reps != null -> reps.toString()
    seconds != null -> "$seconds $secondsUnit"
    else -> "—"
}

/** "3 × 8" or "3 × 30 s". */
fun targetLabel(sets: Int, reps: Int?, seconds: Int?, secondsUnit: String): String = when {
    reps != null -> "$sets × $reps"
    seconds != null -> "$sets × $seconds $secondsUnit"
    else -> "$sets"
}

/** Text of a numeric field: digits only, capped, so "-5" or "abc" can never reach the model. */
fun numberText(raw: String, maxDigits: Int): String = raw.filter { it.isDigit() }.take(maxDigits)

/** Parsed value of a numeric field; empty or zero is [fallback] (usually 0 = not set yet). */
fun numberOr(text: String, fallback: Int = 0): Int = text.toIntOrNull() ?: fallback
