package com.david7ce.calistenia

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.david7ce.calistenia.logic.formatDuration
import com.david7ce.calistenia.ui.AppIcons
import com.david7ce.calistenia.ui.AppState
import com.david7ce.calistenia.ui.CalisteniaTheme
import com.david7ce.calistenia.ui.HistoryScreen
import com.david7ce.calistenia.ui.ExerciseDetailScreen
import com.david7ce.calistenia.ui.ExercisesScreen
import com.david7ce.calistenia.ui.LocalPlatform
import com.david7ce.calistenia.ui.LocalStrings
import com.david7ce.calistenia.ui.NoPlatformUi
import com.david7ce.calistenia.ui.PlatformUi
import com.david7ce.calistenia.ui.RoutineEditorScreen
import com.david7ce.calistenia.ui.RoutinesScreen
import com.david7ce.calistenia.ui.Screen
import com.david7ce.calistenia.ui.SessionDetailScreen
import com.david7ce.calistenia.ui.SettingsScreen
import com.david7ce.calistenia.ui.Tab
import com.david7ce.calistenia.ui.TrainScreen
import com.david7ce.calistenia.ui.WorkoutScreen
import com.david7ce.shared.ui.ConfirmDialog
import com.david7ce.shared.ui.LocalSharedStrings
import com.david7ce.shared.ui.SharedIcons
import com.david7ce.shared.ui.SharedStrings
import com.david7ce.shared.ui.isDark
import kotlinx.coroutines.delay

/** Root of the shared UI. Each platform builds an [AppState], handles back, and calls this. */
@Composable
fun CalisteniaApp(app: AppState, platform: PlatformUi = NoPlatformUi) {
    val strings = app.strings
    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalPlatform provides platform,
        LocalSharedStrings provides SharedStrings(
            cancel = strings.cancel,
            back = strings.back,
            close = strings.close,
            delete = strings.delete,
            selectAll = strings.all,
        ),
    ) {
        CalisteniaTheme(isDark(app.settings.theme)) {
            Root(app)
        }
    }
}

@Composable
private fun Root(app: AppState) {
    val strings = LocalStrings.current
    val snackbar = remember { SnackbarHostState() }
    val message = app.message

    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            app.clearMessage()
        }
    }

    // Drives the rest countdown. The state itself is absolute-time based, so a missed tick never drifts.
    val restRunning = app.rest?.running == true
    LaunchedEffect(restRunning) {
        while (restRunning) {
            delay(200)
            app.tickRest()
        }
    }

    val screen = app.screen
    Scaffold(
        topBar = { AppTopBar(app) },
        bottomBar = { if (screen == null) AppBottomBar(app) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Surface(modifier = Modifier.fillMaxSize().padding(padding), color = MaterialTheme.colorScheme.background) {
            if (!app.loaded) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (screen) {
                    null -> when (app.tab) {
                        Tab.Train -> TrainScreen(app)
                        Tab.Routines -> RoutinesScreen(app)
                        Tab.Exercises -> ExercisesScreen(app)
                        Tab.History -> HistoryScreen(app)
                    }

                    Screen.Settings -> SettingsScreen(app)
                    Screen.Workout -> WorkoutScreen(app)
                    is Screen.RoutineEditor -> RoutineEditorScreen(app, screen.state)
                    is Screen.ExerciseDetail -> ExerciseDetailScreen(app, screen.exerciseId)
                    is Screen.SessionDetail -> SessionDetailScreen(app, screen.sessionId)
                }
            }
        }
    }

    if (app.showHelp) HelpDialog(app)
    if (app.finishConfirm) {
        ConfirmDialog(
            title = strings.finishTitle,
            text = strings.finishText,
            confirmLabel = strings.finishWorkout,
            onConfirm = app::finishWorkout,
            onDismiss = app::cancelFinish,
        )
    }
    if (app.discardConfirm) {
        ConfirmDialog(
            title = strings.discardTitle,
            text = strings.discardText,
            confirmLabel = strings.discardWorkout,
            onConfirm = app::discardWorkout,
            onDismiss = app::cancelDiscard,
            destructive = true,
        )
    }
    app.pendingImport?.let { pending ->
        ConfirmDialog(
            title = strings.importConfirmTitle,
            text = strings.importConfirmText(pending.merge.added, pending.merge.alreadyPresent),
            confirmLabel = strings.importConfirm,
            onConfirm = app::confirmImport,
            onDismiss = app::cancelImport,
        )
    }
    app.justFinished?.let { session ->
        AlertDialog(
            onDismissRequest = app::dismissFinished,
            title = { Text(strings.workoutSaved) },
            text = {
                Column {
                    Text(
                        strings.workoutSummary(
                            formatDuration(session.durationMillis ?: 0),
                            session.totalSets,
                            session.exercises.size,
                        ),
                    )
                    Text(
                        app.formatDateTime(session.finishedAt ?: session.startedAt),
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { TextButton(onClick = app::dismissFinished) { Text(strings.done) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(app: AppState) {
    val strings = LocalStrings.current
    val screen = app.screen
    val title = when (screen) {
        null -> when (app.tab) {
            Tab.Train -> strings.train
            Tab.Routines -> strings.routines
            Tab.Exercises -> strings.exercises
            Tab.History -> strings.history
        }

        Screen.Settings -> strings.settings
        Screen.Workout -> app.active?.name ?: strings.workout
        is Screen.RoutineEditor -> screen.state.draft.name.ifBlank {
            if (screen.state.isNew) strings.newRoutine else strings.routines
        }
        is Screen.ExerciseDetail -> app.exerciseById(screen.exerciseId)?.name ?: strings.exercises
        is Screen.SessionDetail -> app.sessionById(screen.sessionId)?.name ?: strings.workout
    }
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (screen != null) {
                IconButton(onClick = app::back) { Icon(SharedIcons.ArrowBack, strings.back) }
            }
        },
        actions = {
            when (screen) {
                null -> IconButton(onClick = app::openSettings) { Icon(SharedIcons.Settings, strings.settings) }
                is Screen.RoutineEditor -> IconButton(onClick = { app.saveRoutine(screen.state) }) {
                    Icon(SharedIcons.Check, strings.save)
                }

                else -> Unit
            }
        },
    )
}

@Composable
private fun AppBottomBar(app: AppState) {
    val strings = LocalStrings.current
    NavigationBar {
        NavigationBarItem(
            selected = app.tab == Tab.Train,
            onClick = { app.selectTab(Tab.Train) },
            icon = { Icon(SharedIcons.Play, null) },
            label = { Text(strings.train, maxLines = 1) },
        )
        NavigationBarItem(
            selected = app.tab == Tab.Routines,
            onClick = { app.selectTab(Tab.Routines) },
            icon = { Icon(SharedIcons.Checklist, null) },
            label = { Text(strings.routines, maxLines = 1) },
        )
        NavigationBarItem(
            selected = app.tab == Tab.Exercises,
            onClick = { app.selectTab(Tab.Exercises) },
            icon = { Icon(AppIcons.Dumbbell, null) },
            label = { Text(strings.exercises, maxLines = 1) },
        )
        NavigationBarItem(
            selected = app.tab == Tab.History,
            onClick = { app.selectTab(Tab.History) },
            icon = { Icon(SharedIcons.History, null) },
            label = { Text(strings.history, maxLines = 1) },
        )
    }
}

@Composable
private fun HelpDialog(app: AppState) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = app::closeHelp,
        title = { Text(strings.howItWorks) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                strings.helpParagraphs.forEach { paragraph ->
                    Text(
                        paragraph.replace("**", ""),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                Text(
                    strings.helpNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = app::closeHelp) { Text(strings.gotIt) } },
    )
}
