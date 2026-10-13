package com.david7ce.sleepcycles.platform

/** String key/value persistence: SharedPreferences on Android, java.util.prefs on desktop. */
interface SettingsStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
}

/** Current time of day; an interface so tests (and Sleep now) are deterministic. */
interface Clock {
    fun minutesOfDay(): Int
}

class Services(val settings: SettingsStore, val clock: Clock)
