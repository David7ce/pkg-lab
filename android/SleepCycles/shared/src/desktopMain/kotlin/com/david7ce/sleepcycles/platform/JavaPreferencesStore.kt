package com.david7ce.sleepcycles.platform

import java.util.prefs.Preferences

/** Registry on Windows, ~/.java on Linux, plist on macOS: whatever the OS considers user settings. */
class JavaPreferencesStore(
    private val node: Preferences = Preferences.userRoot().node("com/david7ce/sleepcycles"),
) : SettingsStore {
    override fun get(key: String): String? = node.get(key, null)

    override fun put(key: String, value: String) {
        node.put(key, value)
        runCatching { node.flush() } // an unwritable backing store must never crash the app
    }
}
