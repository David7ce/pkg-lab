package com.david7ce.calistenia.platform

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Localized date and time with java.time. Both targets are JVM, so one implementation covers
 * Android and desktop; the locale comes from the device (or the app's language, when the caller
 * wants it) and the zone from the system.
 */
class JvmTimeFormat(
    locale: Locale = Locale.getDefault(),
    private val zone: ZoneId = ZoneId.systemDefault(),
) : TimeFormat {
    private val date: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    private val time: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)

    private fun local(epochMillis: Long) = Instant.ofEpochMilli(epochMillis).atZone(zone)

    override fun dateTime(epochMillis: Long): String {
        val value = local(epochMillis)
        return "${date.format(value)}, ${time.format(value)}"
    }

    override fun shortDate(epochMillis: Long): String = date.format(local(epochMillis))

    override fun time(epochMillis: Long): String = time.format(local(epochMillis))

    override fun isoDate(epochMillis: Long): String = local(epochMillis).toLocalDate().toString()
}
