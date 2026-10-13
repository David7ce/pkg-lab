package com.david7ce.sleepcycles.logic

import com.david7ce.shared.ui.ThemeMode
import com.david7ce.sleepcycles.platform.SettingsStore

/** Look of the system time picker on Android: [Analog] is the clock dial, [Digital] the scrolling numbers. */
enum class ClockStyle { Analog, Digital }

/** Languages the app offers. [code] is the ISO 639 code; System follows the device. Names are shown in their own language. */
enum class AppLanguage(val code: String?, val nativeName: String) {
    System(null, ""),
    Es("es", "Español"),
    En("en", "English"),
    Fr("fr", "Français"),
    De("de", "Deutsch"),
    Pt("pt", "Português"),
    It("it", "Italiano"),
}

/** Everything that survives a restart. Defaults are the PWA defaults. */
data class UserSettings(
    val mode: Mode = Mode.Wake,
    val wakeTime: Int = 7 * 60,
    val bedTime: Int = 23 * 60,
    val age: AgeRange = AgeRange.Adult,
    val latencyMinutes: Int = DEFAULT_LATENCY_MINUTES,
    val format: ClockFormat = ClockFormat.H24,
    val theme: ThemeMode = ThemeMode.System,
    val language: AppLanguage = AppLanguage.System,
    val clockStyle: ClockStyle = ClockStyle.Analog,
)

private object Key {
    const val MODE = "mode"
    const val WAKE = "wakeTime"
    const val BED = "bedTime"
    const val AGE = "age"
    const val LATENCY = "latency"
    const val FORMAT = "format"
    const val THEME = "theme"
    const val LANGUAGE = "language"
    const val CLOCK_STYLE = "clockStyle"
}

private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default

private fun timeOr(text: String?, default: Int): Int =
    text?.toIntOrNull()?.takeIf { it in 0 until MINUTES_PER_DAY } ?: default

/** Each field falls back to its default on its own, so one corrupt value never loses the rest. */
fun SettingsStore.load(): UserSettings {
    val d = UserSettings()
    return UserSettings(
        mode = enumOr(get(Key.MODE), d.mode),
        wakeTime = timeOr(get(Key.WAKE), d.wakeTime),
        bedTime = timeOr(get(Key.BED), d.bedTime),
        age = enumOr(get(Key.AGE), d.age),
        latencyMinutes = get(Key.LATENCY)?.toIntOrNull()?.let(::clampLatency) ?: d.latencyMinutes,
        format = enumOr(get(Key.FORMAT), d.format),
        theme = enumOr(get(Key.THEME), d.theme),
        language = enumOr(get(Key.LANGUAGE), d.language),
        clockStyle = enumOr(get(Key.CLOCK_STYLE), d.clockStyle),
    )
}

fun SettingsStore.save(settings: UserSettings) {
    put(Key.MODE, settings.mode.name)
    put(Key.WAKE, settings.wakeTime.toString())
    put(Key.BED, settings.bedTime.toString())
    put(Key.AGE, settings.age.name)
    put(Key.LATENCY, settings.latencyMinutes.toString())
    put(Key.FORMAT, settings.format.name)
    put(Key.THEME, settings.theme.name)
    put(Key.LANGUAGE, settings.language.name)
    put(Key.CLOCK_STYLE, settings.clockStyle.name)
}

/**
 * The first language of the device list (most preferred first) that the app translates; otherwise the
 * first one, which then falls back to English. A device set to Japanese then Spanish gets Spanish.
 */
fun pickDeviceLanguage(deviceLanguages: List<String>): String {
    val supported = AppLanguage.entries.mapNotNull { it.code }
    return deviceLanguages.firstOrNull { it.lowercase().take(2) in supported } ?: deviceLanguages.firstOrNull().orEmpty()
}
