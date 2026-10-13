package com.david7ce.sleepcycles.platform

import java.time.LocalTime

/** The device wall clock; shared by Android and desktop. */
object LocalClock : Clock {
    override fun minutesOfDay(): Int = LocalTime.now().let { it.hour * 60 + it.minute }
}
