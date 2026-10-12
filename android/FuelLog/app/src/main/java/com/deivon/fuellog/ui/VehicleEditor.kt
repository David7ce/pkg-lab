@file:OptIn(ExperimentalMaterial3Api::class)

package com.deivon.fuellog.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.deivon.fuellog.R
import com.deivon.fuellog.data.Vehicle
import com.deivon.fuellog.data.VehicleType

private val typeLabels = mapOf(
    VehicleType.Car to R.string.type_car, VehicleType.Motorcycle to R.string.type_motorcycle,
    VehicleType.Van to R.string.type_van, VehicleType.Truck to R.string.type_truck,
    VehicleType.Bus to R.string.type_bus, VehicleType.Plane to R.string.type_plane,
    VehicleType.Boat to R.string.type_boat, VehicleType.Other to R.string.type_other,
)

/** Add/edit a vehicle. Edits stay local until Save, so half-typed values never reach the database. */
@Composable
fun VehicleEditor(vm: AppViewModel, original: Vehicle) {
    var name by rememberSaveable { mutableStateOf(original.name) }
    var type by rememberSaveable { mutableStateOf(original.type) }
    var registration by rememberSaveable { mutableStateOf(original.registration) }
    var fuelType by rememberSaveable { mutableStateOf(original.fuelType) }
    var notes by rememberSaveable { mutableStateOf(original.notes) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val isNew = original.id == 0L
    val canDelete = !isNew && vm.vehicles.size > 1

    fun save() = vm.saveVehicle(
        original.copy(name = name.trim(), type = type, registration = registration.trim(), fuelType = fuelType.trim(), notes = notes.trim())
    )

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(if (isNew) R.string.add_vehicle else R.string.edit_vehicle)) },
            navigationIcon = { IconButton(onClick = vm::closeVehicle) { Icon(Icons.Filled.Close, stringResource(R.string.cancel)) } },
            actions = {
                if (canDelete) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
                TextButton(onClick = { save() }) { Text(stringResource(R.string.save)) }
            }
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VehicleType.entries.forEach { t ->
                    FilterChip(
                        selected = type == t, onClick = { type = t },
                        label = { Text(stringResource(typeLabels.getValue(t))) },
                        leadingIcon = { Icon(AppIcons.of(t), null, Modifier.size(18.dp)) }
                    )
                }
            }
            Input(name, { name = it }, R.string.vehicle_name)
            Input(registration, { registration = it }, R.string.registration)
            Input(fuelType, { fuelType = it }, R.string.fuel_type)
            Input(notes, { notes = it }, R.string.notes, singleLine = false)
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.delete_vehicle_title)) },
        text = { Text(stringResource(R.string.delete_vehicle_message, vm.refuelCount(original.id))) },
        confirmButton = { TextButton(onClick = { confirmDelete = false; vm.deleteVehicle(original.id) }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun Input(value: String, onChange: (String) -> Unit, label: Int, singleLine: Boolean = true) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(stringResource(label)) }, singleLine = singleLine,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth()
    )
}
