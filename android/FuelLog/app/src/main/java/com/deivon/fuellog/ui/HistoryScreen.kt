@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.deivon.fuellog.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.deivon.fuellog.R
import com.deivon.fuellog.data.Refuel
import com.deivon.fuellog.logic.FuelStats
import com.deivon.fuellog.logic.Period
import com.deivon.shared.ui.ConfirmDialog
import com.deivon.shared.ui.SelectionActions
import com.deivon.shared.ui.rememberSelection

private val periodLabels = mapOf(
    Period.All to R.string.period_all, Period.ThisMonth to R.string.period_month,
    Period.Last3Months to R.string.period_3months, Period.ThisYear to R.string.period_year,
)

/**
 * Refuellings grouped by month (newest first) with a sticky month header and its total.
 * Long press selects several; deleting always asks first.
 */
@Composable
fun HistoryScreen(vm: AppViewModel, formats: Formats, modifier: Modifier, onOpen: (Refuel) -> Unit) {
    var period by rememberSaveable { mutableStateOf(Period.All) }
    val records = remember(vm.refuels, period) { period.filter(vm.refuels) }
    val months = remember(records) { records.groupBy { Formats.yearMonth(it.timestamp) }.toList() }
    val summary = vm.summary
    val periodConsumption = remember(records, summary) {
        FuelStats.averageConsumption(records.mapNotNull { summary.byEndId[it.id] })
    }

    val sel = rememberSelection<Long>()
    val visibleIds = records.map { it.id }
    LaunchedEffect(visibleIds) { sel.retain(visibleIds) }
    BackHandler(enabled = sel.active) { sel.clear() }
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) ConfirmDialog(
        text = if (sel.keys.size == 1) stringResource(R.string.delete_refuel_confirm)
               else stringResource(R.string.delete_refuels_confirm, sel.keys.size),
        confirmLabel = stringResource(R.string.delete),
        destructive = true,
        onConfirm = { vm.deleteRefuels(sel.keys); sel.clear(); confirmDelete = false },
        onDismiss = { confirmDelete = false },
    )

    Column(modifier.fillMaxSize()) {
        if (sel.active) {
            SelectionBar(
                count = sel.keys.size,
                allSelected = sel.keys.size >= visibleIds.size,
                onClose = sel::clear,
                onSelectAll = { sel.selectAll(visibleIds) },
                onDelete = { confirmDelete = true },
            )
        } else {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Period.entries.forEach { p ->
                    FilterChip(selected = period == p, onClick = { period = p }, label = { Text(stringResource(periodLabels.getValue(p))) })
                }
            }
        }
        val totals = remember(records, periodConsumption, formats) {
            listOfNotNull(
                formats.money(FuelStats.spentCents(records)),
                formats.volume(FuelStats.volumeMl(records)),
                periodConsumption?.let { formats.consumption(it) },
            ).joinToString(" · ")
        }
        Text(
            stringResource(R.string.period_summary, records.size, totals),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (records.isEmpty() && vm.loaded) {
            Text(stringResource(R.string.empty_history), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
            months.forEach { (month, list) ->
                stickyHeader(key = month.toString()) {
                    Row(
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(formats.month(month), style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        Text(formats.money(FuelStats.spentCents(list)), style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
                items(list, key = { it.id }) { r ->
                    RefuelRow(
                        r, summary.byEndId[r.id], summary.extremes, formats,
                        selecting = sel.active, checked = r.id in sel,
                        onLongClick = { sel.toggle(r.id) },
                    ) {
                        if (sel.active) sel.toggle(r.id) else onOpen(r)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

/** Contextual bar shown while selecting: close, count, select all, delete. */
@Composable
private fun SelectionBar(count: Int, allSelected: Boolean, onClose: () -> Unit, onSelectAll: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, stringResource(com.deivon.shared.R.string.shared_close))
        }
        Text(
            stringResource(R.string.selected_count, count),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        SelectionActions(allSelected = allSelected, onSelectAll = onSelectAll, onDelete = onDelete)
    }
}
