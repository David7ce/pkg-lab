package com.deivon.fuellog.ui

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.deivon.fuellog.logic.UnitSystem
import java.util.Currency
import java.util.Locale

/** App preferences (not vehicle data): language, theme, currency and the selected vehicle. */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** "" = follow the system language. */
    var language by mutableStateOf(prefs.getString(KEY_LANG, "") ?: "")
        private set
    /** 0 = follow system, 1 = light, 2 = dark. */
    var theme by mutableIntStateOf(prefs.getInt("theme", 0))
        private set
    var currency by mutableStateOf(prefs.getString("currency", null) ?: defaultCurrency())
        private set
    var vehicleId by mutableLongStateOf(prefs.getLong("vehicle", -1))
        private set
    var units by mutableStateOf(UnitSystem.of(prefs.getString("units", null)))
        private set

    fun updateLanguage(value: String) { language = value; prefs.edit().putString(KEY_LANG, value).apply() }
    fun updateTheme(value: Int) { theme = value; prefs.edit().putInt("theme", value).apply() }
    fun updateVehicle(id: Long) { vehicleId = id; prefs.edit().putLong("vehicle", id).apply() }
    fun updateUnits(value: UnitSystem) { units = value; prefs.edit().putString("units", value.key).apply() }

    /** Returns false if [code] is not a valid ISO 4217 code (e.g. "EUR"). */
    fun updateCurrency(code: String): Boolean {
        val valid = runCatching { Currency.getInstance(code.uppercase()) }.getOrNull() ?: return false
        currency = valid.currencyCode
        prefs.edit().putString("currency", currency).apply()
        return true
    }

    private fun defaultCurrency() = runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrNull() ?: "EUR"

    companion object {
        private const val PREFS = "settings"
        private const val KEY_LANG = "lang"

        /** Language names in their own language, so anyone can find theirs. */
        val LANGUAGES = listOf(
            "en" to "English", "es" to "Español", "fr" to "Français", "de" to "Deutsch",
            "pt" to "Português", "zh" to "中文", "hi" to "हिन्दी", "ar" to "العربية",
        )

        /** Applies the chosen language to an Activity's context; "" keeps the system language. */
        fun wrap(base: Context): Context {
            val lang = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANG, "")
            if (lang.isNullOrEmpty()) {
                Locale.setDefault(Resources.getSystem().configuration.locales[0])
                return base
            }
            val locale = Locale.forLanguageTag(lang)
            Locale.setDefault(locale)
            return base.createConfigurationContext(Configuration(base.resources.configuration).apply { setLocale(locale) })
        }
    }
}
