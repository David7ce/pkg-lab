@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.deivon.fuellog.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.deivon.fuellog.R
import com.deivon.fuellog.data.Refuel
import com.deivon.fuellog.logic.FuelStats
import com.deivon.fuellog.logic.Period

private val periodLabels = mapOf(
    Period.All to R.string.period_all, Period.ThisMonth to R.string.period_month,
    Period.Last3Months to R.string.period_3months, Period.ThisYear to R.string.period_year,
)

@Composable
fun HistoryScreen(vm: AppViewModel, formats: Formats, modifier: Modifier, onOpen: (Refuel) -> Unit) {
    var period by rememberSaveable { mutableStateOf(Period.All) }
    val records = remember(vm.refuels, period) { period.filter(vm.refuels) }
    val summary = vm.summary
    val periodConsumption = remember(records, summary) {
        FuelStats.averageConsumption(records.mapNotNull { summary.byEndId[it.id] })
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Period.entries.forEach { p ->
                FilterChip(selected = period == p, onClick = { period = p }, label = { Text(stringResource(periodLabels.getValue(p))) })
            }
        }
        val totals = listOfNotNull(
            formats.money(FuelStats.spentCents(records)),
            formats.litres(FuelStats.volumeMl(records)),
            periodConsumption?.let { formats.consumption(it) },
        ).joinToString(" · ")
        Text(
            stringResource(R.string.period_summary, records.size, totals),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (records.isEmpty() && vm.loaded) {
            Text(stringResource(R.string.empty_history), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
            items(records, key = { it.id }) { r ->
                RefuelRow(r, summary.byEndId[r.id], formats) { onOpen(r) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
