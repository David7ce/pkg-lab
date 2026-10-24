package com.david7ce.calistenia.platform

/** The wall clock, injectable so tests and previews can be deterministic. */
object LocalClock : () -> Long {
    override fun invoke(): Long = System.currentTimeMillis()
}
