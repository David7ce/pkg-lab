package com.david7ce.sleepcycles.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.david7ce.shared.ui.SegmentedChoice
import com.david7ce.sleepcycles.logic.ClockFormat
import com.david7ce.sleepcycles.logic.parseTime
import com.david7ce.sleepcycles.logic.timeParts

/**
 * Typed hh:mm input (like the PWA), plus AM/PM in 12-hour format. A value is emitted only when the
 * text is a complete valid time; otherwise the fields are flagged and the last valid time stays.
 * Always left to right, also under an RTL layout direction.
 */
@Composable
fun TimeField(minutes: Int, format: ClockFormat, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val initial = timeParts(minutes, format)
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf(initial.minute) }
    var pm by remember { mutableStateOf(initial.pm) }
    var emitted by remember { mutableStateOf(minutes) }
    var shownFormat by remember { mutableStateOf(format) }

    // A change that did not come from typing here (Sleep now, mode or format switch) replaces the fields.
    // Own typing must not: it would turn a typed "1" into "01" under the cursor.
    LaunchedEffect(minutes, format) {
        if (minutes != emitted || format != shownFormat) {
            val p = timeParts(minutes, format)
            hour = p.hour
            minute = p.minute
            pm = p.pm
            emitted = minutes
            shownFormat = format
        }
    }

    fun commit() {
        parseTime(hour, minute, pm, format)?.let {
            emitted = it
            onChange(it)
        }
    }
    val valid = parseTime(hour, minute, pm, format) != null

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DigitField(hour, s.hourLabel, !valid, Modifier.weight(1f)) {
                hour = it
                commit()
            }
            Text(":", style = MaterialTheme.typography.headlineMedium)
            DigitField(minute, s.minuteLabel, !valid, Modifier.weight(1f)) {
                minute = it
                commit()
            }
            if (format == ClockFormat.H12) {
                SegmentedChoice(
                    options = listOf(false to s.am, true to s.pm),
                    selected = pm,
                    modifier = Modifier.width(132.dp),
                ) {
                    pm = it
                    commit()
                }
            }
        }
    }
}

@Composable
private fun DigitField(value: String, label: String, isError: Boolean, modifier: Modifier, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValue(it.filter(Char::isDigit).take(2)) },
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}
