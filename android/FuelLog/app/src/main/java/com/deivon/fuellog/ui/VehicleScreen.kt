@file:OptIn(ExperimentalMaterial3Api::class)

package com.deivon.fuellog.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.deivon.fuellog.R
import com.deivon.fuellog.logic.Backup
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun VehicleScreen(vm: AppViewModel, showMessage: (String) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingRestore by remember { mutableStateOf<Backup.Parsed?>(null) }

    val msgExported = stringResource(R.string.exported)
    val msgError = stringResource(R.string.file_error)
    val msgRestored = stringResource(R.string.restored)

    fun runFileAction(action: suspend () -> Unit, success: String?) = scope.launch {
        try { action(); success?.let(showMessage) }
        catch (e: Backup.InvalidBackup) { showMessage(msgError + ": " + e.message) }
        catch (e: Exception) { showMessage(msgError) }
    }

    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { runFileAction({ vm.exportCsv(it) }, msgExported) }
    }
    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { runFileAction({ vm.exportJson(it) }, msgExported) }
    }
    val importJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { runFileAction({ pendingRestore = vm.readBackup(it) }, null) }
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        VehicleForm(vm)

        SectionTitle(stringResource(R.string.appearance))
        val themes = listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            themes.forEachIndexed { i, label ->
                SegmentedButton(
                    selected = vm.settings.theme == i, onClick = { vm.settings.updateTheme(i) },
                    shape = SegmentedButtonDefaults.itemShape(i, themes.size)
                ) { Text(stringResource(label), maxLines = 1) }
            }
        }
        Spacer(Modifier.height(12.dp))
        var currency by rememberSaveable { mutableStateOf(vm.settings.currency) }
        OutlinedTextField(
            value = currency,
            onValueChange = { currency = it.take(3).uppercase(); if (currency.length == 3) vm.settings.updateCurrency(currency) },
            label = { Text(stringResource(R.string.currency)) },
            supportingText = { Text(stringResource(R.string.currency_hint)) },
            isError = currency != vm.settings.currency,
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth()
        )

        SectionTitle(stringResource(R.string.data))
        Text(stringResource(R.string.data_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        val stamp = LocalDate.now().toString()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { exportCsv.launch("fuellog-$stamp.csv") }, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.export_csv))
            }
            OutlinedButton(onClick = { exportJson.launch("fuellog-backup-$stamp.json") }, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.export_json))
            }
            OutlinedButton(onClick = { importJson.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.restore_json))
            }
        }

        Spacer(Modifier.height(20.dp))
        val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
        Text(stringResource(R.string.version, version ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
    }

    pendingRestore?.let { parsed ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text(stringResource(R.string.restore_title)) },
            text = { Text(stringResource(R.string.restore_message, parsed.refuels.size, parsed.skipped, vm.refuels.size)) },
            confirmButton = {
                TextButton(onClick = { pendingRestore = null; vm.restore(parsed); showMessage(msgRestored) }) {
                    Text(stringResource(R.string.restore))
                }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

/** Vehicle details, saved explicitly so half-typed edits never hit the database. */
@Composable
private fun VehicleForm(vm: AppViewModel) {
    val saved = vm.vehicle
    var name by rememberSaveable(saved) { mutableStateOf(saved.name) }
    var registration by rememberSaveable(saved) { mutableStateOf(saved.registration) }
    var fuelType by rememberSaveable(saved) { mutableStateOf(saved.fuelType) }
    var notes by rememberSaveable(saved) { mutableStateOf(saved.notes) }
    val edited = saved.copy(name = name.trim(), registration = registration.trim(), fuelType = fuelType.trim(), notes = notes.trim())

    SectionTitle(stringResource(R.string.vehicle))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextInput(name, { name = it }, R.string.vehicle_name)
        TextInput(registration, { registration = it }, R.string.registration)
        TextInput(fuelType, { fuelType = it }, R.string.fuel_type)
        TextInput(notes, { notes = it }, R.string.notes, singleLine = false)
        Button(onClick = { vm.saveVehicle(edited) }, enabled = edited != saved) { Text(stringResource(R.string.save)) }
    }
}

@Composable
private fun TextInput(value: String, onChange: (String) -> Unit, label: Int, singleLine: Boolean = true) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(stringResource(label)) }, singleLine = singleLine,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth()
    )
}
