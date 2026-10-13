package com.david7ce.sleepcycles.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.david7ce.sleepcycles.logic.AgeRange

/**
 * UI text. English is the base; each language overrides it (Translations.kt). Plain Kotlin instead
 * of a resources library: identical on every platform, and a missing translation falls back to
 * English instead of crashing. (Same approach as Recetario.)
 */
open class Strings {
    /** Separator in "7.5 h". */
    open val decimalSeparator = '.'

    // Main
    open val wakeUpAt = "Wake up at"
    open val goToBedAt = "Go to bed at"
    open val sleepNow = "Sleep now"
    open val resultsWake = "Go to bed at one of these times"
    open val resultsBed = "Wake up at one of these times"
    open fun hint(latency: Int) = "1 cycle = 90 min · includes $latency min to fall asleep"
    open fun cycles(n: Int, hours: String) = (if (n == 1) "1 cycle" else "$n cycles") + " · $hours h"
    open val hourLabel = "Hour"
    open val minuteLabel = "Min"
    open val am = "AM"
    open val pm = "PM"
    open val ratingGood = "Within the recommended range for your age"
    open val ratingOk = "Slightly outside the recommended range"
    open val ratingLow = "Far from the recommended range"

    // Settings
    open val settings = "Settings"
    open val ageRange = "Age range"
    protected open val ageChild = "Child (6–13) · 9–11 h"
    protected open val ageTeen = "Teen (14–17) · 8–10 h"
    protected open val ageAdult = "Adult (18–64) · 7–9 h"
    protected open val ageSenior = "Senior (65+) · 7–8 h"

    fun ageLabel(age: AgeRange): String = when (age) {
        AgeRange.Child -> ageChild
        AgeRange.Teen -> ageTeen
        AgeRange.Adult -> ageAdult
        AgeRange.Senior -> ageSenior
    }

    open val fallAsleepTime = "Time to fall asleep (minutes)"
    open val timeFormat = "Time format"
    open val format24 = "24-hour"
    open val format12 = "12-hour (AM/PM)"
    open val clockType = "Type of clock"
    open val clockAnalog = "Analog"
    open val clockDigital = "Digital"
    open val theme = "Theme"
    open val themeSystem = "System"
    open val themeLight = "Light"
    open val themeDark = "Dark"
    open val language = "Language"
    open val languageSystem = "System default"
    open fun versionLabel(version: String) = "Version $version"

    // Help
    open val howItWorks = "How does it work?"

    /** **Double asterisks** mark bold runs (see splitBold). */
    open val howItWorksParagraphs = listOf(
        "Sleep isn't uniform — it moves through repeating **cycles of roughly 90 minutes**, each passing " +
            "through light sleep, deep sleep, and REM (when most dreaming happens).",
        "Waking up **in the middle of a cycle** — especially during deep sleep — causes sleep inertia: that " +
            "groggy, heavy feeling that can linger for a while. Waking **between cycles**, when sleep is " +
            "lightest, makes getting up much easier.",
        "This calculator counts backward or forward in 90-minute steps, plus the time it takes you to fall " +
            "asleep (about **15 minutes** on average — adjustable in settings), so your alarm lands at the end " +
            "of a cycle instead of the middle.",
        "How many cycles you need depends on age. The National Sleep Foundation recommends **7–9 hours** " +
            "(about 5–6 cycles) for adults, more for teens and children, slightly less for seniors. Each card " +
            "has a face colored by how close that option is to your recommended range.",
    )
    open val howItWorksNote =
        "90 minutes is an average — real cycles range from about 70 to 120 minutes and vary night to night. " +
            "Treat the suggestions as good targets, not exact science."
    open val gotIt = "Got it"
    open val done = "Done"

    // Android alarm and clock
    open val alarmHint = "Tap a time to save it as an alarm in your clock app"
    open val pickTime = "Pick the time with the clock"
    open val noClockApp = "No clock app found"

    // Shared components (android/shared)
    open val back = "Back"
    open val close = "Close"
    open val cancel = "Cancel"
    open val delete = "Delete"
    open val selectAll = "Select all"
}

val LocalStrings = staticCompositionLocalOf<Strings> { EnStrings }
