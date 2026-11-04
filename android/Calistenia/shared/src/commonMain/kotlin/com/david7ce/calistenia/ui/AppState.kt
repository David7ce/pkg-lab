package com.david7ce.calistenia.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.david7ce.calistenia.data.CalisteniaRepository
import com.david7ce.calistenia.logic.AppLanguage
import com.david7ce.calistenia.logic.AppSettings
import com.david7ce.calistenia.logic.BackupFile
import com.david7ce.calistenia.logic.BackupProblem
import com.david7ce.calistenia.logic.BackupResult
import com.david7ce.calistenia.logic.ExerciseProgress
import com.david7ce.calistenia.logic.MergeResult
import com.david7ce.calistenia.logic.RestState
import com.david7ce.calistenia.logic.RoutineDraft
import com.david7ce.calistenia.logic.StarterRoutine
import com.david7ce.calistenia.logic.addExercise
import com.david7ce.calistenia.logic.backupFileName
import com.david7ce.calistenia.logic.canFinish
import com.david7ce.calistenia.logic.encodeBackup
import com.david7ce.calistenia.logic.isValid
import com.david7ce.calistenia.logic.lastSetsFor
import com.david7ce.calistenia.logic.numberText
import com.david7ce.calistenia.logic.parentOf
import com.david7ce.calistenia.logic.parseBackup
import com.david7ce.calistenia.logic.pause
import com.david7ce.calistenia.logic.planMerge
import com.david7ce.calistenia.logic.problems
import com.david7ce.calistenia.logic.progressFor
import com.david7ce.calistenia.logic.addMillis
import com.david7ce.calistenia.logic.addSet as addSetToSession
import com.david7ce.calistenia.logic.removeSet as removeSetFromSession
import com.david7ce.calistenia.logic.resume
import com.david7ce.calistenia.logic.settingsFrom
import com.david7ce.calistenia.logic.skip
import com.david7ce.calistenia.logic.startRest as newRestState
import com.david7ce.calistenia.logic.tick
import com.david7ce.calistenia.logic.toDraft
import com.david7ce.calistenia.logic.toStored
import com.david7ce.calistenia.logic.toggled
import com.david7ce.calistenia.logic.toggleSet
import com.david7ce.calistenia.logic.updateExerciseNotes
import com.david7ce.calistenia.logic.updateNotes
import com.david7ce.calistenia.logic.updateSetValue
import com.david7ce.calistenia.model.Exercise
import com.david7ce.calistenia.model.Measure
import com.david7ce.calistenia.model.Muscle
import com.david7ce.calistenia.model.MuscleGroup
import com.david7ce.calistenia.model.Routine
import com.david7ce.calistenia.model.WorkoutSession
import com.david7ce.calistenia.model.WorkoutSet
import com.david7ce.calistenia.platform.Services
import com.david7ce.shared.ui.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The four bottom tabs. Sub-screens open on top of them. */
sealed interface Tab {
    data object Train : Tab
    data object Routines : Tab
    data object Exercises : Tab
    data object History : Tab
}

/** State of the routine editor, kept outside composition so rotation keeps the typing. */
class RoutineEditorState(initial: RoutineDraft) {
    private val original = initial
    var draft by mutableStateOf(initial)
        private set
    var showProblems by mutableStateOf(false)
        private set

    val isNew: Boolean get() = original.id == null
    val problems get() = draft.problems()
    val canSave: Boolean get() = draft.isValid

    fun update(change: (RoutineDraft) -> RoutineDraft) {
        draft = change(draft)
        if (showProblems && draft.isValid) showProblems = false
    }

    fun revealProblems() {
        showProblems = true
    }
}

sealed interface Screen {
    data object Settings : Screen
    data object Workout : Screen
    data object WhatToday : Screen
    class RoutineEditor(val state: RoutineEditorState) : Screen
    data class ExerciseDetail(val exerciseId: String) : Screen
    data class SessionDetail(val sessionId: String) : Screen
}

/** A validated backup waiting for the user to say yes. */
data class PendingImport(val file: BackupFile, val merge: MergeResult)

/**
 * All UI state and actions, shared by every platform. The process owns one instance (Android
 * Application, desktop main), so rotation keeps navigation, an in-progress workout and the rest timer.
 * Database work runs on one background thread, so writes never interleave; state changes happen on
 * the main thread, which keeps Compose and the rest timer responsive.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppState(
    private val services: Services,
    private val repo: CalisteniaRepository,
    private val scope: CoroutineScope,
    systemLanguage: String,
    val appVersion: String,
) {
    private val io: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1)

    var loaded by mutableStateOf(false)
        private set
    var systemLanguage by mutableStateOf(systemLanguage)
    var settings by mutableStateOf(AppSettings())
        private set

    /** Texts in the chosen language, or the device language when the choice is System. */
    val languageCode: String get() = settings.language.code ?: systemLanguage

    val strings: Strings get() = stringsFor(languageCode)

    var tab by mutableStateOf<Tab>(Tab.Train)
        private set
    var screen by mutableStateOf<Screen?>(null)
        private set
    /**
     * "What are you training today": the body-map selection. Deliberately not stored - it is a filter for
     * this browsing session. Empty means no filter.
     */
    var muscleFilter by mutableStateOf<Set<Muscle>>(emptySet())
        private set
    var showHelp by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    var exercises by mutableStateOf<List<Exercise>>(emptyList())
        private set
    var routines by mutableStateOf<List<Routine>>(emptyList())
        private set
    var history by mutableStateOf<List<WorkoutSession>>(emptyList())
        private set
    var active by mutableStateOf<WorkoutSession?>(null)
        private set

    var rest by mutableStateOf<RestState?>(null)
        private set

    var finishConfirm by mutableStateOf(false)
        private set
    var discardConfirm by mutableStateOf(false)
        private set
    var justFinished by mutableStateOf<WorkoutSession?>(null)
        private set

    var pendingImport by mutableStateOf<PendingImport?>(null)
        private set
    var backupWorking by mutableStateOf(false)
        private set

    /** Android 13+: the notification permission is requested once, the first time a rest needs it. */
    var notificationPermissionAsked = false

    /** Set the window flag on Android; desktop ignores it. */
    val keepScreenOn: Boolean get() = settings.keepScreenOn && active != null

    val canGoBack: Boolean get() = showHelp || screen != null

    init {
        scope.launch {
            val snapshot = runCatching { withContext(io) { loadSnapshot(seed = true) } }.getOrNull()
            if (snapshot != null) apply(snapshot, applySettings = true) else message = strings.errorSaving
            loaded = true
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Loading

    private data class Snapshot(
        val settings: Map<String, String>,
        val exercises: List<Exercise>,
        val routines: List<Routine>,
        val history: List<WorkoutSession>,
        val active: WorkoutSession?,
    )

    private fun loadSnapshot(seed: Boolean): Snapshot {
        if (seed) repo.seedBuiltins()
        return Snapshot(
            settings = repo.loadSettings(),
            exercises = repo.exercises(),
            routines = repo.routines(),
            history = repo.finishedSessions(),
            active = repo.activeSession(),
        )
    }

    private fun apply(snapshot: Snapshot, applySettings: Boolean) {
        if (applySettings) settings = settingsFrom(snapshot.settings)
        exercises = snapshot.exercises
        routines = snapshot.routines
        history = snapshot.history
        active = snapshot.active
        if (snapshot.active == null && rest?.finished != true) stopRest()
    }

    private fun reloadAll(applySettings: Boolean = false) {
        scope.launch {
            runCatching { withContext(io) { loadSnapshot(seed = false) } }
                .onSuccess { apply(it, applySettings) }
                .onFailure { message = strings.errorSaving }
        }
    }

    /** Runs a blocking database action off the main thread, one at a time. */
    private fun <T> persist(block: () -> T) = persist(block) {}

    private fun <T> persist(block: () -> T, onDone: (T) -> Unit) {
        scope.launch {
            try {
                val result = withContext(io) { block() }
                onDone(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = strings.errorSaving
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Navigation

    fun selectTab(value: Tab) {
        tab = value
        screen = null
    }

    fun openWhatToday() {
        screen = Screen.WhatToday
    }

    fun toggleMuscle(muscle: Muscle) {
        muscleFilter = muscleFilter.toggled(muscle)
    }

    fun clearMuscles() {
        muscleFilter = emptySet()
    }

    /** Leaves the picker for the filtered exercise library. */
    fun showFilteredExercises() = selectTab(Tab.Exercises)

    fun openSettings() {
        screen = Screen.Settings
    }

    fun openHelp() {
        showHelp = true
    }

    fun closeHelp() {
        showHelp = false
    }

    fun clearMessage() {
        message = null
    }

    fun dismissFinished() {
        justFinished = null
    }

    /** Closes the help first, then leaves a sub-screen. A running workout asks before discarding. */
    fun back() {
        when {
            showHelp -> showHelp = false
            screen == Screen.Workout && active != null -> discardConfirm = true
            screen != null -> screen = null
        }
    }

    fun cancelDiscard() {
        discardConfirm = false
    }

    fun openExercise(id: String) {
        screen = Screen.ExerciseDetail(id)
    }

    fun openSession(id: String) {
        screen = Screen.SessionDetail(id)
    }

    fun exerciseById(id: String): Exercise? = exercises.firstOrNull { it.id == id }
    fun routineById(id: String): Routine? = routines.firstOrNull { it.id == id }
    fun sessionById(id: String): WorkoutSession? =
        history.firstOrNull { it.id == id } ?: active?.takeIf { it.id == id }

    /** The family's standard exercise, even when [exerciseId] is one of its variants. */
    fun parentExercise(exerciseId: String): Exercise? =
        exerciseById(exerciseId)?.let { parentOf(it, exercises) }

    /** The standard exercise first, then its variants by name (one element when it has no variants). */
    fun familyOf(exerciseId: String): List<Exercise> {
        val exercise = exerciseById(exerciseId) ?: return emptyList()
        val parent = parentOf(exercise, exercises)
        return listOf(parent) + exercises
            .filter { it.parentId == parent.id && it.id != parent.id }
            .sortedBy { it.name }
    }

    fun progressFor(exerciseId: String): ExerciseProgress = progressFor(exerciseId, history)
    fun lastSetsFor(exerciseId: String): List<WorkoutSet> = lastSetsFor(exerciseId, history)

    fun now(): Long = services.clock()
    fun formatDateTime(epochMillis: Long): String = services.timeFormat.dateTime(epochMillis)
    fun formatShortDate(epochMillis: Long): String = services.timeFormat.shortDate(epochMillis)
    fun formatTime(epochMillis: Long): String = services.timeFormat.time(epochMillis)

    fun notifyExportFailed() {
        message = strings.exportFailed
    }

    fun notifyImportFailed() {
        message = strings.importFailed
    }

    /** The most recent finished session started from [routineId]. */
    fun lastUsedAt(routineId: String): Long? =
        history.firstOrNull { it.routineId == routineId }?.finishedAt

    // ---------------------------------------------------------------------------------------------
    // Settings

    fun newKey(): String = services.newId()

    private fun changeSettings(change: (AppSettings) -> AppSettings) {
        val next = change(settings)
        settings = next
        persist { next.toStored().forEach { (key, value) -> repo.saveSetting(key, value) } }
    }

    fun setTheme(value: ThemeMode) = changeSettings { it.copy(theme = value) }
    fun setLanguage(value: AppLanguage) = changeSettings { it.copy(language = value) }
    fun setDefaultRest(seconds: Int) = changeSettings { it.copy(defaultRestSeconds = seconds) }
    fun setRestNotifications(enabled: Boolean) = changeSettings { it.copy(restNotifications = enabled) }
    fun setRestSound(enabled: Boolean) = changeSettings { it.copy(restSound = enabled) }
    fun setRestVibrate(enabled: Boolean) = changeSettings { it.copy(restVibrate = enabled) }
    fun setKeepScreenOn(enabled: Boolean) = changeSettings { it.copy(keepScreenOn = enabled) }

    // ---------------------------------------------------------------------------------------------
    // Workout

    fun startWorkout(routine: Routine?) {
        if (active != null) {
            screen = Screen.Workout
            return
        }
        val name = routine?.name ?: strings.freeWorkout
        persist({ repo.startSession(name, routine) }) { session ->
            active = session
            stopRest()
            tab = Tab.Train
            screen = Screen.Workout
        }
    }

    fun resumeWorkout() {
        if (active != null) screen = Screen.Workout
    }

    fun setValue(exerciseId: String, setId: String, raw: String) {
        val current = active ?: return
        val value = numberText(raw, 4).toIntOrNull()?.takeIf { it > 0 }
        val next = current.updateSetValue(exerciseId, setId, value)
        active = next
        val stored = next.exercises.firstOrNull { it.id == exerciseId }?.sets?.firstOrNull { it.id == setId } ?: return
        persist {
            repo.updateSetValue(setId, stored.reps, stored.seconds)
            if (!stored.hasValue) repo.setCompleted(setId, null)
        }
    }

    fun toggleSet(exerciseId: String, setId: String) {
        val current = active ?: return
        val before = current.exercises.firstOrNull { it.id == exerciseId }?.sets?.firstOrNull { it.id == setId } ?: return
        val next = current.toggleSet(exerciseId, setId, services.clock())
        val after = next.exercises.firstOrNull { it.id == exerciseId }?.sets?.firstOrNull { it.id == setId } ?: return
        if (after.completedAt == before.completedAt) return
        active = next
        persist { repo.setCompleted(setId, after.completedAt) }
        if (after.isDone) {
            startRestFor(exerciseId)
        } else {
            stopRest()
        }
    }

    fun addSet(exerciseId: String) {
        val current = active ?: return
        val id = services.newId()
        active = current.addSetToSession(exerciseId, id)
        persist { repo.addSet(exerciseId, id) }
    }

    fun removeSet(exerciseId: String, setId: String) {
        val current = active ?: return
        active = current.removeSetFromSession(exerciseId, setId)
        persist { repo.removeSet(setId) }
    }

    fun addExerciseToWorkout(exercise: Exercise) {
        val current = active ?: return
        persist({
            repo.addExerciseToSession(current.id, exercise, settings.defaultRestSeconds)
            repo.session(current.id)
        }) { session -> if (session != null) active = session }
    }

    fun removeExerciseFromWorkout(sessionExerciseId: String) {
        val current = active ?: return
        active = current.copy(exercises = current.exercises.filterNot { it.id == sessionExerciseId })
        persist { repo.removeSessionExercise(sessionExerciseId) }
    }

    fun sessionNotes(text: String) {
        val current = active ?: return
        active = current.updateNotes(text)
        persist { repo.updateSessionNotes(current.id, text.take(500)) }
    }

    fun exerciseNotes(sessionExerciseId: String, text: String) {
        val current = active ?: return
        active = current.updateExerciseNotes(sessionExerciseId, text)
        persist { repo.updateSessionExerciseNotes(sessionExerciseId, text.take(500)) }
    }

    fun requestFinish() {
        if (active?.canFinish != true) {
            message = strings.oneSetToFinish
            return
        }
        finishConfirm = true
    }

    fun cancelFinish() {
        finishConfirm = false
    }

    fun finishWorkout() {
        val current = active ?: return
        finishConfirm = false
        persist({
            repo.finishSession(current.id, current.notes)
            repo.finishedSessions()
        }) { sessions ->
            history = sessions
            active = null
            tab = Tab.History
            screen = null
            stopRest()
            justFinished = sessions.firstOrNull { it.id == current.id }
        }
    }

    fun discardWorkout() {
        val current = active ?: return
        discardConfirm = false
        persist({ repo.deleteSession(current.id) }) {
            active = null
            tab = Tab.Train
            screen = null
            stopRest()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Rest timer

    private fun startRestFor(sessionExerciseId: String) {
        val seconds = active?.exercises?.firstOrNull { it.id == sessionExerciseId }?.restSeconds ?: 0
        if (seconds <= 0) {
            stopRest()
            return
        }
        rest = newRestState(seconds, services.clock())
        rest?.let(::scheduleRestNotification)
    }

    fun tickRest() {
        val current = rest ?: return
        if (!current.running) return
        val next = current.tick(services.clock())
        if (next.finished && !current.finished) services.restNotifier.cancel()
        rest = next
    }

    fun pauseRest() {
        val current = rest ?: return
        rest = current.pause(services.clock())
        services.restNotifier.cancel()
    }

    fun resumeRest() {
        val current = rest ?: return
        val next = current.resume(services.clock())
        rest = next
        scheduleRestNotification(next)
    }

    fun addRestTime() {
        val current = rest ?: return
        val next = current.addMillis(15_000, services.clock())
        rest = next
        if (next.running) {
            services.restNotifier.cancel()
            scheduleRestNotification(next)
        }
    }

    fun skipRest() {
        rest = rest?.skip()
        services.restNotifier.cancel()
    }

    fun dismissRest() {
        stopRest()
    }

    private fun stopRest() {
        rest = null
        services.restNotifier.cancel()
    }

    private fun scheduleRestNotification(state: RestState) {
        if (!services.restNotifier.canNotify || !settings.restNotifications) return
        services.restNotifier.schedule(state.remaining(services.clock()), settings.restSound, settings.restVibrate)
    }

    // ---------------------------------------------------------------------------------------------
    // Routines

    fun openNewRoutine() {
        screen = Screen.RoutineEditor(RoutineEditorState(RoutineDraft()))
    }

    fun editRoutine(id: String) {
        val routine = routineById(id) ?: return
        screen = Screen.RoutineEditor(RoutineEditorState(routine.toDraft()))
    }

    fun duplicateRoutine(id: String) {
        val routine = routineById(id) ?: return
        val draft = routine.toDraft().copy(
            id = null,
            name = strings.copyOf(routine.name),
            exercises = routine.toDraft().exercises.map { it.copy(key = services.newId()) },
        )
        screen = Screen.RoutineEditor(RoutineEditorState(draft))
    }

    fun addExerciseToDraft(state: RoutineEditorState, exercise: Exercise) {
        state.update { it.addExercise(exercise, services.newId(), settings.defaultRestSeconds) }
    }

    fun saveRoutine(state: RoutineEditorState) {
        if (!state.canSave) {
            state.revealProblems()
            return
        }
        persist({
            repo.saveRoutine(state.draft)
            repo.routines()
        }) { list ->
            routines = list
            message = strings.routineSaved
            screen = null
        }
    }

    /** Copies a starter template with a new id; its name and notes are in the current app language. */
    fun addStarterRoutine(template: StarterRoutine, onDone: () -> Unit = {}) {
        val draft = template.toDraft(exercises, languageCode, ::newKey) ?: return
        persist({
            repo.saveRoutine(draft)
            repo.routines()
        }) { list ->
            routines = list
            message = strings.routineAdded
            onDone()
        }
    }

    fun deleteRoutine(id: String) {
        persist({
            repo.deleteRoutine(id)
            repo.routines()
        }) { list ->
            routines = list
            screen = null
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Exercises

    fun addExercise(name: String, group: MuscleGroup, measure: Measure, description: String = "") {
        persist({
            repo.addExercise(name, group, measure, description)
            repo.exercises() to repo.routines()
        }) { (list, routinesList) ->
            exercises = list
            routines = routinesList
        }
    }

    fun updateExercise(id: String, name: String, group: MuscleGroup, measure: Measure, description: String) {
        persist({
            repo.updateExercise(id, name, group, measure, description)
            repo.exercises() to repo.routines()
        }) { (list, routinesList) ->
            exercises = list
            routines = routinesList
        }
    }

    fun deleteExercise(id: String) {
        if (active != null) {
            message = strings.exerciseDeleteBlocked
            return
        }
        persist({
            repo.deleteExercise(id)
            repo.exercises() to repo.routines()
        }) { (list, routinesList) ->
            exercises = list
            routines = routinesList
            screen = null
        }
    }

    // ---------------------------------------------------------------------------------------------
    // History

    fun deleteSession(id: String) {
        persist({
            repo.deleteSession(id)
            repo.finishedSessions()
        }) { list ->
            history = list
            screen = null
            message = strings.sessionDeleted
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Backup

    fun exportBackup(onReady: (fileName: String, json: String) -> Unit) {
        backupWorking = true
        scope.launch {
            try {
                val json = withContext(io) { encodeBackup(repo.backup(settings)) }
                onReady(backupFileName(services.timeFormat.isoDate(services.clock())), json)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = strings.exportFailed
            } finally {
                backupWorking = false
            }
        }
    }

    fun importBackup(bytes: ByteArray) {
        backupWorking = true
        scope.launch {
            try {
                val parsed = withContext(io) { parseBackup(bytes.decodeToString()) }
                when (parsed) {
                    is BackupResult.Invalid -> {
                        message = when (parsed.problem) {
                            BackupProblem.NewerVersion -> strings.importNewer
                            else -> strings.importFailed
                        }
                    }

                    is BackupResult.Valid -> {
                        val (existing, existingSettings) = withContext(io) {
                            repo.existingIds() to repo.loadSettings().keys
                        }
                        pendingImport = PendingImport(parsed.file, planMerge(parsed.file, existing, existingSettings))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = strings.backupFailed
            } finally {
                backupWorking = false
            }
        }
    }

    fun confirmImport() {
        val pending = pendingImport ?: return
        pendingImport = null
        persist({
            repo.importBackup(pending.merge)
            loadSnapshot(seed = false)
        }) { snapshot ->
            // Settings only arrive for keys that were not stored yet, so applying the merged
            // result immediately is safe and avoids showing old defaults until the next start.
            apply(snapshot, applySettings = true)
            message = strings.importDone(pending.merge.added)
        }
    }

    fun cancelImport() {
        pendingImport = null
    }
}
