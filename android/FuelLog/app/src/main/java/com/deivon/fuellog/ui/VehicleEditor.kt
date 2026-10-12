@file:OptIn(ExperimentalMaterial3Api::class)

package com.deivon.fuellog.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.deivon.fuellog.R
import com.deivon.shared.ui.ConfirmDialog
import com.deivon.fuellog.data.Vehicle
import com.deivon.fuellog.data.VehicleType
import com.deivon.fuellog.logic.CatalogFuel
import com.deivon.fuellog.logic.CatalogModel
import com.deivon.fuellog.logic.Decimals
import com.deivon.fuellog.logic.ModelCatalog
import com.deivon.fuellog.logic.UnitSystem
import com.deivon.fuellog.logic.Units

private val typeLabels = mapOf(
    VehicleType.Car to R.string.type_car, VehicleType.Motorcycle to R.string.type_motorcycle,
    VehicleType.Van to R.string.type_van, VehicleType.Truck to R.string.type_truck,
    VehicleType.Bus to R.string.type_bus, VehicleType.Plane to R.string.type_plane,
    VehicleType.Boat to R.string.type_boat, VehicleType.Other to R.string.type_other,
)

/** Quick picks for the free-text fuel type, in the user's language. */
private val fuelLabels = listOf(R.string.fuel_petrol, R.string.fuel_diesel, R.string.fuel_hybrid, R.string.fuel_lpg, R.string.fuel_cng)
private val catalogFuelLabels = mapOf(
    CatalogFuel.Petrol to R.string.fuel_petrol, CatalogFuel.Diesel to R.string.fuel_diesel, CatalogFuel.Hybrid to R.string.fuel_hybrid,
)

/** Add/edit a vehicle. Edits stay local until Save, so half-typed values never reach the database. */
@Composable
fun VehicleEditor(vm: AppViewModel, original: Vehicle) {
    val locale = LocalConfiguration.current.locales[0]
    val units = vm.settings.units
    /** Stored tenths of L/100 km → text in the user's unit (L/100 km or mpg), one decimal. */
    fun refText(dl100: Int) = Decimals.toInput(Math.round(Units.consumption(dl100 / 10.0, units) * 10), 1, locale)
    var name by rememberSaveable { mutableStateOf(original.name) }
    var type by rememberSaveable { mutableStateOf(original.type) }
    var registration by rememberSaveable { mutableStateOf(original.registration) }
    var fuelType by rememberSaveable { mutableStateOf(original.fuelType) }
    var notes by rememberSaveable { mutableStateOf(original.notes) }
    var reference by rememberSaveable { mutableStateOf(original.referenceDl100?.let(::refText) ?: "") }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var pickingModel by rememberSaveable { mutableStateOf(false) }
    val isNew = original.id == 0L
    val canDelete = !isNew && vm.vehicles.size > 1
    val referenceValue = Decimals.parse(reference, 1)?.let { Units.consumptionToDl100(it / 10.0, units) }?.takeIf { it in 1..999 }
    val referenceInvalid = reference.isNotBlank() && referenceValue == null

    if (pickingModel) {
        BackHandler { pickingModel = false }
        val fuelNames = catalogFuelLabels.mapValues { stringResource(it.value) }
        ModelPicker(vm, onClose = { pickingModel = false }) { m ->
            name = m.name
            type = VehicleType.Car
            fuelType = fuelNames.getValue(m.fuel)
            reference = refText(m.referenceDl100)
            pickingModel = false
        }
        return
    }

    fun save() = vm.saveVehicle(
        original.copy(
            name = name.trim(), type = type, registration = registration.trim(), fuelType = fuelType.trim(),
            notes = notes.trim(), referenceDl100 = referenceValue,
        )
    )

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(if (isNew) R.string.add_vehicle else R.string.edit_vehicle)) },
            navigationIcon = { IconButton(onClick = vm::closeVehicle) { Icon(Icons.Filled.Close, stringResource(R.string.cancel)) } },
            actions = {
                if (canDelete) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
                TextButton(onClick = { save() }, enabled = !referenceInvalid) { Text(stringResource(R.string.save)) }
            }
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(onClick = { pickingModel = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Search, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.pick_model))
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VehicleType.entries.forEach { t ->
                    FilterChip(
                        selected = type == t, onClick = { type = t },
                        label = { Text(stringResource(typeLabels.getValue(t))) },
                        leadingIcon = { Icon(AppIcons.of(t), null, Modifier.size(18.dp)) }
                    )
                }
            }
            Input(name, { name = it }, R.string.vehicle_name, AppIcons.of(type))
            Input(registration, { registration = it }, R.string.registration, AppIcons.Plate, KeyboardCapitalization.Characters)
            Input(fuelType, { fuelType = it }, R.string.fuel_type, AppIcons.Fuel)
            val fuels = fuelLabels.map { stringResource(it) }.filter { it != fuelType }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fuels.forEach { f -> SuggestionChip(onClick = { fuelType = f }, label = { Text(f) }) }
            }
            OutlinedTextField(
                value = reference,
                onValueChange = { reference = it },
                label = { Text(stringResource(R.string.reference_consumption)) },
                leadingIcon = { Icon(AppIcons.Speed, null) },
                suffix = { Text(if (units == UnitSystem.Metric) "L/100 km" else "mpg") },
                supportingText = { Text(stringResource(if (referenceInvalid) R.string.error_number else R.string.reference_hint)) },
                isError = referenceInvalid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Input(notes, { notes = it }, R.string.notes, AppIcons.Notes, singleLine = false)
        }
    }

    if (confirmDelete) ConfirmDialog(
        title = stringResource(R.string.delete_vehicle_title),
        text = stringResource(R.string.delete_vehicle_message, vm.refuelCount(original.id)),
        confirmLabel = stringResource(R.string.delete),
        destructive = true,
        onConfirm = { confirmDelete = false; vm.deleteVehicle(original.id) },
        onDismiss = { confirmDelete = false },
    )
}

/** Searchable brand/model list (Spain's best sellers) with reference consumption. */
@Composable
private fun ModelPicker(vm: AppViewModel, onClose: () -> Unit, onPick: (CatalogModel) -> Unit) {
    LaunchedEffect(Unit) { vm.loadCatalog() }
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(vm.catalog, query) { ModelCatalog.search(vm.catalog, query) }
    val formats = rememberFormats(vm.settings.currency, vm.settings.units)

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.pick_model)) },
            navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Filled.Close, stringResource(R.string.cancel)) } },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_model)) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Text(
                stringResource(R.string.catalog_note), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp)
            )
            LazyColumn {
                items(results, key = { it.name }) { m ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(m) }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VehicleBadge(VehicleType.Car, size = 36)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.name, style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(catalogFuelLabels.getValue(m.fuel)), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(formats.consumption(m.referenceDl100 / 10.0), style = MaterialTheme.typography.bodyMedium)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun Input(
    value: String, onChange: (String) -> Unit, label: Int, icon: ImageVector,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences, singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(stringResource(label)) }, singleLine = singleLine,
        leadingIcon = { Icon(icon, null) },
        keyboardOptions = KeyboardOptions(capitalization = capitalization),
        modifier = Modifier.fillMaxWidth()
    )
}
