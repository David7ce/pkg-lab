package com.david7ce.calistenia.logic

import com.david7ce.shared.ui.ThemeMode

/** Everything that survives a restart, stored in the `setting` table. */
data class AppSettings(
    val theme: ThemeMode = ThemeMode.System,
    val language: AppLanguage = AppLanguage.System,
    val defaultRestSeconds: Int = DEFAULT_REST_SECONDS,
    val restNotifications: Boolean = true,
    val restSound: Boolean = true,
    val restVibrate: Boolean = true,
    val keepScreenOn: Boolean = true,
    val favoriteExerciseIds: Set<String> = emptySet(),
)

const val DEFAULT_REST_SECONDS = 90

/** The rest choices offered in Settings, in seconds. */
val REST_OPTIONS = listOf(30, 60, 90, 120, 180)

fun clampRestSeconds(value: Int): Int = value.coerceIn(0, 600)

object SettingsKey {
    const val THEME = "theme"
    const val LANGUAGE = "language"
    const val DEFAULT_REST = "defaultRestSeconds"
    const val REST_NOTIFICATIONS = "restNotifications"
    const val REST_SOUND = "restSound"
    const val REST_VIBRATE = "restVibrate"
    const val KEEP_SCREEN_ON = "keepScreenOn"
    const val FAVORITE_EXERCISES = "favoriteExerciseIds"
}

/** Each field falls back to its default on its own, so one corrupt value never loses the rest. */
fun settingsFrom(stored: Map<String, String>): AppSettings {
    val d = AppSettings()
    fun bool(key: String, default: Boolean) = when (stored[key]) {
        "true" -> true
        "false" -> false
        else -> default
    }
    return AppSettings(
        theme = ThemeMode.entries.firstOrNull { it.name == stored[SettingsKey.THEME] } ?: d.theme,
        language = AppLanguage.entries.firstOrNull { it.name == stored[SettingsKey.LANGUAGE] } ?: d.language,
        defaultRestSeconds = stored[SettingsKey.DEFAULT_REST]?.toIntOrNull()
            ?.let(::clampRestSeconds) ?: d.defaultRestSeconds,
        restNotifications = bool(SettingsKey.REST_NOTIFICATIONS, d.restNotifications),
        restSound = bool(SettingsKey.REST_SOUND, d.restSound),
        restVibrate = bool(SettingsKey.REST_VIBRATE, d.restVibrate),
        keepScreenOn = bool(SettingsKey.KEEP_SCREEN_ON, d.keepScreenOn),
        favoriteExerciseIds = stored[SettingsKey.FAVORITE_EXERCISES]
            ?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: d.favoriteExerciseIds,
    )
}

fun AppSettings.toStored(): Map<String, String> = mapOf(
    SettingsKey.THEME to theme.name,
    SettingsKey.LANGUAGE to language.name,
    SettingsKey.DEFAULT_REST to defaultRestSeconds.toString(),
    SettingsKey.REST_NOTIFICATIONS to restNotifications.toString(),
    SettingsKey.REST_SOUND to restSound.toString(),
    SettingsKey.REST_VIBRATE to restVibrate.toString(),
    SettingsKey.KEEP_SCREEN_ON to keepScreenOn.toString(),
    SettingsKey.FAVORITE_EXERCISES to favoriteExerciseIds.joinToString(","),
)
