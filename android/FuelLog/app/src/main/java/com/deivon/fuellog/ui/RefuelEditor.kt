@file:OptIn(ExperimentalMaterial3Api::class)

package com.deivon.fuellog.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat as AndroidDateFormat
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.deivon.fuellog.R
import com.deivon.fuellog.logic.Decimals
import com.deivon.fuellog.logic.FormError
import com.deivon.fuellog.logic.RefuelForm
import java.util.Calendar

/** Add/edit form. Ordered for fast entry: odometer, litres and price are all most fills need. */
@Composable
fun RefuelEditor(vm: AppViewModel, form: RefuelForm, formats: Formats) {
    val errors = vm.formErrors
    val update = vm::updateForm
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val isNew = form.id == 0L
    val lastOdometer = remember(vm.refuels, form.id) { vm.refuels.firstOrNull { it.id != form.id }?.odometerKm }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(if (isNew) R.string.new_refuel else R.string.edit_refuel)) },
            navigationIcon = { IconButton(onClick = vm::closeForm) { Icon(Icons.Filled.Close, stringResource(R.string.cancel)) } },
            actions = {
                if (!isNew) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
                TextButton(onClick = vm::saveForm) { Text(stringResource(R.string.save)) }
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
                form.station, { update(form.copy(station = it)) }, R.string.station,
                error = if (FormError.Station in errors) stringResource(R.string.error_required) else null,
                capitalize = true
            )
            val suggestions = remember(vm.refuels) { vm.recentStations() }.filter { it != form.station }
            if (suggestions.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEach { s -> SuggestionChip(onClick = { update(form.copy(station = s)) }, label = { Text(s) }) }
                }
            }

            Field(
                form.odometer, { update(form.copy(odometer = it)) }, R.string.odometer, KeyboardType.Number,
                error = when {
                    FormError.Odometer in errors -> stringResource(R.string.error_number)
                    FormError.OdometerNotAfterPrevious in errors -> stringResource(R.string.error_odometer_previous)
                    FormError.OdometerNotBeforeNext in errors -> stringResource(R.string.error_odometer_next)
                    else -> null
                },
                hint = lastOdometer?.let { stringResource(R.string.last_odometer, formats.km(it)) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(
                    form.litres, { update(form.copy(litres = it)) }, R.string.litres, KeyboardType.Decimal,
                    error = if (FormError.Litres in errors) stringResource(R.string.error_positive) else null,
                    modifier = Modifier.weight(1f)
                )
                Field(
                    form.price, { update(form.copy(price = it)) }, R.string.price_per_litre, KeyboardType.Decimal,
                    error = if (FormError.Price in errors) stringResource(R.string.error_positive) else null,
                    modifier = Modifier.weight(1f)
                )
            }
            TotalField(form, formats, errors, update)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.full_tank))
                    Text(
                        stringResource(R.string.full_tank_hint), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = form.fullTank, onCheckedChange = { update(form.copy(fullTank = it)) })
            }

            Field(form.location, { update(form.copy(location = it)) }, R.string.location, capitalize = true)
            Field(form.notes, { update(form.copy(notes = it)) }, R.string.notes, singleLine = false, capitalize = true)

            vm.summary.byEndId[form.id]?.let {
                Text(
                    stringResource(R.string.interval_info, formats.consumption(it.litresPer100Km), formats.km(it.distanceKm), formats.perKm(it.costPerKm)),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.delete_title)) },
        text = { Text(stringResource(R.string.delete_message)) },
        confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete(form.id) }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } }
    )
}

/** Total: shows the calculated value until the user types over it; then it is marked as entered. */
@Composable
private fun TotalField(form: RefuelForm, formats: Formats, errors: Set<FormError>, update: (RefuelForm) -> Unit) {
    val calculated = form.calculatedTotalCents()
    val value = if (form.totalManual) form.total else calculated?.let { Decimals.toInput(it, 2, formats.locale) } ?: ""
    OutlinedTextField(
        value = value,
        onValueChange = { update(form.copy(total = it, totalManual = true)) },
        label = { Text(stringResource(R.string.total_paid)) },
        singleLine = true,
        isError = FormError.Total in errors,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        supportingText = {
            Text(
                when {
                    FormError.Total in errors -> stringResource(R.string.error_number)
                    form.totalManual -> stringResource(R.string.total_manual)
                    else -> stringResource(R.string.total_calculated)
                }
            )
        },
        trailingIcon = if (form.totalManual) {
            { IconButton(onClick = { update(form.copy(total = "", totalManual = false)) }) {
                Icon(Icons.Filled.Refresh, stringResource(R.string.recalculate))
            } }
        } else null,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DateTimeRow(timestamp: Long, formats: Formats, onChange: (Long) -> Unit) {
    val context = LocalContext.current
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    fun change(block: Calendar.() -> Unit) = onChange(Calendar.getInstance().apply { timeInMillis = timestamp; block() }.timeInMillis)

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = {
                DatePickerDialog(context, { _, y, m, d -> change { set(y, m, d) } },
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.DateRange, null)
            Spacer(Modifier.width(8.dp))
            Text(formats.date(timestamp))
        }
        OutlinedButton(onClick = {
            TimePickerDialog(context, { _, h, min -> change { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, min) } },
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), AndroidDateFormat.is24HourFormat(context)).show()
        }) { Text(formats.time(timestamp)) }
    }
}

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
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        singleLine = singleLine,
        isError = error != null,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboard,
            capitalization = if (capitalize) KeyboardCapitalization.Sentences else KeyboardCapitalization.None
        ),
        supportingText = if (error != null || hint != null) { { Text(error ?: hint.orEmpty()) } } else null,
        modifier = modifier
    )
}
