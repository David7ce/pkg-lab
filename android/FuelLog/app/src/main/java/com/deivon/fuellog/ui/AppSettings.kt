package com.deivon.fuellog.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Currency
import java.util.Locale

/** App preferences (not vehicle data): theme and display currency. */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** 0 = follow system, 1 = light, 2 = dark. */
    var theme by mutableIntStateOf(prefs.getInt("theme", 0))
        private set
    var currency by mutableStateOf(prefs.getString("currency", null) ?: defaultCurrency())
        private set

    fun updateTheme(value: Int) { theme = value; prefs.edit().putInt("theme", value).apply() }

    /** Returns false if [code] is not a valid ISO 4217 code (e.g. "EUR"). */
    fun updateCurrency(code: String): Boolean {
        val valid = runCatching { Currency.getInstance(code.uppercase()) }.getOrNull() ?: return false
        currency = valid.currencyCode
        prefs.edit().putString("currency", currency).apply()
        return true
    }

    private fun defaultCurrency() = runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrNull() ?: "EUR"
}
