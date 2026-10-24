package com.david7ce.calistenia.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.david7ce.calistenia.model.Exercise
import com.david7ce.calistenia.model.Measure
import com.david7ce.calistenia.model.MuscleGroup
import com.david7ce.shared.ui.SegmentedChoice

/** Create or rename a custom exercise. Built-ins never reach this dialog. */
@Composable
fun ExerciseEditDialog(
    initial: Exercise?,
    onDismiss: () -> Unit,
    onSave: (name: String, group: MuscleGroup, measure: Measure) -> Unit,
) {
    val strings = LocalStrings.current
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var group by remember(initial) { mutableStateOf(initial?.group ?: MuscleGroup.Push) }
    var measure by remember(initial) { mutableStateOf(initial?.measure ?: Measure.Reps) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) strings.newExercise else strings.editExercise) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text(strings.exerciseName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    strings.group,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                )
                SegmentedChoice(
                    options = MuscleGroup.entries.map { it to strings.groupLabel(it) },
                    selected = group,
                    onSelect = { group = it },
                )
                Text(
                    strings.measuredIn,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                )
                SegmentedChoice(
                    options = Measure.entries.map { it to strings.measureLabel(it) },
                    selected = measure,
                    onSelect = { measure = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), group, measure) },
                enabled = name.isNotBlank(),
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } },
    )
}
