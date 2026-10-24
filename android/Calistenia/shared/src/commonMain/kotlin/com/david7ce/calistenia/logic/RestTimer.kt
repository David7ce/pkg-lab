package com.david7ce.calistenia.logic

/**
 * The rest timer, as plain data so it can be tested and survive recomposition. Running is
 * represented by an absolute end time (wall clock from [Services.clock]): recomposition and the
 * 200 ms UI tick can never make it drift or count twice.
 */
data class RestState(
    val totalMillis: Long,
    /** What is left while paused. Ignored while running. */
    val remainingWhenPaused: Long,
    /** Non-null while running. */
    val endAtMillis: Long?,
    val finished: Boolean = false,
) {
    val running: Boolean get() = endAtMillis != null && !finished

    fun remaining(now: Long): Long =
        if (running) (endAtMillis!! - now).coerceAtLeast(0) else remainingWhenPaused.coerceAtLeast(0)

    /** 0..1, for a progress ring. */
    fun progress(now: Long): Float {
        if (totalMillis <= 0) return 0f
        return (1f - remaining(now).toFloat() / totalMillis).coerceIn(0f, 1f)
    }
}

fun startRest(seconds: Int, now: Long): RestState? {
    if (seconds <= 0) return null
    val total = seconds * 1000L
    return RestState(totalMillis = total, remainingWhenPaused = total, endAtMillis = now + total)
}

fun RestState.pause(now: Long): RestState {
    if (!running) return this
    return copy(remainingWhenPaused = remaining(now), endAtMillis = null)
}

fun RestState.resume(now: Long): RestState {
    if (running || finished) return this
    return copy(endAtMillis = now + remainingWhenPaused)
}

fun RestState.addMillis(extraMillis: Long, now: Long): RestState {
    val extra = extraMillis.coerceAtLeast(0)
    if (finished || extra == 0L) return this
    return if (running) {
        copy(totalMillis = totalMillis + extra, endAtMillis = endAtMillis!! + extra)
    } else {
        copy(totalMillis = totalMillis + extra, remainingWhenPaused = remainingWhenPaused + extra)
    }
}

/** One UI tick. Reaching zero marks it finished instead of letting it count into negatives. */
fun RestState.tick(now: Long): RestState {
    if (!running) return this
    val left = remaining(now)
    return if (left <= 0) copy(remainingWhenPaused = 0, endAtMillis = null, finished = true) else this
}

fun RestState.skip(): RestState = copy(remainingWhenPaused = 0, endAtMillis = null, finished = true)
