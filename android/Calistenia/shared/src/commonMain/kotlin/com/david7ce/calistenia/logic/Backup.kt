package com.david7ce.calistenia.logic

import com.david7ce.calistenia.model.Exercise
import com.david7ce.calistenia.model.MAX_EXERCISE_DESCRIPTION
import com.david7ce.calistenia.model.Measure
import com.david7ce.calistenia.model.Muscle
import com.david7ce.calistenia.model.MuscleGroup
import com.david7ce.calistenia.model.Routine
import com.david7ce.calistenia.model.WorkoutSession
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The backup file: plain JSON, human-readable, one file for everything (no photos or files exist).
 * Only finished sessions are exported; an in-progress workout is transient. Built-in exercises are
 * exported too, so a backup still resolves every reference if a future version drops one.
 */
@Serializable
data class BackupFile(
    val format: String,
    val version: Int,
    val exportedAt: Long,
    val app: String,
    val exercises: List<ExerciseDto> = emptyList(),
    val routines: List<RoutineDto> = emptyList(),
    val sessions: List<SessionDto> = emptyList(),
    val settings: Map<String, String> = emptyMap(),
)

@Serializable
data class ExerciseDto(
    val id: String,
    val name: String,
    val group: String,
    val measure: String,
    val builtin: Boolean = false,
    /** v2; older backups have no field and decode to "". */
    val description: String = "",
    /** v3; the family's standard exercise, or null for a top-level one. */
    val parentId: String? = null,
    /** v4; curated Muscle names. Unknown ones are dropped on import, not rejected. */
    val muscles: List<String> = emptyList(),
)

@Serializable
data class RoutineDto(
    val id: String,
    val name: String,
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val exercises: List<RoutineExerciseDto> = emptyList(),
)

@Serializable
data class RoutineExerciseDto(
    val id: String,
    val exerciseId: String,
    val position: Int,
    val sets: Int,
    val targetReps: Int? = null,
    val targetSeconds: Int? = null,
    val restSeconds: Int,
)

@Serializable
data class SessionDto(
    val id: String,
    val name: String,
    val routineId: String? = null,
    val startedAt: Long,
    val finishedAt: Long,
    val notes: String = "",
    val exercises: List<SessionExerciseDto> = emptyList(),
)

@Serializable
data class SessionExerciseDto(
    val id: String,
    val exerciseId: String,
    val name: String,
    val measure: String,
    val position: Int,
    val targetSets: Int = DEFAULT_SET_COUNT,
    val targetReps: Int? = null,
    val targetSeconds: Int? = null,
    val restSeconds: Int = DEFAULT_REST_SECONDS,
    val notes: String = "",
    val sets: List<SetDto> = emptyList(),
)

@Serializable
data class SetDto(
    val id: String,
    val position: Int,
    val reps: Int? = null,
    val seconds: Int? = null,
    val completedAt: Long? = null,
)

const val BACKUP_FORMAT = "calistenia"
const val BACKUP_VERSION = 3
const val MAX_BACKUP_CHARS = 10 * 1024 * 1024
const val MAX_EXERCISES = 2_000
const val MAX_ROUTINES = 2_000
const val MAX_SESSIONS = 20_000
const val MAX_TOTAL_SETS = 200_000

val backupJson = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
}

fun encodeBackup(file: BackupFile): String = backupJson.encodeToString(BackupFile.serializer(), file)

fun backupFileName(isoDate: String): String = "calistenia-backup-$isoDate.json"

sealed interface BackupResult {
    data class Valid(val file: BackupFile) : BackupResult
    data class Invalid(val problem: BackupProblem) : BackupResult
}

enum class BackupProblem {
    TooLarge,
    NotJson,
    WrongFormat,
    NewerVersion,
    DuplicateId,
    InvalidExercise,
    InvalidRoutine,
    InvalidSession,
    TooManyRows,
}

/** Reads and validates the whole file before anything is written. */
fun parseBackup(text: String): BackupResult {
    if (text.length > MAX_BACKUP_CHARS) return BackupResult.Invalid(BackupProblem.TooLarge)
    val file = try {
        backupJson.decodeFromString(BackupFile.serializer(), text)
    } catch (e: Exception) {
        return BackupResult.Invalid(BackupProblem.NotJson)
    }
    if (file.format != BACKUP_FORMAT) return BackupResult.Invalid(BackupProblem.WrongFormat)
    if (file.version > BACKUP_VERSION) return BackupResult.Invalid(BackupProblem.NewerVersion)
    if (file.version < 1) return BackupResult.Invalid(BackupProblem.WrongFormat)
    if (file.exercises.size > MAX_EXERCISES || file.routines.size > MAX_ROUTINES || file.sessions.size > MAX_SESSIONS) {
        return BackupResult.Invalid(BackupProblem.TooManyRows)
    }
    val totalSets = file.sessions.sumOf { session -> session.exercises.sumOf { it.sets.size } }
    if (totalSets > MAX_TOTAL_SETS) return BackupResult.Invalid(BackupProblem.TooManyRows)

    if (file.exercises.map { it.id }.toSet().size != file.exercises.size ||
        file.routines.map { it.id }.toSet().size != file.routines.size ||
        file.sessions.map { it.id }.toSet().size != file.sessions.size
    ) return BackupResult.Invalid(BackupProblem.DuplicateId)

    val exerciseIds = file.exercises.map { it.id }.toSet()
    if (file.exercises.any {
            it.id.isBlank() || it.name.isBlank() || it.id.length > 200 || it.name.length > 200 ||
                it.description.length > MAX_EXERCISE_DESCRIPTION ||
                it.parentId != null && (it.parentId.isBlank() || it.parentId == it.id)
        }
    ) {
        return BackupResult.Invalid(BackupProblem.InvalidExercise)
    }
    if (file.exercises.any {
            !MuscleGroup.entries.any { group -> group.name.equals(it.group, true) } ||
                !Measure.entries.any { measure -> measure.name.equals(it.measure, true) }
        }
    ) return BackupResult.Invalid(BackupProblem.InvalidExercise)

    // A variant's parent must be in the file or in the built-in catalog (older backups have none).
    if (file.exercises.any { exercise ->
            exercise.parentId != null &&
                exercise.parentId !in exerciseIds &&
                BuiltinExercises.find(exercise.parentId) == null
        }
    ) return BackupResult.Invalid(BackupProblem.InvalidExercise)

    val routineLineIds = mutableSetOf<String>()
    for (routine in file.routines) {
        if (routine.id.isBlank() || routine.name.isBlank()) return BackupResult.Invalid(BackupProblem.InvalidRoutine)
        for (line in routine.exercises) {
            if (!routineLineIds.add(line.id)) return BackupResult.Invalid(BackupProblem.DuplicateId)
            if (line.id.isBlank() || line.sets !in MIN_SETS..MAX_SETS) {
                return BackupResult.Invalid(BackupProblem.InvalidRoutine)
            }
            if (line.restSeconds !in 0..MAX_REST_SECONDS) return BackupResult.Invalid(BackupProblem.InvalidRoutine)
            if (line.targetReps != null && line.targetReps !in 0..MAX_REPS) {
                return BackupResult.Invalid(BackupProblem.InvalidRoutine)
            }
            if (line.targetSeconds != null && line.targetSeconds !in 0..MAX_HOLD_SECONDS) {
                return BackupResult.Invalid(BackupProblem.InvalidRoutine)
            }
            val known = line.exerciseId in exerciseIds || BuiltinExercises.find(line.exerciseId) != null
            if (!known) return BackupResult.Invalid(BackupProblem.InvalidRoutine)
        }
    }

    val sessionExerciseIds = mutableSetOf<String>()
    val setIds = mutableSetOf<String>()
    for (session in file.sessions) {
        if (session.id.isBlank() || session.name.isBlank()) return BackupResult.Invalid(BackupProblem.InvalidSession)
        if (session.finishedAt < session.startedAt) return BackupResult.Invalid(BackupProblem.InvalidSession)
        if (session.startedAt < 0) return BackupResult.Invalid(BackupProblem.InvalidSession)
        for (exercise in session.exercises) {
            if (!sessionExerciseIds.add(exercise.id)) return BackupResult.Invalid(BackupProblem.DuplicateId)
            if (exercise.id.isBlank() || exercise.name.isBlank()) return BackupResult.Invalid(BackupProblem.InvalidSession)
            if (!Measure.entries.any { measure -> measure.name.equals(exercise.measure, true) }) {
                return BackupResult.Invalid(BackupProblem.InvalidSession)
            }
            if (exercise.targetSets !in MIN_SETS..MAX_SETS || exercise.restSeconds !in 0..MAX_REST_SECONDS) {
                return BackupResult.Invalid(BackupProblem.InvalidSession)
            }
            if (exercise.targetReps != null && exercise.targetReps !in 0..MAX_REPS) {
                return BackupResult.Invalid(BackupProblem.InvalidSession)
            }
            if (exercise.targetSeconds != null && exercise.targetSeconds !in 0..MAX_HOLD_SECONDS) {
                return BackupResult.Invalid(BackupProblem.InvalidSession)
            }
            for (set in exercise.sets) {
                if (!setIds.add(set.id)) return BackupResult.Invalid(BackupProblem.DuplicateId)
                if (set.id.isBlank() || set.reps != null && set.seconds != null) {
                    return BackupResult.Invalid(BackupProblem.InvalidSession)
                }
                if (set.completedAt != null && set.reps == null && set.seconds == null) {
                    return BackupResult.Invalid(BackupProblem.InvalidSession)
                }
                if ((set.reps ?: 0) < 0 || (set.seconds ?: 0) < 0) {
                    return BackupResult.Invalid(BackupProblem.InvalidSession)
                }
            }
        }
    }
    return BackupResult.Valid(file)
}

/** Ids already present, so a merge never overwrites or duplicates anything. */
data class ExistingIds(
    val exercises: Set<String> = emptySet(),
    val routines: Set<String> = emptySet(),
    val routineExercises: Set<String> = emptySet(),
    val sessions: Set<String> = emptySet(),
    val sessionExercises: Set<String> = emptySet(),
    val sets: Set<String> = emptySet(),
)

/**
 * What an import will insert, computed before touching the database. Existing top-level entities are
 * skipped with all their children, so a re-import adds nothing and never orphans rows.
 */
data class MergeResult(
    val exercises: List<ExerciseDto>,
    val routines: List<RoutineDto>,
    val sessions: List<SessionDto>,
    val settings: Map<String, String>,
    val added: Int,
    val alreadyPresent: Int,
) {
    val isEmpty: Boolean get() = added == 0
}

fun planMerge(file: BackupFile, existing: ExistingIds, existingSettings: Set<String>): MergeResult {
    val newExercises = file.exercises.filterNot { it.id in existing.exercises }
    val newRoutines = file.routines.filterNot { it.id in existing.routines }
    val newSessions = file.sessions.filterNot { it.id in existing.sessions }

    val knownSettings = setOf(
        SettingsKey.THEME, SettingsKey.LANGUAGE, SettingsKey.DEFAULT_REST,
        SettingsKey.REST_NOTIFICATIONS, SettingsKey.REST_SOUND, SettingsKey.REST_VIBRATE,
        SettingsKey.KEEP_SCREEN_ON,
    )
    val settings = file.settings.filterKeys { it in knownSettings && it !in existingSettings }

    return MergeResult(
        exercises = newExercises,
        routines = newRoutines,
        sessions = newSessions,
        settings = settings,
        added = newExercises.size + newRoutines.size + newSessions.size,
        alreadyPresent = (file.exercises.size - newExercises.size) +
            (file.routines.size - newRoutines.size) + (file.sessions.size - newSessions.size),
    )
}

// ---------------------------------------------------------------------------------------------
// Domain → file

fun Exercise.toDto() = ExerciseDto(
    id = id, name = name, group = group.name, measure = measure.name, builtin = builtin,
    description = description, parentId = parentId,
    muscles = Muscle.entries.filter { it in muscles }.map { it.name },
)

fun Routine.toDto(): RoutineDto = RoutineDto(
    id = id,
    name = name,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    exercises = exercises.mapIndexed { index, line ->
        RoutineExerciseDto(
            id = line.id,
            exerciseId = line.exercise.id,
            position = index,
            sets = line.sets,
            targetReps = line.targetReps,
            targetSeconds = line.targetSeconds,
            restSeconds = line.restSeconds,
        )
    },
)

fun WorkoutSession.toDto(): SessionDto? {
    val end = finishedAt ?: return null
    return SessionDto(
        id = id,
        name = name,
        routineId = routineId,
        startedAt = startedAt,
        finishedAt = end,
        notes = notes,
        exercises = exercises.mapIndexed { index, exercise ->
            SessionExerciseDto(
                id = exercise.id,
                exerciseId = exercise.exerciseId,
                name = exercise.name,
                measure = exercise.measure.name,
                position = index,
                targetSets = exercise.targetSets,
                targetReps = exercise.targetReps,
                targetSeconds = exercise.targetSeconds,
                restSeconds = exercise.restSeconds,
                notes = exercise.notes,
                sets = exercise.sets.mapIndexed { setIndex, set ->
                    SetDto(
                        id = set.id,
                        position = setIndex,
                        reps = set.reps,
                        seconds = set.seconds,
                        completedAt = set.completedAt,
                    )
                },
            )
        },
    )
}

fun buildBackup(
    exercises: List<Exercise>,
    routines: List<Routine>,
    sessions: List<WorkoutSession>,
    settings: AppSettings,
    now: Long,
): BackupFile = BackupFile(
    format = BACKUP_FORMAT,
    version = BACKUP_VERSION,
    exportedAt = now,
    app = "Calistenia",
    exercises = exercises.map { it.toDto() },
    routines = routines.map { it.toDto() },
    sessions = sessions.mapNotNull { it.toDto() },
    settings = settings.toStored(),
)

// ---------------------------------------------------------------------------------------------
// File → domain rows (used by the repository inside one transaction)

fun ExerciseDto.toModel() = Exercise(
    id = id,
    name = name,
    group = MuscleGroup.fromStored(group),
    measure = Measure.fromStored(measure),
    builtin = builtin,
    description = description.take(MAX_EXERCISE_DESCRIPTION),
    parentId = parentId,
    muscles = Muscle.fromNames(muscles),
)
