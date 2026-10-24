package com.david7ce.calistenia.platform

import android.app.Application
import android.app.Activity
import android.os.Bundle
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.david7ce.calistenia.data.CalisteniaRepository
import com.david7ce.calistenia.db.CalisteniaDatabase
import com.david7ce.calistenia.ui.AppState
import kotlinx.coroutines.MainScope
import java.util.Locale

/** Builds the state once per process; it outlives activity recreation (rotation, language change). */
class CalisteniaApplication : Application(), Application.ActivityLifecycleCallbacks {

    private var startedActivities = 0

    /** True while at least one activity is started: the rest signal is in-app when visible. */
    val isAppVisible: Boolean get() = startedActivities > 0

    val state: AppState by lazy {
        ensureRestChannel(this)
        val services = Services(
            clock = LocalClock,
            timeFormat = JvmTimeFormat(),
            restNotifier = AndroidRestNotifier(this, isAppVisible = { isAppVisible }),
        )
        // The driver opens the database lazily, on the first query (a background thread).
        val database = CalisteniaDatabase(AndroidSqliteDriver(CalisteniaDatabase.Schema, this, "calistenia.db"))
        val repository = CalisteniaRepository(database, services.clock, services.newId)
        val version = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        AppState(services, repository, MainScope(), Locale.getDefault().language, version)
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivities++
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities = (startedActivities - 1).coerceAtLeast(0)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
