package com.david7ce.vozreves

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/** Tiempos con el separador decimal y los dígitos del idioma (3.4 / 3,4 / ٣٫٤). */
class TimeFormat(val locale: Locale) {
    private fun decimals(intDigits: Int) = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
        minimumIntegerDigits = intDigits
        isGroupingUsed = false
    }
    private val sec = decimals(1)
    private val sec2 = decimals(2)
    private val int = NumberFormat.getIntegerInstance(locale)

    /** 3,4 */
    fun seconds(s: Float): String = sec.format(max(0f, s).toDouble())

    /** 0:03,4 — redondeado a décimas antes de partir en minutos (evita "0:60,0"). */
    fun clock(s: Float): String {
        val tenths = (max(0f, s) * 10).roundToInt()
        return int.format(tenths / 600) + ":" + sec2.format((tenths % 600) / 10.0)
    }
}

@Composable
fun rememberTimeFormat(): TimeFormat {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { TimeFormat(locale) }
}
