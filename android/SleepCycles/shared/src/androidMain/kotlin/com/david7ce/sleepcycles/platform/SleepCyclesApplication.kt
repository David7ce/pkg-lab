package com.david7ce.sleepcycles.platform

import android.app.Application
import com.david7ce.sleepcycles.ui.AppState
import java.util.Locale

/** Builds the state once per process; it outlives activity recreation (rotation, language change). */
class SleepCyclesApplication : Application() {
    val state: AppState by lazy {
        val services = Services(
            settings = PreferencesSettingsStore(getSharedPreferences("settings", MODE_PRIVATE)),
            clock = LocalClock,
        )
        val version = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        AppState(services, Locale.getDefault().language, version)
    }
}
