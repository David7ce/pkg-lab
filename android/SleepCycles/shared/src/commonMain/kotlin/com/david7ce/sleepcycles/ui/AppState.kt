package com.david7ce.sleepcycles.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.david7ce.sleepcycles.logic.AgeRange
import com.david7ce.sleepcycles.logic.AppLanguage
import com.david7ce.sleepcycles.logic.ClockFormat
import com.david7ce.sleepcycles.logic.Mode
import com.david7ce.sleepcycles.logic.Suggestion
import com.david7ce.sleepcycles.logic.ThemeMode
import com.david7ce.sleepcycles.logic.UserSettings
import com.david7ce.sleepcycles.logic.clampLatency
import com.david7ce.sleepcycles.logic.load
import com.david7ce.sleepcycles.logic.save
import com.david7ce.sleepcycles.logic.suggestions as calculateSuggestions // the property below has the same name
import com.david7ce.sleepcycles.platform.Services

sealed interface Screen {
    data object Main : Screen
    data object Settings : Screen
}

/**
 * All UI state. Every setter persists immediately: the data is tiny and the app can be killed at any time.
 * The Android Application owns one instance so it outlives activity recreation.
 */
class AppState(
    private val services: Services,
    systemLanguage: String,
    val appVersion: String,
) {
    private val saved = services.settings.load()

    var mode by mutableStateOf(saved.mode)
        private set
    var wakeTime by mutableStateOf(saved.wakeTime)
        private set
    var bedTime by mutableStateOf(saved.bedTime)
        private set
    var age by mutableStateOf(saved.age)
        private set
    var latencyMinutes by mutableStateOf(saved.latencyMinutes)
        private set
    var format by mutableStateOf(saved.format)
        private set
    var theme by mutableStateOf(saved.theme)
        private set
    var language by mutableStateOf(saved.language)
        private set

    /** The device language (ISO 639 code), updated by the platform; used when [language] is System. */
    var systemLanguage by mutableStateOf(systemLanguage)

    /** Texts in the chosen language, or the device language when the choice is System. */
    val strings: Strings get() = stringsFor(language.code ?: systemLanguage)

    var screen by mutableStateOf<Screen>(Screen.Main)
        private set
    var showHelp by mutableStateOf(false)
        private set

    /** The time the user fixes: the wake-up time in Wake mode, the bedtime in Bed mode. */
    val anchor: Int get() = if (mode == Mode.Wake) wakeTime else bedTime
    val suggestions: List<Suggestion> get() = calculateSuggestions(mode, anchor, latencyMinutes, age)
    val canGoBack: Boolean get() = showHelp || screen != Screen.Main

    fun setMode(value: Mode) {
        mode = value
        persist()
    }

    fun setAnchor(minutes: Int) {
        if (mode == Mode.Wake) wakeTime = minutes else bedTime = minutes
        persist()
    }

    /** Same as the PWA Sleep now: bed mode at the current time. */
    fun sleepNow() {
        mode = Mode.Bed
        bedTime = services.clock.minutesOfDay()
        persist()
    }

    fun setAge(value: AgeRange) {
        age = value
        persist()
    }

    fun setLatency(minutes: Int) {
        latencyMinutes = clampLatency(minutes)
        persist()
    }

    fun setFormat(value: ClockFormat) {
        format = value
        persist()
    }

    fun setTheme(value: ThemeMode) {
        theme = value
        persist()
    }

    fun setLanguage(value: AppLanguage) {
        language = value
        persist()
    }

    fun openSettings() {
        screen = Screen.Settings
    }

    fun openHelp() {
        showHelp = true
    }

    fun closeHelp() {
        showHelp = false
    }

    /** Closes the help dialog first, then leaves Settings. */
    fun back() {
        if (showHelp) showHelp = false else if (screen != Screen.Main) screen = Screen.Main
    }

    private fun persist() {
        services.settings.save(
            UserSettings(mode, wakeTime, bedTime, age, latencyMinutes, format, theme, language)
        )
    }
}
