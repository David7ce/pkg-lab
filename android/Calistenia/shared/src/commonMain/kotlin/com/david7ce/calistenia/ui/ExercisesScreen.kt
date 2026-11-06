package com.david7ce.calistenia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.david7ce.calistenia.logic.DEFAULT_HOLD_SECONDS
import com.david7ce.calistenia.logic.DEFAULT_REPS
import com.david7ce.calistenia.logic.DEFAULT_SET_COUNT
import com.david7ce.calistenia.logic.ExerciseFamily
import com.david7ce.calistenia.logic.ExerciseProgress
import com.david7ce.calistenia.logic.MAX_HOLD_SECONDS
import com.david7ce.calistenia.logic.MAX_REPS
import com.david7ce.calistenia.logic.MAX_SETS
import com.david7ce.calistenia.logic.MIN_HOLD_SECONDS
import com.david7ce.calistenia.logic.MIN_REPS
import com.david7ce.calistenia.logic.MIN_SETS
import com.david7ce.calistenia.logic.descriptionFor
import com.david7ce.calistenia.logic.effectiveMuscles
import com.david7ce.calistenia.logic.filterFamilies
import com.david7ce.calistenia.logic.formatDuration
import com.david7ce.calistenia.logic.ProgressPoint
import com.david7ce.calistenia.model.Exercise
import com.david7ce.calistenia.model.Measure
import com.david7ce.calistenia.model.MuscleGroup
import com.david7ce.calistenia.ui.figure.Figure
import com.david7ce.calistenia.ui.figure.figureFor
import com.david7ce.shared.ui.ChoiceDropdown
import com.david7ce.shared.ui.ConfirmDialog
import com.david7ce.shared.ui.SharedIcons

@Composable
fun ExercisesScreen(app: AppState) {
    val strings = LocalStrings.current
    var query by remember { mutableStateOf("") }
    var group by remember { mutableStateOf<MuscleGroup?>(null) }
    var measure by remember { mutableStateOf<Measure?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    val favoriteIds = app.settings.favoriteExerciseIds

    val shown = remember(query, group, measure, favoritesOnly, app.exercises, app.muscleFilter, favoriteIds) {
        filterFamilies(
            app.exercises, query, group, measure, app.muscleFilter,
            favoriteIds = if (favoritesOnly) favoriteIds else null,
        )
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(strings.search) },
                leadingIcon = { Icon(SharedIcons.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        TextButton(onClick = { query = "" }) { Text(strings.clear) }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { favoritesOnly = !favoritesOnly },
                    label = { Text(strings.favorites) },
                    leadingIcon = {
                        Icon(if (favoritesOnly) SharedIcons.Star else SharedIcons.StarOutline, null)
                    },
                )
            }
            if (app.muscleFilter.isNotEmpty()) MuscleFilterRow(app)
            FilterRow(
                groups = listOf(null) + MuscleGroup.entries,
                selectedGroup = group,
                onGroup = { group = it },
                measures = listOf(null) + Measure.entries,
                selectedMeasure = measure,
                onMeasure = { measure = it },
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (shown.isEmpty()) {
                    item {
                        Text(
                            if (app.exercises.isEmpty()) strings.noExercisesYet else strings.noResults,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                        )
                    }
                } else {
                    items(shown, key = { it.parent.id }) { family ->
                        ExerciseFamilyRow(family, onClick = { app.openExercise(family.parent.id) })
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { creating = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) {
            Icon(SharedIcons.Add, strings.newExercise)
        }
    }

    if (creating) {
        ExerciseEditDialog(
            initial = null,
            onDismiss = { creating = false },
            onSave = { name, group, measure, description ->
                app.addExercise(name, group, measure, description)
                creating = false
            },
        )
    }
}

@Composable
private fun FilterRow(
    groups: List<MuscleGroup?>,
    selectedGroup: MuscleGroup?,
    onGroup: (MuscleGroup?) -> Unit,
    measures: List<Measure?>,
    selectedMeasure: Measure?,
    onMeasure: (Measure?) -> Unit,
) {
    val strings = LocalStrings.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            groups.forEach { option ->
                FilterChip(
                    selected = option == selectedGroup,
                    onClick = { onGroup(option) },
                    label = { Text(if (option == null) strings.all else strings.groupLabel(option)) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            measures.forEach { option ->
                FilterChip(
                    selected = option == selectedMeasure,
                    onClick = { onMeasure(option) },
                    label = { Text(if (option == null) strings.all else strings.measureLabel(option)) },
                )
            }
        }
    }
}

@Composable
private fun ExerciseFamilyRow(family: ExerciseFamily, onClick: () -> Unit) {
    val strings = LocalStrings.current
    val parent = family.parent
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    parent.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${strings.variantsCount(family.variants.size)} · ${strings.groupLabel(parent.group)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (parent.isCustom) Pill(strings.custom)
        }
    }
}

@Composable
fun ExerciseDetailScreen(app: AppState, exerciseId: String) {
    val strings = LocalStrings.current
    val opened = app.exerciseById(exerciseId)
    if (opened == null) {
        EmptyState(strings.noExercisesYet)
        return
    }
    val parent = app.parentExercise(exerciseId) ?: opened
    val family = app.familyOf(parent.id)
    var selectedId by remember(parent.id) { mutableStateOf(exerciseId) }
    val exercise = app.exerciseById(selectedId) ?: parent
    val progress = app.progressFor(exercise.id)
    val lastSets = app.lastSetsFor(exercise.id)
    val description = exercise.descriptionFor(app.languageCode)
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var startingSets by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (family.size > 1) {
            item {
                ChoiceDropdown(
                    options = family.map { it.id to it.name },
                    selected = exercise.id,
                    label = strings.variant,
                    leadingIcon = SharedIcons.Tune,
                    onSelect = { selectedId = it },
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Pill(strings.groupLabel(exercise.group))
                Pill(strings.measureLabel(exercise.measure))
                if (exercise.isCustom) Pill(strings.custom)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { app.toggleFavorite(exercise.id) }) {
                    Icon(
                        if (app.isFavorite(exercise.id)) SharedIcons.Star else SharedIcons.StarOutline,
                        strings.favorites,
                    )
                }
            }
        }
        val muscles = exercise.effectiveMuscles()
        if (muscles.isNotEmpty()) {
            item {
                Column {
                    Text(
                        strings.musclesWorked,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        muscles.forEach { muscle -> Pill(strings.muscleLabel(muscle)) }
                    }
                }
            }
        }
        item {
            Button(onClick = { startingSets = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(SharedIcons.Play, null)
                Text(
                    if (app.active != null) strings.addExerciseToWorkout else strings.startExercise,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        // Families without a registered figure stay text-only.
        val figure = figureFor(parent.id)
        if (figure != null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Figure(
                        animation = figure,
                        modifier = Modifier.fillMaxWidth().height(180.dp).padding(14.dp),
                    )
                }
            }
        }
        if (description.isNotBlank()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            strings.exerciseHowTo,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            description,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
        if (progress.hasData) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatTile(
                        strings.bestSet,
                        if (exercise.measure == Measure.Reps) progress.bestValue.toString()
                        else strings.secondsValue(progress.bestValue.toInt()),
                        Modifier.weight(1f),
                    )
                    StatTile(strings.doneSets, progress.totalSets.toString(), Modifier.weight(1f))
                    StatTile(
                        if (exercise.measure == Measure.Reps) strings.totalReps else strings.totalHoldTime,
                        if (exercise.measure == Measure.Reps) progress.totalValue.toString()
                        else formatDuration(progress.totalValue * 1000),
                        Modifier.weight(1f),
                    )
                    StatTile(strings.timesDone, progress.sessions.toString(), Modifier.weight(1f))
                }
            }
            item {
                Column {
                    Text(
                        strings.lastSessions,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ProgressChart(progress, Modifier.padding(top = 8.dp))
                }
            }
            item {
                Text(
                    "${strings.lastDone}: ${progress.lastPerformedAt?.let(app::formatDateTime).orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (lastSets.isNotEmpty()) {
                item {
                    Text(
                        strings.lastTime(strings.setValues(lastSets.map { it.valueLabel(strings.secondsUnit) })),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            item {
                Text(
                    strings.noProgressYet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (exercise.isCustom) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { editing = true }, modifier = Modifier.weight(1f)) {
                        Icon(SharedIcons.Edit, null)
                        Text(strings.edit, modifier = Modifier.padding(start = 8.dp))
                    }
                    OutlinedButton(onClick = { deleting = true }, modifier = Modifier.weight(1f)) {
                        Icon(SharedIcons.Delete, null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            strings.delete,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    strings.builtInNotice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (editing) {
        ExerciseEditDialog(
            initial = exercise,
            onDismiss = { editing = false },
            onSave = { name, group, measure, description ->
                app.updateExercise(exercise.id, name, group, measure, description)
                editing = false
            },
        )
    }
    if (deleting) {
        ConfirmDialog(
            title = strings.deleteExerciseTitle,
            text = strings.deleteExerciseText(exercise.name),
            confirmLabel = strings.delete,
            onConfirm = {
                app.deleteExercise(exercise.id)
                deleting = false
            },
            onDismiss = { deleting = false },
            destructive = true,
        )
    }
    if (startingSets) {
        QuickStartDialog(
            exercise = exercise,
            joiningWorkout = app.active != null,
            onConfirm = { sets, reps, seconds ->
                app.quickStart(exercise, sets, reps, seconds)
                startingSets = false
            },
            onDismiss = { startingSets = false },
        )
    }
}

/** Sets/target before training one exercise on its own, outside the routine editor. */
@Composable
private fun QuickStartDialog(
    exercise: Exercise,
    joiningWorkout: Boolean,
    onConfirm: (sets: Int, targetReps: Int?, targetSeconds: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    var sets by remember { mutableStateOf(DEFAULT_SET_COUNT) }
    var reps by remember { mutableStateOf(DEFAULT_REPS) }
    var seconds by remember { mutableStateOf(DEFAULT_HOLD_SECONDS) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (joiningWorkout) strings.addExerciseToWorkout else strings.startExercise) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stepper(
                    value = sets,
                    label = strings.targetSets,
                    onValueChange = { sets = it },
                    modifier = Modifier.weight(1f),
                    step = 1,
                    min = MIN_SETS,
                    max = MAX_SETS,
                )
                if (exercise.measure == Measure.Reps) {
                    Stepper(
                        value = reps,
                        label = strings.targetReps,
                        onValueChange = { reps = it },
                        modifier = Modifier.weight(1f),
                        step = 5,
                        min = MIN_REPS,
                        max = MAX_REPS,
                    )
                } else {
                    Stepper(
                        value = seconds,
                        label = strings.targetSeconds,
                        onValueChange = { seconds = it },
                        modifier = Modifier.weight(1f),
                        step = 5,
                        min = MIN_HOLD_SECONDS,
                        max = MAX_HOLD_SECONDS,
                        suffix = strings.secondsUnit,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (exercise.measure == Measure.Reps) onConfirm(sets, reps, null) else onConfirm(sets, null, seconds)
            }) {
                Text(if (joiningWorkout) strings.addExerciseToWorkout else strings.startExercise)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } },
    )
}

/** Bars for the last sessions. Drawn with layout boxes, so it works on every target. */
@Composable
private fun ProgressChart(progress: ExerciseProgress, modifier: Modifier = Modifier) {
    val points = progress.series
    val max = points.maxOfOrNull { it.value } ?: 0
    Row(
        modifier = modifier.fillMaxWidth().height(120.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        points.forEach { point ->
            Bar(point, max)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            progress.series.size.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            max.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Bar(point: ProgressPoint, max: Long) {
    val fraction = if (max <= 0) 0.02f else (point.value.toFloat() / max).coerceIn(0.02f, 1f)
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(fraction)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
    )
}

/** The active body-map selection: tap a chip to drop that muscle, "x" to clear, the label to reopen the map. */
@Composable
internal fun MuscleFilterRow(app: AppState) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = app::openWhatToday) { Text(strings.musclesLabel) }
        app.muscleFilter.sortedBy { it.ordinal }.forEach { muscle ->
            FilterChip(
                selected = true,
                onClick = { app.toggleMuscle(muscle) },
                label = { Text(strings.muscleLabel(muscle)) },
            )
        }
        TextButton(onClick = app::clearMuscles) { Text(strings.clear) }
    }
}
