package com.david7ce.calistenia.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.david7ce.calistenia.model.Measure
import com.david7ce.calistenia.model.Muscle
import com.david7ce.calistenia.model.MuscleGroup
import com.david7ce.calistenia.logic.targetLabel

/**
 * UI text. English is the base; each language overrides it (Translations.kt). Plain Kotlin instead of
 * a resources library, so Android and desktop show exactly the same text, a missing translation falls
 * back to English instead of crashing, and the language can change without recreating the app.
 */
open class Strings {
    open val appName = "Calistenia"

    // Navigation and common actions
    open val train = "Train"
    open val routines = "Routines"
    open val exercises = "Exercises"
    open val history = "History"
    open val settings = "Settings"
    open val back = "Back"
    open val close = "Close"
    open val cancel = "Cancel"
    open val delete = "Delete"
    open val save = "Save"
    open val add = "Add"
    open val done = "Done"
    open val edit = "Edit"
    open val duplicate = "Duplicate"
    open val search = "Search"
    open val noResults = "No matches."
    open val clear = "Clear"
    open val all = "All"
    open val confirm = "Confirm"

    // Groups and measures
    open val groupPush = "Push"
    open val groupPull = "Pull"
    open val groupLegs = "Legs"
    open val groupCore = "Core"
    open val groupSkill = "Skill"
    open val groupBody = "Body"
    open val muscleChest = "Chest"
    open val muscleLats = "Lats"
    open val muscleShoulders = "Shoulders"
    open val muscleBiceps = "Biceps"
    open val muscleTriceps = "Triceps"
    open val muscleForearms = "Forearms"
    open val muscleAbs = "Abs"
    open val muscleObliques = "Obliques"
    open val muscleLowerBack = "Lower back"
    open val muscleGlutes = "Glutes"
    open val muscleQuads = "Quads"
    open val muscleHamstrings = "Hamstrings"
    open val muscleCalves = "Calves"
    open val whatTrainToday = "What are you training today?"
    open val calendar = "Calendar"
    open val todayLabel = "Today"
    open val plannedLabel = "Planned"
    open val plannedAnything = "Something, I'll decide"
    open val routineDeletedNote = "routine deleted"
    open val nothingThisDay = "Nothing planned and nothing done this day."
    open val sessionsDoneLabel = "Done"
    open val inProgressLabel = "In progress"
    open val previousMonth = "Previous month"
    open val nextMonth = "Next month"
    open val weekdaysShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    open val monthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    open val muscleMapHint = "Tap the body or pick from the list. Nothing selected shows everything."
    open val showExercises = "Show exercises"
    open val bodyFront = "Front"
    open val bodyBack = "Back"
    open val musclesLabel = "Muscles"
    open val reps = "reps"
    open val secondsUnit = "s"
    open val hold = "Hold"
    open val custom = "Custom"
    open val builtIn = "Built-in"
    open val favorites = "Favorites"

    fun groupLabel(group: MuscleGroup): String = when (group) {
        MuscleGroup.Push -> groupPush
        MuscleGroup.Pull -> groupPull
        MuscleGroup.Legs -> groupLegs
        MuscleGroup.Core -> groupCore
        MuscleGroup.Skill -> groupSkill
        MuscleGroup.Body -> groupBody
    }

    fun muscleLabel(muscle: Muscle): String = when (muscle) {
        Muscle.Chest -> muscleChest
        Muscle.Lats -> muscleLats
        Muscle.Shoulders -> muscleShoulders
        Muscle.Biceps -> muscleBiceps
        Muscle.Triceps -> muscleTriceps
        Muscle.Forearms -> muscleForearms
        Muscle.Abs -> muscleAbs
        Muscle.Obliques -> muscleObliques
        Muscle.LowerBack -> muscleLowerBack
        Muscle.Glutes -> muscleGlutes
        Muscle.Quads -> muscleQuads
        Muscle.Hamstrings -> muscleHamstrings
        Muscle.Calves -> muscleCalves
    }

    fun measureLabel(measure: Measure): String = when (measure) {
        Measure.Reps -> reps
        Measure.Seconds -> hold
    }

    fun secondsValue(n: Int): String = "$n $secondsUnit"

    open fun setsCount(n: Int): String = if (n == 1) "1 set" else "$n sets"
    open fun exercisesCount(n: Int): String = if (n == 1) "1 exercise" else "$n exercises"
    open fun sessionsCount(n: Int): String = if (n == 1) "1 session" else "$n sessions"
    fun restValue(seconds: Int): String = if (seconds <= 0) noRest else "$seconds $secondsUnit"
    open val noRest = "None"

    /** "3 × 8" or "3 × 30 s". */
    fun routineTarget(sets: Int, reps: Int?, seconds: Int?): String =
        targetLabel(sets, reps, seconds, secondsUnit)

    /** "8 · 8 · 7" or "30 s · 25 s". */
    fun setValues(values: List<String>): String = values.joinToString(" · ")

    // Train
    open val readyToTrain = "Ready to train?"
    open val startEmptyWorkout = "Start empty workout"
    open val continueWorkout = "Continue workout"
    open val workoutInProgress = "Workout in progress"
    open val thisWeek = "Last 7 days"
    open val lastSession = "Last session"
    open val noSessionsYet = "No finished workouts yet. Your first one will show up here."
    open val startRoutine = "Start a routine"
    open val noRoutinesYet = "No routines yet. Create one in the Routines tab and it will be one tap away."
    open val newRoutine = "New routine"
    open val starterRoutines = "Starter routines"
    open val routineAdded = "Routine added"
    open val variant = "Variant"
    open fun variantsCount(n: Int) = if (n == 1) "1 variant" else "$n variants"
    open val freeWorkout = "Free workout"

    // Routines
    open val routineName = "Routine name"
    open val routineNotes = "Notes (optional)"
    open val addExercise = "Add exercise"
    open val targetSets = "Sets"
    open val targetReps = "Reps"
    open val targetSeconds = "Seconds"
    open val restBetweenSets = "Rest"
    open val moveUp = "Move up"
    open val moveDown = "Move down"
    open val removeExercise = "Remove exercise"
    open fun removeExerciseText(name: String) = "\"$name\" and its sets will be removed from this workout."
    open val deleteRoutineTitle = "Delete routine?"
    open fun deleteRoutineText(name: String) =
        "\"$name\" will be deleted. Finished workouts stay in history."
    open val saveRoutine = "Save routine"
    open val routineSaved = "Routine saved"
    open val nameRequired = "Give the routine a name."
    open val noExercisesInRoutine = "Add at least one exercise."
    open val invalidTarget = "Check the targets: sets, reps and rest must be valid."
    open fun copyOf(name: String) = "$name (copy)"
    open val emptyRoutines = "Create a routine and it will be one tap away when you train."
    open val lastUsed = "Last used"
    open val neverUsed = "Not used yet"

    // Workout
    open val elapsed = "Elapsed"
    open val finishWorkout = "Finish workout"
    open val discardWorkout = "Discard workout"
    open val finishTitle = "Finish this workout?"
    open val discardTitle = "Discard this workout?"
    open val finishText = "Only the sets you ticked will be saved."
    open val discardText = "Everything logged in this workout will be lost."
    open val workoutSaved = "Workout saved"
    open val oneSetToFinish = "Tick at least one set to finish."
    open fun setsDone(done: Int, total: Int) = "$done of $total sets"
    open fun lastTime(values: String) = "Last time: $values"
    open val addSet = "Add set"
    open val removeSet = "Remove set"
    open fun setNumber(n: Int) = "Set $n"
    open val tickSet = "Complete set"
    open val untickSet = "Mark set as not done"
    open val exerciseNotes = "Notes for this exercise"
    open val sessionNotes = "Workout notes"
    open val notesHint = "How did it feel?"
    open val addExerciseToWorkout = "Add exercise"
    open val rest = "Rest"
    open val restOver = "Rest over"
    open val pause = "Pause"
    open val resume = "Resume"
    open val skip = "Skip"
    open val add15 = "+15 s"
    open val dismiss = "Dismiss"
    open val noExercisesYet = "No exercises yet."
    open fun workoutSummary(duration: String, sets: Int, exercises: Int) =
        "$duration · $sets sets · $exercises exercises"

    // Exercises
    open val exerciseLibrary = "Exercise library"
    open val newExercise = "New exercise"
    open val editExercise = "Edit exercise"
    open val exerciseName = "Name"
    open val group = "Group"
    open val measuredIn = "Measured in"
    open val deleteExerciseTitle = "Delete exercise?"
    open fun deleteExerciseText(name: String) =
        "\"$name\" will be removed from your routines. Finished workouts keep their records."
    open val exerciseDeleteBlocked = "Finish or discard the current workout before deleting an exercise."
    open val bestSet = "Best set"
    open val doneSets = "Done sets"
    open val totalReps = "Total reps"
    open val totalHoldTime = "Total hold"
    open val timesDone = "Sessions"
    open val lastDone = "Last done"
    open val noProgressYet = "Finish a workout with this exercise to see progress here."
    open val lastSessions = "Last sessions"
    open val builtInNotice = "Built-in exercises cannot be edited or deleted."
    open val exerciseHowTo = "How to do it"
    open val musclesWorked = "Muscles worked"
    open val startExercise = "Start exercise"
    open val descriptionLabel = "Description (optional)"
    open val filterGroup = "Group"
    open val filterMeasure = "Measure"

    // History
    open val emptyHistory = "No finished workouts yet."
    open val workout = "Workout"
    open val duration = "Duration"
    open val deleteSessionTitle = "Delete workout?"
    open val deleteSessionText = "This workout will be removed from history."
    open val sessionDeleted = "Workout deleted"

    // Settings
    open val appearance = "Appearance"
    open val theme = "Theme"
    open val themeSystem = "System"
    open val themeLight = "Light"
    open val themeDark = "Dark"
    open val language = "Language"
    open val languageSystem = "System default"
    open val training = "Training"
    open val defaultRest = "Default rest"
    open val restNotifications = "Notify when rest ends"
    open val restNotificationsHint = "Android only, and only while the app is in the background."
    open val restSound = "Sound"
    open val restVibrate = "Vibration"
    open val keepScreenOn = "Keep the screen on during a workout"
    open val notificationsDenied = "Notifications are blocked in the system settings."
    open val notificationsUnavailable = "Notifications are not available on this device."
    open val data = "Data"
    open val exportBackup = "Export backup"
    open val importBackup = "Import backup"
    open val preparingFile = "Preparing the file…"
    open val exportFailed = "The file could not be saved."
    open val importFailed = "That file is not a valid Calistenia backup."
    open val importNewer = "This backup comes from a newer version of the app."
    open val importConfirmTitle = "Import backup?"
    open fun importConfirmText(added: Int, present: Int) =
        "$added new records will be added. The $present already present will be left untouched."
    open val importConfirm = "Import"
    open fun importDone(added: Int) = if (added == 1) "1 record imported." else "$added records imported."
    open val backupHint = "One JSON file with routines, custom exercises and finished workouts. Nothing leaves the device."
    open val backupFailed = "The backup could not be read."
    open val about = "About"
    open val howItWorks = "How does it work?"
    open fun versionLabel(version: String) = "Version $version"
    open val privacy = "No accounts, no network. Everything stays on this device."
    open val gotIt = "Got it"
    open val permissionDenied = "Permission denied: no notification will be shown."
    open val errorSaving = "Something could not be saved. Try again."
    open val helpParagraphs = listOf(
        "**Log the work, not the plan.** Start an empty workout or one of your routines, type the reps or seconds of each set and tick it. Only ticked sets are saved; you can edit a value afterwards.",
        "**Rest is part of the exercise.** When a set is ticked, the rest timer starts with the rest set for that exercise (or the default in Settings). Pause it, add 15 seconds or skip it. If the app is in the background on Android, a notification tells you when it is over.",
        "**Routines are reusable.** In the Routines tab, put exercises in the order you train them, set the planned sets and target, and set the rest. Starting a routine pre-fills every set, so training is just typing the numbers.",
        "**Progress is per exercise.** Open any exercise to see your best set, total sets, how many sessions you did and a bar for each of the last ten sessions.",
        "**Everything is local.** No account, no network. The backup in Settings writes one JSON file with your routines, custom exercises and finished workouts; importing it merges by id and never overwrites what you already have.",
    )
    open val helpNote =
        "Exercise names are in English, the common language of calisthenics training; the how-to descriptions follow the app language. Built-in exercises are a starting point: add your own variations whenever you need them."
}

val LocalStrings = staticCompositionLocalOf<Strings> { EnStrings }
