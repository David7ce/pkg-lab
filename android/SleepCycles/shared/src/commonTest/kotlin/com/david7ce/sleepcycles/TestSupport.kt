package com.david7ce.sleepcycles

import com.david7ce.sleepcycles.platform.Clock
import com.david7ce.sleepcycles.platform.SettingsStore

class MapStore(val values: MutableMap<String, String> = mutableMapOf()) : SettingsStore {
    override fun get(key: String): String? = values[key]
    override fun put(key: String, value: String) { values[key] = value }
}

class FakeClock(var minutes: Int = 0) : Clock {
    override fun minutesOfDay(): Int = minutes
}
