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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.david7ce.calistenia.logic.BuiltinRoutines
import com.david7ce.calistenia.logic.StarterRoutine
import com.david7ce.calistenia.logic.formatDuration
import com.david7ce.calistenia.logic.weekSummary
import com.david7ce.calistenia.model.Routine
import com.david7ce.calistenia.model.WorkoutSession
import com.david7ce.shared.ui.SectionTitle
import com.david7ce.shared.ui.SharedIcons

@Composable
fun TrainScreen(app: AppState) {
    val strings = LocalStrings.current
    val active = app.active
    val last = app.history.firstOrNull()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            if (active != null) ContinueCard(app, active) else StartCard(app)
        }
        item { WhatTodayCard(app) }
        item { WeekCard(app) }
        if (active == null) {
            item { SectionTitle(strings.startRoutine) }
            if (app.routines.isEmpty()) {
                item { Hint(strings.noRoutinesYet) }
            } else {
                items(app.routines.take(5), key = { it.id }) { routine -> RoutineQuickCard(app, routine) }
            }
            item { SectionTitle(strings.starterRoutines) }
            item { StarterRoutinesRow(app) }
        }
        item { SectionTitle(strings.lastSession) }
        if (last == null) {
            item { Hint(strings.noSessionsYet) }
        } else {
            item { SessionCard(app, last) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun StartCard(app: AppState) {
    val strings = LocalStrings.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(strings.readyToTrain, style = MaterialTheme.typography.titleLarge)
            Button(
                onClick = { app.startWorkout(null) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Icon(SharedIcons.Add, null)
                Text(strings.startEmptyWorkout, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun ContinueCard(app: AppState, session: WorkoutSession) {
    val strings = LocalStrings.current
    ElevatedCard(onClick = app::resumeWorkout, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(SharedIcons.Play, null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    strings.workoutInProgress,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                session.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                strings.setsDone(session.totalSets, session.exercises.sumOf { it.targetSets }),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Button(onClick = app::resumeWorkout, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Text(strings.continueWorkout)
            }
        }
    }
}

@Composable
private fun WeekCard(app: AppState) {
    val strings = LocalStrings.current
    val summary = weekSummary(app.history, app.now())
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                strings.thisWeek,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatTile(strings.timesDone, summary.sessions.toString(), Modifier.weight(1f))
                StatTile(strings.doneSets, summary.sets.toString(), Modifier.weight(1f))
                StatTile(strings.duration, formatDuration(summary.durationMillis), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RoutineQuickCard(app: AppState, routine: Routine) {
    val strings = LocalStrings.current
    Card(onClick = { app.startWorkout(routine) }, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    routine.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (routine.exercises.isEmpty()) strings.noExercisesInRoutine
                    else listsExercises(routine, strings),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(SharedIcons.Play, strings.startRoutine, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Recommended routines, explored right from the home screen instead of only from the Routines tab. */
@Composable
private fun StarterRoutinesRow(app: AppState) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(BuiltinRoutines.all, key = { it.key }) { template -> StarterRoutineCard(app, template) }
    }
}

@Composable
private fun StarterRoutineCard(app: AppState, template: StarterRoutine) {
    val strings = LocalStrings.current
    Card(onClick = { app.startStarterRoutine(template) }, modifier = Modifier.width(220.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                template.name(app.languageCode),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                template.notes(app.languageCode),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    strings.exercisesCount(template.exercises.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Icon(SharedIcons.Play, strings.startExercise, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun listsExercises(routine: Routine, strings: Strings): String {
    val first = routine.exercises.first()
    return "${strings.exercisesCount(routine.exercises.size)} · " +
        first.targetLabel(strings.secondsUnit)
}

@Composable
private fun SessionCard(app: AppState, session: WorkoutSession) {
    val strings = LocalStrings.current
    Card(onClick = { app.openSession(session.id) }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                session.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${app.formatDateTime(session.finishedAt ?: session.startedAt)} · " +
                    "${formatDuration(session.durationMillis ?: 0)} · ${strings.setsCount(session.totalSets)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

/** Entry to the body map: say what you train before choosing exercises. */
@Composable
private fun WhatTodayCard(app: AppState) {
    val strings = LocalStrings.current
    Card(onClick = app::openWhatToday, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(strings.whatTrainToday, style = MaterialTheme.typography.titleMedium)
            if (app.muscleFilter.isNotEmpty()) {
                Text(
                    app.muscleFilter.sortedBy { it.ordinal }.joinToString(" · ") { strings.muscleLabel(it) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
