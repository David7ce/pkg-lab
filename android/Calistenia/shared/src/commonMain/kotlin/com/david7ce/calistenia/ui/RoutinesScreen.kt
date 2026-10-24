package com.david7ce.calistenia.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.david7ce.calistenia.model.Routine
import com.david7ce.shared.ui.ConfirmDialog
import com.david7ce.shared.ui.SharedIcons

@Composable
fun RoutinesScreen(app: AppState) {
    val strings = LocalStrings.current
    var toDelete by remember { mutableStateOf<Routine?>(null) }

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
        if (app.routines.isEmpty()) {
            item {
                Text(
                    strings.emptyRoutines,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        } else {
            items(app.routines, key = { it.id }) { routine ->
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
            Text(
                routine.exercises.take(3).joinToString(" · ") {
                    it.exercise.name
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit) { Text(strings.edit) }
                TextButton(onClick = { app.duplicateRoutine(routine.id) }) { Text(strings.duplicate) }
                TextButton(onClick = onDelete) {
                    Text(strings.delete, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
