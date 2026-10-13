package com.david7ce.sleepcycles.platform

import android.content.SharedPreferences

class PreferencesSettingsStore(private val prefs: SharedPreferences) : SettingsStore {
    override fun get(key: String): String? = prefs.getString(key, null)

    override fun put(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
}
