@file:OptIn(ExperimentalMaterial3Api::class)

package com.deivon.fuellog.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deivon.fuellog.R
import com.deivon.fuellog.logic.Backup
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Currency

/** Third tab: vehicles first (most used), then app settings and data. */
@Composable
fun SettingsScreen(vm: AppViewModel, showMessage: (String) -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
        VehicleList(vm)
        LanguagePicker(vm.settings)
        AppearanceAndCurrency(vm.settings)
        DataSection(vm, showMessage)
        val context = LocalContext.current
        val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.version, version ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun VehicleList(vm: AppViewModel) {
    SectionTitle(stringResource(R.string.vehicles), AppIcons.Car)
    Surface(shape = CardShape, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 4.dp)) {
            vm.vehicles.forEach { v ->
                val selected = v.id == vm.vehicle.id
                Row(
                    Modifier.fillMaxWidth().clickable { vm.selectVehicle(v.id) }.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VehicleBadge(v.type)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(vehicleName(v), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val sub = listOf(v.registration, stringResource(R.string.refuel_count, vm.refuelCount(v.id)))
                            .filter { it.isNotBlank() }.joinToString(" · ")
                        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (selected) Icon(Icons.Filled.Check, stringResource(R.string.selected), tint = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = { vm.editVehicle(v) }) { Icon(Icons.Filled.Edit, stringResource(R.string.edit_vehicle)) }
                }
            }
            TextButton(onClick = { vm.editVehicle(null) }, modifier = Modifier.padding(horizontal = 8.dp)) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_vehicle))
            }
        }
    }
}

@Composable
private fun LanguagePicker(settings: AppSettings) {
    val activity = LocalContext.current as Activity
    SectionTitle(stringResource(R.string.language), AppIcons.Language)
    (listOf("" to stringResource(R.string.system_default)) + AppSettings.LANGUAGES).forEach { (code, name) ->
        Row(
            Modifier.fillMaxWidth().selectable(selected = settings.language == code) {
                if (settings.language != code) { settings.updateLanguage(code); activity.recreate() }
            }.padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = settings.language == code, onClick = null)
            Spacer(Modifier.width(12.dp))
            Text(name)
        }
    }
}

@Composable
private fun AppearanceAndCurrency(settings: AppSettings) {
    SectionTitle(stringResource(R.string.appearance), AppIcons.Palette)
    val themes = listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        themes.forEachIndexed { i, label ->
            SegmentedButton(
                selected = settings.theme == i, onClick = { settings.updateTheme(i) },
                shape = SegmentedButtonDefaults.itemShape(i, themes.size)
            ) { Text(stringResource(label), maxLines = 1) }
        }
    }
    Spacer(Modifier.height(16.dp))
    CurrencyPicker(settings)
}

/** Common currencies first; the current one is always listed even if it is not among them. */
private val COMMON_CURRENCIES = listOf(
    "EUR", "USD", "GBP", "CHF", "MXN", "ARS", "COP", "CLP", "PEN", "BRL", "CAD", "AUD",
    "JPY", "CNY", "INR", "SAR", "AED", "MAD", "PLN", "SEK", "NOK", "DKK", "CZK", "RON", "TRY",
)

@Composable
private fun CurrencyPicker(settings: AppSettings) {
    var open by remember { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val codes = remember(settings.currency) { (listOf(settings.currency) + COMMON_CURRENCIES).distinct() }
    fun label(code: String) = runCatching {
        val c = Currency.getInstance(code)
        "$code · ${c.getSymbol(locale)} · ${c.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }}"
    }.getOrDefault(code)

    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = label(settings.currency),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.currency)) },
            leadingIcon = { Icon(AppIcons.Payments, null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            codes.forEach { code ->
                DropdownMenuItem(
                    text = { Text(label(code), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = { settings.updateCurrency(code); open = false },
                    trailingIcon = if (code == settings.currency) { { Icon(Icons.Filled.Check, null) } } else null,
                )
            }
        }
    }
}

@Composable
private fun DataSection(vm: AppViewModel, showMessage: (String) -> Unit) {
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

    SectionTitle(stringResource(R.string.data), AppIcons.Storage)
    Text(stringResource(R.string.data_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    val stamp = LocalDate.now().toString()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DataButton(AppIcons.Table, R.string.export_csv) { exportCsv.launch("fuellog-$stamp.csv") }
        DataButton(AppIcons.Download, R.string.export_json) { exportJson.launch("fuellog-backup-$stamp.json") }
        DataButton(AppIcons.Restore, R.string.restore_json) {
            importJson.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
        }
    }

    pendingRestore?.let { parsed ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text(stringResource(R.string.restore_title)) },
            text = { Text(stringResource(R.string.restore_message, parsed.vehicles.size, parsed.refuelCount, parsed.skipped)) },
            confirmButton = {
                TextButton(onClick = { pendingRestore = null; vm.restore(parsed); showMessage(msgRestored) }) {
                    Text(stringResource(R.string.restore))
                }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun DataButton(icon: ImageVector, label: Int, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(label), modifier = Modifier.weight(1f))
    }
}
