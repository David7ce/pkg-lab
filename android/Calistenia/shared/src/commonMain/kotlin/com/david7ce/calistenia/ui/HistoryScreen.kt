package com.david7ce.calistenia.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.david7ce.calistenia.logic.formatDuration
import com.david7ce.calistenia.model.WorkoutSession
import com.david7ce.shared.ui.ConfirmDialog
import com.david7ce.shared.ui.SharedIcons

@Composable
fun HistoryScreen(app: AppState) {
    val strings = LocalStrings.current
    if (app.history.isEmpty()) {
        EmptyState(strings.emptyHistory)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(app.history, key = { it.id }) { session ->
            Card(onClick = { app.openSession(session.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row {
                        Text(
                            session.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            formatDuration(session.durationMillis ?: 0),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        "${app.formatDateTime(session.finishedAt ?: session.startedAt)} · " +
                            "${strings.setsCount(session.totalSets)} · ${strings.exercisesCount(session.exercises.size)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun SessionDetailScreen(app: AppState, sessionId: String) {
    val strings = LocalStrings.current
    val session = app.sessionById(sessionId)
    if (session == null) {
        EmptyState(strings.emptyHistory)
        return
    }
    var deleting by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SessionSummary(app, session) }
        items(session.exercises, key = { it.id }) { exercise ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        strings.measureLabel(exercise.measure),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        exercise.sets.filter { it.isDone }
                            .joinToString("  ·  ") { it.valueLabel(strings.secondsUnit) },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    if (exercise.notes.isNotBlank()) {
                        Text(
                            exercise.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
        if (session.notes.isNotBlank()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            strings.sessionNotes,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(session.notes, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = { deleting = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(SharedIcons.Delete, null, tint = MaterialTheme.colorScheme.error)
                Text(
                    strings.delete,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (deleting) {
        ConfirmDialog(
            title = strings.deleteSessionTitle,
            text = strings.deleteSessionText,
            confirmLabel = strings.delete,
            onConfirm = {
                app.deleteSession(session.id)
                deleting = false
            },
            onDismiss = { deleting = false },
            destructive = true,
        )
    }
}

@Composable
private fun SessionSummary(app: AppState, session: WorkoutSession) {
    val strings = LocalStrings.current
    Column {
        Text(
            strings.workoutSummary(
                formatDuration(session.durationMillis ?: 0),
                session.totalSets,
                session.exercises.size,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            app.formatDateTime(session.finishedAt ?: session.startedAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
