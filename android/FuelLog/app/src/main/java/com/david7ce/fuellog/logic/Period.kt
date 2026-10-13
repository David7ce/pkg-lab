package com.david7ce.fuellog.logic

import com.david7ce.fuellog.data.Refuel
import java.time.LocalDate
import java.time.ZoneId

/** History filter. "Last 3 months" = the current month plus the two before it. */
enum class Period {
    All, ThisMonth, Last3Months, ThisYear;

    /** Start of the period in epoch millis (inclusive), or null for no limit. */
    fun startMillis(today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Long? {
        val start = when (this) {
            All -> return null
            ThisMonth -> today.withDayOfMonth(1)
            Last3Months -> today.minusMonths(2).withDayOfMonth(1)
            ThisYear -> today.withDayOfYear(1)
        }
        return start.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun filter(records: List<Refuel>, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): List<Refuel> {
        val start = startMillis(today, zone) ?: return records
        return records.filter { it.timestamp >= start }
    }
}
