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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.david7ce.calistenia.logic.RestState
import com.david7ce.calistenia.logic.descriptionFor
import com.david7ce.calistenia.logic.formatCountdown
import com.david7ce.calistenia.logic.formatDuration
import com.david7ce.calistenia.model.Measure
import com.david7ce.calistenia.model.SessionExercise
import com.david7ce.calistenia.model.WorkoutSet
import com.david7ce.shared.ui.ConfirmDialog
import com.david7ce.shared.ui.SharedIcons
import kotlinx.coroutines.delay

@Composable
fun WorkoutScreen(app: AppState) {
    val strings = LocalStrings.current
    val session = app.active
    if (session == null) {
        EmptyState(strings.emptyHistory)
        return
    }

    var now by remember(session.id) { mutableStateOf(app.now()) }
    LaunchedEffect(session.id) {
        while (true) {
            now = app.now()
            delay(1000)
        }
    }
    var picking by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<SessionExercise?>(null) }

    // Android 13+: ask for the rest-notification permission the first time a rest actually needs it,
    // which is clearer than a prompt on first launch. Desktop has no notifications (no-op).
    val platform = LocalPlatform.current
    val hasRest = app.rest != null
    LaunchedEffect(hasRest) {
        if (hasRest && !app.notificationPermissionAsked && app.settings.restNotifications &&
            platform.notificationsAvailable && !platform.notificationsGranted
        ) {
            app.notificationPermissionAsked = true
            platform.requestNotificationPermission()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    strings.elapsed,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(formatDuration(now - session.startedAt), style = MaterialTheme.typography.titleLarge)
            }
            Button(onClick = app::requestFinish) {
                Icon(SharedIcons.Check, null)
                Text(strings.finishWorkout, modifier = Modifier.padding(start = 6.dp))
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(session.exercises, key = { it.id }) { exercise ->
                WorkoutExerciseCard(
                    app = app,
                    exercise = exercise,
                    onRemove = { removing = exercise },
                )
            }
            item {
                OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(SharedIcons.Add, null)
                    Text(strings.addExerciseToWorkout, modifier = Modifier.padding(start = 8.dp))
                }
            }
            item {
                OutlinedTextField(
                    value = session.notes,
                    onValueChange = app::sessionNotes,
                    label = { Text(strings.sessionNotes) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }

        app.rest?.let { rest -> RestBar(app, rest) }
    }

    if (picking) {
        ExercisePickerDialog(
            exercises = app.exercises,
            onPick = {
                app.addExerciseToWorkout(it)
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
    removing?.let { exercise ->
        ConfirmDialog(
            title = strings.removeExercise,
            text = strings.removeExerciseText(exercise.name),
            confirmLabel = strings.removeExercise,
            onConfirm = {
                app.removeExerciseFromWorkout(exercise.id)
                removing = null
            },
            onDismiss = { removing = null },
            destructive = true,
        )
    }
}

@Composable
private fun WorkoutExerciseCard(
    app: AppState,
    exercise: SessionExercise,
    onRemove: () -> Unit,
) {
    val strings = LocalStrings.current
    val lastSets = app.lastSetsFor(exercise.exerciseId)
    val lastLabel = if (lastSets.isEmpty()) null else
        strings.lastTime(strings.setValues(lastSets.map { it.valueLabel(strings.secondsUnit) }))
    val description = app.exerciseById(exercise.exerciseId)?.descriptionFor(app.languageCode).orEmpty()
    var showInfo by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${strings.routineTarget(exercise.targetSets, exercise.targetReps, exercise.targetSeconds)} · " +
                            strings.measureLabel(exercise.measure),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (description.isNotBlank()) {
                    IconButton(onClick = { showInfo = true }) {
                        Icon(SharedIcons.Info, strings.exerciseHowTo, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(SharedIcons.Delete, strings.removeExercise, tint = MaterialTheme.colorScheme.error)
                }
            }
            if (lastLabel != null) {
                Text(
                    lastLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            exercise.sets.forEachIndexed { index, set ->
                SetRow(app, exercise, set, index)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = { app.addSet(exercise.id) }) {
                    Icon(SharedIcons.Add, null)
                    Text(strings.addSet, modifier = Modifier.padding(start = 4.dp))
                }
                TextButton(
                    onClick = { app.removeSet(exercise.id, exercise.sets.last().id) },
                    enabled = exercise.sets.size > 1,
                ) {
                    Icon(SharedIcons.Minus, null, modifier = Modifier.size(16.dp))
                    Text(strings.removeSet, modifier = Modifier.padding(start = 4.dp))
                }
            }
            OutlinedTextField(
                value = exercise.notes,
                onValueChange = { app.exerciseNotes(exercise.id, it) },
                label = { Text(strings.exerciseNotes) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(exercise.name) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        strings.exerciseHowTo,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) { Text(strings.close) }
            },
        )
    }
}

@Composable
private fun SetRow(app: AppState, exercise: SessionExercise, set: WorkoutSet, index: Int) {
    val strings = LocalStrings.current
    val value = when (exercise.measure) {
        Measure.Reps -> set.reps
        Measure.Seconds -> set.seconds
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${index + 1}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(22.dp),
        )
        OutlinedTextField(
            value = value?.toString().orEmpty(),
            onValueChange = { app.setValue(exercise.id, set.id, it) },
            placeholder = { Text("0") },
            suffix = { Text(if (exercise.measure == Measure.Reps) strings.reps else strings.secondsUnit) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            modifier = Modifier.width(132.dp),
        )
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = { app.toggleSet(exercise.id, set.id) },
            enabled = set.hasValue || set.isDone,
        ) {
            Icon(
                SharedIcons.Check,
                if (set.isDone) strings.untickSet else strings.tickSet,
                tint = if (set.isDone) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            )
        }
    }
}

@Composable
private fun RestBar(app: AppState, rest: RestState) {
    val strings = LocalStrings.current
    var now by remember(rest.endAtMillis, rest.running, rest.finished) { mutableStateOf(app.now()) }
    LaunchedEffect(rest.running, rest.endAtMillis) {
        while (rest.running) {
            now = app.now()
            delay(200)
        }
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(SharedIcons.Timer, null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    if (rest.finished) strings.restOver else formatCountdown(rest.remaining(now)),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                )
                if (rest.finished) {
                    TextButton(onClick = app::dismissRest) { Text(strings.dismiss) }
                } else {
                    IconButton(onClick = if (rest.running) app::pauseRest else app::resumeRest) {
                        Icon(if (rest.running) SharedIcons.Pause else SharedIcons.Play, if (rest.running) strings.pause else strings.resume)
                    }
                    TextButton(onClick = app::addRestTime) { Text(strings.add15) }
                    IconButton(onClick = app::skipRest) {
                        Icon(SharedIcons.Forward, strings.skip)
                    }
                }
            }
            LinearProgressIndicator(
                progress = { if (rest.finished) 1f else rest.progress(now) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}
