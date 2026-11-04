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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.david7ce.calistenia.logic.MAX_NOTES
import com.david7ce.calistenia.logic.MAX_ROUTINE_NAME
import com.david7ce.calistenia.logic.RoutineDraft
import com.david7ce.calistenia.logic.RoutineDraftExercise
import com.david7ce.calistenia.logic.RoutineProblem
import com.david7ce.calistenia.logic.trainsAny
import com.david7ce.calistenia.model.Measure
import com.david7ce.shared.ui.SectionTitle
import com.david7ce.shared.ui.SharedIcons

@Composable
fun RoutineEditorScreen(app: AppState, state: RoutineEditorState) {
    val strings = LocalStrings.current
    val draft = state.draft
    var picking by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OutlinedTextField(
                value = draft.name,
                onValueChange = { name -> state.update { it.copy(name = name.take(MAX_ROUTINE_NAME)) } },
                label = { Text(strings.routineName) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = draft.notes,
                onValueChange = { notes -> state.update { it.copy(notes = notes.take(MAX_NOTES)) } },
                label = { Text(strings.routineNotes) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { SectionTitle(strings.exercises) }
        itemsIndexed(draft.exercises, key = { _, line -> line.key }) { index, line ->
            LineCard(
                line = line,
                strings = strings,
                highlighted = app.muscleFilter.isNotEmpty() && line.exercise.trainsAny(app.muscleFilter),
                canMoveUp = index > 0,
                canMoveDown = index < draft.exercises.lastIndex,
                onMoveUp = { state.update { it.swap(index, -1) } },
                onMoveDown = { state.update { it.swap(index, 1) } },
                onRemove = { state.update { it.copy(exercises = it.exercises.filterIndexed { i, _ -> i != index }) } },
                onChange = { changed -> state.update { it.replaceLine(index, changed) } },
            )
        }
        item {
            OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(SharedIcons.Add, null)
                Text(strings.addExercise, modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (state.showProblems && state.problems.isNotEmpty()) {
            item {
                Column {
                    state.problems.forEach { problem ->
                        Text(
                            when (problem) {
                                RoutineProblem.NameRequired -> strings.nameRequired
                                RoutineProblem.NoExercises -> strings.noExercisesInRoutine
                                else -> strings.invalidTarget
                            },
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        item {
            Button(
                onClick = { app.saveRoutine(state) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(strings.saveRoutine) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (picking) {
        ExercisePickerDialog(
            exercises = app.exercises,
            onPick = {
                app.addExerciseToDraft(state, it)
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun LineCard(
    line: RoutineDraftExercise,
    strings: Strings,
    highlighted: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onChange: (RoutineDraftExercise) -> Unit,
) {
    // Highlighted = trains a muscle picked on the body map; same primary tint as the map's selection.
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (highlighted) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        line.exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${strings.groupLabel(line.exercise.group)} · ${strings.measureLabel(line.exercise.measure)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(SharedIcons.Up, strings.moveUp)
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(SharedIcons.Down, strings.moveDown)
                }
                IconButton(onClick = onRemove) {
                    Icon(SharedIcons.Delete, strings.removeExercise, tint = MaterialTheme.colorScheme.error)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NumberBox(
                    value = line.sets.takeIf { it > 0 },
                    label = strings.targetSets,
                    onValueChange = { raw ->
                        onChange(line.copy(sets = raw.toIntOrNull()?.coerceIn(0, 99) ?: 0))
                    },
                    modifier = Modifier.weight(1f),
                    maxDigits = 2,
                )
                if (line.exercise.measure == Measure.Reps) {
                    NumberBox(
                        value = line.targetReps?.takeIf { it > 0 },
                        label = strings.targetReps,
                        onValueChange = { raw ->
                            onChange(line.copy(targetReps = raw.toIntOrNull(), targetSeconds = null))
                        },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    NumberBox(
                        value = line.targetSeconds?.takeIf { it > 0 },
                        label = strings.targetSeconds,
                        onValueChange = { raw ->
                            onChange(line.copy(targetSeconds = raw.toIntOrNull(), targetReps = null))
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                NumberBox(
                    value = line.restSeconds,
                    label = strings.restBetweenSets,
                    onValueChange = { raw ->
                        onChange(line.copy(restSeconds = raw.toIntOrNull()?.coerceIn(0, 600) ?: 0))
                    },
                    modifier = Modifier.weight(1f),
                    suffix = strings.secondsUnit,
                    maxDigits = 3,
                )
            }
        }
    }
}

private fun RoutineDraft.swap(index: Int, delta: Int): RoutineDraft {
    val target = index + delta
    if (index !in exercises.indices || target !in exercises.indices) return this
    val list = exercises.toMutableList()
    val item = list.removeAt(index)
    list.add(target, item)
    return copy(exercises = list)
}

private fun RoutineDraft.replaceLine(index: Int, line: RoutineDraftExercise): RoutineDraft =
    copy(exercises = exercises.mapIndexed { i, current -> if (i == index) line else current })
