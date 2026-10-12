package com.deivon.fuellog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.deivon.fuellog.R
import com.deivon.fuellog.data.Refuel
import com.deivon.fuellog.logic.FuelStats
import com.deivon.fuellog.logic.Period

@Composable
fun OverviewScreen(vm: AppViewModel, formats: Formats, modifier: Modifier, onOpen: (Refuel) -> Unit) {
    val refuels = vm.refuels
    val summary = vm.summary
    val monthSpent = remember(refuels) { FuelStats.spentCents(Period.ThisMonth.filter(refuels)) }
    val latest = summary.latest

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            vm.vehicle.name.ifBlank { stringResource(R.string.app_name) },
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.this_month), formats.money(monthSpent), "", Modifier.weight(1f))
            StatCard(
                stringResource(R.string.consumption), latest?.let { formats.consumption(it.litresPer100Km) },
                stringResource(R.string.needs_two_full), Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                stringResource(R.string.average_price), summary.averagePriceMilli?.let { formats.pricePerLitre(it) },
                stringResource(R.string.no_data), Modifier.weight(1f)
            )
            StatCard(
                stringResource(R.string.cost_per_km), latest?.let { formats.perKm(it.costPerKm) },
                stringResource(R.string.needs_two_full), Modifier.weight(1f)
            )
        }

        SectionTitle(stringResource(R.string.latest_refuel))
        val last = refuels.firstOrNull()
        if (last == null) {
            if (vm.loaded) Text(stringResource(R.string.empty_overview), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Surface(shape = CardShape, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                RefuelRow(last, summary.byEndId[last.id], formats) { onOpen(last) }
            }
        }
        Spacer(Modifier.height(88.dp)) // Room for the floating button.
    }
}
