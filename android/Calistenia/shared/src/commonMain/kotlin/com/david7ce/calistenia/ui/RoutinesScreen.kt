package com.david7ce.calistenia.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.david7ce.calistenia.logic.BuiltinRoutines
import com.david7ce.calistenia.logic.trainsAny
import com.david7ce.calistenia.model.Routine
import com.david7ce.shared.ui.ConfirmDialog
import com.david7ce.shared.ui.SharedIcons

@Composable
fun RoutinesScreen(app: AppState) {
    val strings = LocalStrings.current
    var toDelete by remember { mutableStateOf<Routine?>(null) }
    var showStarters by remember { mutableStateOf(false) }
    val shown = remember(app.routines, app.muscleFilter) {
        app.routines.filter { it.trainsAny(app.muscleFilter) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Button(onClick = app::openNewRoutine, modifier = Modifier.fillMaxWidth()) {
                Icon(SharedIcons.Add, null)
                Text(strings.newRoutine, modifier = Modifier.padding(start = 8.dp))
            }
        }
        item {
            OutlinedButton(onClick = { showStarters = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(SharedIcons.Checklist, null)
                Text(strings.starterRoutines, modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (app.muscleFilter.isNotEmpty()) {
            item { MuscleFilterRow(app) }
        }
        if (app.routines.isEmpty() || shown.isEmpty()) {
            item {
                Text(
                    if (app.routines.isEmpty()) strings.emptyRoutines else strings.noResults,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        } else {
            items(shown, key = { it.id }) { routine ->
                RoutineCard(
                    app = app,
                    routine = routine,
                    onEdit = { app.editRoutine(routine.id) },
                    onDelete = { toDelete = routine },
                )
            }
        }
    }

    toDelete?.let { routine ->
        ConfirmDialog(
            title = strings.deleteRoutineTitle,
            text = strings.deleteRoutineText(routine.name),
            confirmLabel = strings.delete,
            onConfirm = {
                app.deleteRoutine(routine.id)
                toDelete = null
            },
            onDismiss = { toDelete = null },
            destructive = true,
        )
    }

    if (showStarters) {
        StarterRoutinesDialog(app, onDismiss = { showStarters = false })
    }
}

@Composable
private fun StarterRoutinesDialog(app: AppState, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.starterRoutines) },
        text = {
            LazyColumn(Modifier.heightIn(max = 440.dp)) {
                items(BuiltinRoutines.all, key = { it.key }) { template ->
                    val exerciseCount = template.exercises.count { line ->
                        app.exercises.any { it.id == line.exerciseId }
                    }
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(
                            template.name(app.languageCode),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            template.notes(app.languageCode),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                strings.exercisesCount(exerciseCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                onClick = { app.addStarterRoutine(template, onDone = onDismiss) },
                            ) { Text(strings.add) }
                        }
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.close) } },
    )
}

@Composable
private fun RoutineCard(
    app: AppState,
    routine: Routine,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val strings = LocalStrings.current
    val lastUsed = app.lastUsedAt(routine.id)
    Card(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        routine.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        strings.exercisesCount(routine.exercises.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (lastUsed == null) strings.neverUsed else
                            "${strings.lastUsed}: ${app.formatShortDate(lastUsed)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { app.startWorkout(routine) }) {
                    Icon(SharedIcons.Play, strings.startRoutine, tint = MaterialTheme.colorScheme.primary)
                }
            }
            // With a muscle selection the matching exercises come first (so a match is never
            // hidden behind the 3-name cut) and are drawn in the primary colour, bold.
            val selection = app.muscleFilter
            val preview = routine.exercises
                .sortedBy { selection.isNotEmpty() && !it.exercise.trainsAny(selection) }
                .take(3)
            val highlight = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(
                buildAnnotatedString {
                    preview.forEachIndexed { i, line ->
                        if (i > 0) append(" · ")
                        if (selection.isNotEmpty() && line.exercise.trainsAny(selection)) {
                            withStyle(highlight) { append(line.exercise.name) }
                        } else {
                            append(line.exercise.name)
                        }
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit) {
                    Icon(SharedIcons.Edit, null, modifier = Modifier.size(16.dp))
                    Text(strings.edit, modifier = Modifier.padding(start = 4.dp))
                }
                TextButton(onClick = { app.duplicateRoutine(routine.id) }) {
                    Icon(SharedIcons.Copy, null, modifier = Modifier.size(16.dp))
                    Text(strings.duplicate, modifier = Modifier.padding(start = 4.dp))
                }
                TextButton(onClick = onDelete) {
                    Icon(
                        SharedIcons.Delete,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        strings.delete,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}
