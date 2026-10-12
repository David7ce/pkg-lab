package com.deivon.vozreves

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/** Ajustes guardados en SharedPreferences y expuestos como estado de Compose. */
class Settings(ctx: Context) {
    private val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var language by mutableStateOf(prefs.getString(KEY_LANG, "") ?: "")
        private set
    var theme by mutableIntStateOf(prefs.getInt("theme", 0)) // 0 sistema, 1 claro, 2 oscuro
        private set
    var countdown by mutableIntStateOf(prefs.getInt("countdown", 3))
        private set
    var maxSeconds by mutableIntStateOf(prefs.getInt("max", 60))
        private set

    fun updateLanguage(v: String) { language = v; prefs.edit().putString(KEY_LANG, v).apply() }
    fun updateTheme(v: Int) { theme = v; prefs.edit().putInt("theme", v).apply() }
    fun updateCountdown(v: Int) { countdown = v; prefs.edit().putInt("countdown", v).apply() }
    fun updateMaxSeconds(v: Int) { maxSeconds = v; prefs.edit().putInt("max", v).apply() }

    companion object {
        private const val PREFS = "settings"
        private const val KEY_LANG = "lang"

        /** "" = idioma del sistema (detección automática). Nombres en su propio idioma. */
        val LANGUAGES = listOf(
            "en" to "English", "es" to "Español", "zh" to "中文",
            "hi" to "हिन्दी", "ar" to "العربية"
        )

        /** Aplica el idioma elegido a la Activity; si es "", Android usa el del sistema. */
        fun wrap(base: Context): Context {
            val lang = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANG, "")
            if (lang.isNullOrEmpty()) return base
            val locale = Locale.forLanguageTag(lang)
            Locale.setDefault(locale)
            val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
            return base.createConfigurationContext(config)
        }
    }
}
