package com.deivon.fuellog.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deivon.fuellog.R
import com.deivon.fuellog.data.Refuel
import com.deivon.fuellog.data.Vehicle
import com.deivon.fuellog.logic.FuelStats
import com.deivon.fuellog.logic.Period

@Composable
fun OverviewScreen(vm: AppViewModel, formats: Formats, modifier: Modifier, onOpen: (Refuel) -> Unit) {
    val refuels = vm.refuels
    val summary = vm.summary
    val monthSpent = remember(refuels) { FuelStats.spentCents(Period.ThisMonth.filter(refuels)) }
    val latest = summary.latest
    val needsTwo = stringResource(R.string.needs_two_full)

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
        VehicleSelector(vm)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(AppIcons.Payments, stringResource(R.string.this_month), formats.money(monthSpent), "", Modifier.weight(1f))
            val reference = vm.vehicle.referenceDl100
            val diff = FuelStats.versusReference(latest?.litresPer100Km, reference)
            StatCard(
                AppIcons.Speed, stringResource(R.string.consumption),
                latest?.let { formats.consumption(it.litresPer100Km) }, needsTwo, Modifier.weight(1f),
                extra = reference?.let {
                    val ref = stringResource(R.string.reference_short, formats.consumption(it / 10.0))
                    if (diff == null) ref else "$ref · ${formats.percent(diff)}"
                },
                extraColor = when {
                    diff == null -> Color.Unspecified
                    diff > 5 -> highColor()
                    diff < -5 -> lowColor()
                    else -> Color.Unspecified
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(AppIcons.Fuel, stringResource(R.string.average_price),
                summary.averagePriceMilli?.let { formats.pricePerLitre(it) }, stringResource(R.string.no_data), Modifier.weight(1f))
            StatCard(AppIcons.Drop, stringResource(R.string.cost_per_km),
                latest?.let { formats.perKm(it.costPerKm) }, needsTwo, Modifier.weight(1f))
        }

        Highlights(vm, formats, onOpen)

        SectionTitle(stringResource(R.string.latest_refuel))
        val last = refuels.firstOrNull()
        if (last == null) {
            if (vm.loaded) Text(stringResource(R.string.empty_overview), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Surface(shape = CardShape, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                RefuelRow(last, summary.byEndId[last.id], summary.extremes, formats) { onOpen(last) }
            }
        }
        Spacer(Modifier.height(88.dp)) // Room for the floating button.
    }
}

/** Current vehicle; with more than one, tapping opens a menu to switch. */
@Composable
private fun VehicleSelector(vm: AppViewModel) {
    var open by remember { mutableStateOf(false) }
    val v = vm.vehicle
    Box {
        Row(
            Modifier.clickable { open = true }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VehicleBadge(v.type)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f, fill = false)) {
                Text(vehicleName(v), style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = listOf(v.registration, v.fuelType).filter { it.isNotBlank() }.joinToString(" · ")
                if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ArrowDropDown, stringResource(R.string.switch_vehicle))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            vm.vehicles.forEach { item ->
                DropdownMenuItem(
                    text = { Text(vehicleName(item)) },
                    leadingIcon = { Icon(AppIcons.of(item.type), null) },
                    onClick = { open = false; vm.selectVehicle(item.id) }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.add_vehicle)) },
                leadingIcon = { Icon(Icons.Filled.Add, null) },
                onClick = { open = false; vm.editVehicle(null) }
            )
        }
    }
}

@Composable
fun vehicleName(v: Vehicle): String = v.name.ifBlank { stringResource(R.string.unnamed_vehicle) }

/** Highest/lowest price and best/worst consumption, each linking to its refuelling. */
@Composable
private fun Highlights(vm: AppViewModel, formats: Formats, onOpen: (Refuel) -> Unit) {
    val e = vm.summary.extremes
    val byId = remember(vm.refuels) { vm.refuels.associateBy { it.id } }
    val rows = buildList {
        e.highestPrice?.let { add(Highlight(AppIcons.TrendingUp, highColor(), R.string.highest_price, formats.pricePerLitre(it.pricePerLitreMilli), it)) }
        e.lowestPrice?.let { add(Highlight(AppIcons.TrendingDown, lowColor(), R.string.lowest_price, formats.pricePerLitre(it.pricePerLitreMilli), it)) }
        e.worstConsumption?.let { add(Highlight(AppIcons.TrendingUp, highColor(), R.string.worst_consumption, formats.consumption(it.litresPer100Km), byId[it.endId])) }
        e.bestConsumption?.let { add(Highlight(AppIcons.TrendingDown, lowColor(), R.string.best_consumption, formats.consumption(it.litresPer100Km), byId[it.endId])) }
    }
    if (rows.isEmpty()) return

    SectionTitle(stringResource(R.string.highlights))
    Surface(shape = CardShape, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 4.dp)) {
            rows.forEach { h ->
                Row(
                    Modifier.fillMaxWidth().clickable(enabled = h.refuel != null) { h.refuel?.let(onOpen) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(h.icon, null, Modifier.size(20.dp), tint = h.color)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(h.label), style = MaterialTheme.typography.bodyMedium)
                        h.refuel?.let {
                            Text("${formats.date(it.timestamp)} · ${it.station}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Text(h.value, style = MaterialTheme.typography.titleSmall, color = h.color)
                }
            }
        }
    }
}

private class Highlight(val icon: ImageVector, val color: Color, val label: Int, val value: String, val refuel: Refuel?)
