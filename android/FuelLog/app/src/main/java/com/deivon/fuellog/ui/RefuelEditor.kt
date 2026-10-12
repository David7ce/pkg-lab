@file:OptIn(ExperimentalMaterial3Api::class)

package com.david7ce.fuellog.ui

import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat as AndroidDateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.david7ce.fuellog.R
import com.david7ce.shared.ui.ConfirmDialog
import com.david7ce.shared.ui.SegmentedChoice
import com.david7ce.fuellog.logic.FormError
import com.david7ce.fuellog.logic.FormWarning
import com.david7ce.fuellog.logic.GeoPoint
import com.david7ce.fuellog.logic.RefuelForm
import com.david7ce.fuellog.ui.map.MapPicker
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Add/edit form. Ordered for fast entry: the cursor starts on the odometer and "Next" walks the fields. */
@Composable
fun RefuelEditor(vm: AppViewModel, form: RefuelForm, formats: Formats, showMessage: (String) -> Unit) {
    val errors = vm.formErrors
    val update = vm::updateForm
    var pickingLocation by rememberSaveable { mutableStateOf(false) }

    if (pickingLocation) {
        BackHandler { pickingLocation = false }
        val start = form.location ?: vm.savedLocations.firstOrNull()
        MapPicker(
            initial = start ?: GeoPoint(40.4168, -3.7038), // Madrid when nothing is known yet
            initialZoom = if (start != null) 16.0 else 5.0,
            pins = vm.savedLocations,
            onPick = { update(form.copy(location = it)); pickingLocation = false },
            onClose = { pickingLocation = false },
            showMessage = showMessage,
        )
        return
    }
    BackHandler(onBack = vm::closeForm)
    val isNew = form.id == 0L
    val lastOdometer = remember(vm.refuels, form.id) { vm.refuels.firstOrNull { it.id != form.id }?.odometerKm }
    val odometerFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (isNew && form.odometer.isEmpty()) runCatching { odometerFocus.requestFocus() } }

    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) ConfirmDialog(
        text = stringResource(R.string.delete_refuel_confirm),
        confirmLabel = stringResource(R.string.delete),
        destructive = true,
        onConfirm = { confirmDelete = false; vm.delete(form.id) },
        onDismiss = { confirmDelete = false },
    )

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(if (isNew) R.string.new_refuel else R.string.edit_refuel)) },
            navigationIcon = { IconButton(onClick = vm::closeForm) { Icon(Icons.Filled.Close, stringResource(R.string.cancel)) } },
            actions = {
                if (!isNew) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
                TextButton(onClick = { vm.saveForm() }) { Text(stringResource(R.string.save)) }
            }
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DateTimeRow(form.timestamp, formats) { update(form.copy(timestamp = it)) }

            Field(
                form.odometer, { update(form.copy(odometer = it)) }, R.string.odometer, KeyboardType.Number,
                error = when {
                    FormError.Odometer in errors -> stringResource(R.string.error_number)
                    FormError.OdometerNotAfterPrevious in errors -> stringResource(R.string.error_odometer_previous)
                    FormError.OdometerNotBeforeNext in errors -> stringResource(R.string.error_odometer_next)
                    else -> null
                },
                hint = lastOdometer?.let { stringResource(R.string.last_odometer, formats.distance(it)) },
                icon = AppIcons.Speed, suffix = formats.distanceUnit,
                modifier = Modifier.fillMaxWidth().focusRequester(odometerFocus)
            )

            SegmentedChoice(
                options = listOf(false to stringResource(R.string.by_volume), true to stringResource(R.string.by_amount)),
                selected = form.byAmount,
                onSelect = { update(form.copy(byAmount = it)) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (form.byAmount) {
                    Field(
                        form.amount, { update(form.copy(amount = it)) }, R.string.amount_paid, KeyboardType.Decimal,
                        error = if (FormError.Amount in errors) stringResource(R.string.error_positive) else null,
                        modifier = Modifier.weight(1f), icon = AppIcons.Payments
                    )
                } else {
                    Field(
                        form.litres, { update(form.copy(litres = it)) },
                        if (formats.units.usesGallons) R.string.gallons else R.string.litres, KeyboardType.Decimal,
                        error = if (FormError.Litres in errors) stringResource(R.string.error_positive) else null,
                        modifier = Modifier.weight(1f), icon = AppIcons.Drop, suffix = formats.volumeUnit
                    )
                }
                Field(
                    form.price, { update(form.copy(price = it)) },
                    if (formats.units.usesGallons) R.string.price_per_gallon else R.string.price_per_litre, KeyboardType.Decimal,
                    error = if (FormError.Price in errors) stringResource(R.string.error_positive) else null,
                    modifier = Modifier.weight(1f), icon = AppIcons.Fuel
                )
            }
            TotalRow(form, formats)

            Field(
                form.station, { update(form.copy(station = it)) }, R.string.station,
                error = if (FormError.Station in errors) stringResource(R.string.error_required) else null,
                capitalize = true, icon = AppIcons.Fuel
            )
            val suggestions = vm.recentStations.filter { it.first != form.station }
            if (suggestions.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEach { (name, point) ->
                        SuggestionChip(
                            onClick = { update(form.copy(station = name, location = form.location ?: point)) },
                            label = { Text(name) },
                            icon = { Icon(AppIcons.Fuel, null, Modifier.size(16.dp)) }
                        )
                    }
                }
            }

            SwitchRow(R.string.full_tank, R.string.full_tank_hint, form.fullTank) { update(form.copy(fullTank = it)) }
            SwitchRow(R.string.missed_previous, R.string.missed_previous_hint, form.missedPrevious) { update(form.copy(missedPrevious = it)) }

            LocationRow(form.location, onPick = { pickingLocation = true }, onClear = { update(form.copy(location = null)) })
            Field(form.notes, { update(form.copy(notes = it)) }, R.string.notes, singleLine = false, capitalize = true, icon = AppIcons.Notes)

            vm.summary.byEndId[form.id]?.let {
                Text(
                    stringResource(R.string.interval_info, formats.consumption(it.litresPer100Km), formats.distance(it.distanceKm), formats.perDistance(it.costPerKm)),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (vm.pendingWarnings.isNotEmpty()) WarningsDialog(vm.pendingWarnings, formats, onFix = vm::dismissWarnings) { vm.saveForm(confirmed = true) }
}

@Composable
private fun SwitchRow(title: Int, hint: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title))
            Text(stringResource(hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** "This looks like a typo": lists what is odd and lets the user fix it or save anyway. */
@Composable
private fun WarningsDialog(warnings: List<FormWarning>, formats: Formats, onFix: () -> Unit, onSaveAnyway: () -> Unit) {
    AlertDialog(
        onDismissRequest = onFix,
        icon = { Icon(Icons.Filled.Warning, null) },
        title = { Text(stringResource(R.string.warn_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                warnings.forEach { w ->
                    Text(
                        when (w) {
                            is FormWarning.LongDistance -> stringResource(R.string.warn_distance, formats.distance(w.km))
                            is FormWarning.OddConsumption -> stringResource(R.string.warn_consumption, formats.consumption(w.l100))
                            is FormWarning.OddPrice -> stringResource(R.string.warn_price, formats.pricePerVolume(w.averageMilli))
                        }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onSaveAnyway) { Text(stringResource(R.string.save_anyway)) } },
        dismissButton = { TextButton(onClick = onFix) { Text(stringResource(R.string.review)) } }
    )
}

/** Result of the fill: total always volume × price; in amount mode it also shows the derived volume. */
@Composable
private fun TotalRow(form: RefuelForm, formats: Formats) {
    val u = formats.units
    Surface(shape = CardShape, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Payments, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.total_paid), style = MaterialTheme.typography.labelMedium)
                Text(
                    if (form.byAmount) form.volumeMl(u)?.let { "≈ " + formats.volume(it) } ?: "—"
                    else stringResource(R.string.total_calculated),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(form.totalCents(u)?.let { formats.money(it) } ?: "—", style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** Optional coordinates: pick on the map, open in another maps app, or clear. */
@Composable
private fun LocationRow(location: GeoPoint?, onPick: () -> Unit, onClear: () -> Unit) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onPick, modifier = Modifier.weight(1f)) {
            Icon(if (location == null) AppIcons.Map else Icons.Filled.Place, null)
            Spacer(Modifier.width(8.dp))
            Text(location?.format() ?: stringResource(R.string.add_location), maxLines = 1)
        }
        if (location != null) {
            IconButton(onClick = {
                val uri = Uri.parse("geo:${location.lat},${location.lon}?q=${location.lat},${location.lon}")
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            }) { Icon(AppIcons.Map, stringResource(R.string.open_in_maps)) }
            IconButton(onClick = onClear) { Icon(Icons.Filled.Close, stringResource(R.string.remove_location)) }
        }
    }
}

/**
 * Material 3 date and time pickers. The date picker works in UTC midnights, so the local date is
 * converted to/from UTC explicitly and the time of day is kept.
 */
@Composable
private fun DateTimeRow(timestamp: Long, formats: Formats, onChange: (Long) -> Unit) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val local = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zone)
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var pickTime by rememberSaveable { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.DateRange, null)
            Spacer(Modifier.width(8.dp))
            Text(formats.date(timestamp))
        }
        OutlinedButton(onClick = { pickTime = true }) { Text(formats.time(timestamp)) }
    }

    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = local.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { utc ->
                        val date = Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
                        onChange(date.atTime(local.toLocalTime()).atZone(zone).toInstant().toEpochMilli())
                    }
                    pickDate = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.cancel)) } }
        ) { DatePicker(state) }
    }
    if (pickTime) {
        val state = rememberTimePickerState(local.hour, local.minute, AndroidDateFormat.is24HourFormat(context))
        AlertDialog(
            onDismissRequest = { pickTime = false },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = {
                    onChange(local.withHour(state.hour).withMinute(state.minute).atZone(zone).toInstant().toEpochMilli())
                    pickTime = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

/** Single-line fields show "Next" on the keyboard, which moves to the following field. */
@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: Int,
    keyboard: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth(),
    error: String? = null,
    hint: String? = null,
    singleLine: Boolean = true,
    capitalize: Boolean = false,
    icon: ImageVector? = null,
    suffix: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        leadingIcon = if (icon != null) { { Icon(icon, null) } } else null,
        suffix = if (suffix != null) { { Text(suffix) } } else null,
        singleLine = singleLine,
        isError = error != null,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboard,
            capitalization = if (capitalize) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
        ),
        supportingText = if (error != null || hint != null) { { Text(error ?: hint.orEmpty()) } } else null,
        modifier = modifier
    )
}
