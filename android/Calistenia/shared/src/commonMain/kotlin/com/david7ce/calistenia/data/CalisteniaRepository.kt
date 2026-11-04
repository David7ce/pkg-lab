package com.david7ce.calistenia.data

import com.david7ce.calistenia.db.CalisteniaDatabase
import com.david7ce.calistenia.db.Session_exercise as DbSessionExercise
import com.david7ce.calistenia.db.Workout_set as DbWorkoutSet
import com.david7ce.calistenia.logic.AppSettings
import com.david7ce.calistenia.logic.BackupFile
import com.david7ce.calistenia.logic.BuiltinExercises
import com.david7ce.calistenia.logic.effectiveMuscles
import com.david7ce.calistenia.logic.ExistingIds
import com.david7ce.calistenia.logic.MergeResult
import com.david7ce.calistenia.logic.RoutineDraft
import com.david7ce.calistenia.logic.buildBackup
import com.david7ce.calistenia.logic.isValid
import com.david7ce.calistenia.logic.normalized
import com.david7ce.calistenia.model.Exercise
import com.david7ce.calistenia.model.MAX_EXERCISE_DESCRIPTION
import com.david7ce.calistenia.model.Measure
import com.david7ce.calistenia.model.Muscle
import com.david7ce.calistenia.model.MuscleGroup
import com.david7ce.calistenia.model.Routine
import com.david7ce.calistenia.model.RoutineExercise
import com.david7ce.calistenia.model.SessionExercise
import com.david7ce.calistenia.model.WorkoutSession
import com.david7ce.calistenia.model.WorkoutSet

/**
 * All reads and writes of the training log. Blocking: call from a background dispatcher (AppState
 * serializes calls on one thread, so writes never interleave). Deletes remove children explicitly:
 * the schema has no foreign keys, and history keeps its name/measure snapshots.
 */
class CalisteniaRepository(
    private val db: CalisteniaDatabase,
    private val clock: () -> Long,
    private val newId: () -> String,
) {
    private val q get() = db.calisteniaQueries

    // ---------------------------------------------------------------------------------------------
    // Exercises

    /** Inserts any built-in missing from an older install and refreshes its content and family link. */
    fun seedBuiltins() {
        val now = clock()
        db.transaction {
            BuiltinExercises.all.forEach { exercise ->
                q.insertExerciseIgnore(
                    exercise.id, exercise.name, exercise.group.name, exercise.measure.name, 1L, now,
                    exercise.description, exercise.parentId, Muscle.toCsv(exercise.muscles),
                )
                // Existing rows are not replaced, but newer content should reach them.
                q.updateBuiltinDescription(exercise.description, exercise.id)
                q.updateBuiltinParent(exercise.parentId, exercise.id)
                q.updateBuiltinMuscles(Muscle.toCsv(exercise.effectiveMuscles()), exercise.id)
            }
        }
    }

    fun exercises(): List<Exercise> = q.selectExercises().executeAsList().map { row ->
        Exercise(
            id = row.id,
            name = row.name,
            group = MuscleGroup.fromStored(row.muscleGroup),
            measure = Measure.fromStored(row.measure),
            builtin = row.builtin != 0L,
            description = row.description,
            parentId = row.parentId,
            muscles = Muscle.parseCsv(row.muscles),
        )
    }

    fun addExercise(
        name: String,
        group: MuscleGroup,
        measure: Measure,
        description: String = "",
    ): Exercise {
        val exercise = Exercise(
            id = newId(),
            name = name.trim(),
            group = group,
            measure = measure,
            builtin = false,
            description = description.trim().take(MAX_EXERCISE_DESCRIPTION),
        )
        q.insertExercise(
            exercise.id, exercise.name, group.name, measure.name, 0L, clock(), exercise.description, null,
            Muscle.toCsv(exercise.muscles),
        )
        return exercise
    }

    fun updateExercise(id: String, name: String, group: MuscleGroup, measure: Measure, description: String) {
        q.updateExercise(
            name.trim(), group.name, measure.name,
            description.trim().take(MAX_EXERCISE_DESCRIPTION), id,
        )
    }

    /** Custom exercises only. Routines lose the line; history keeps its snapshot. */
    fun deleteExercise(id: String) {
        if (q.selectExercise(id).executeAsOneOrNull()?.builtin != 0L) return
        db.transaction {
            q.deleteRoutineExercisesByExercise(id)
            q.deleteExercise(id)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Routines

    fun routines(): List<Routine> {
        val exercisesById = exercises().associateBy { it.id }
        val linesByRoutine = q.selectAllRoutineExercises().executeAsList().groupBy { it.routineId }
        return q.selectRoutines().executeAsList().map { row ->
            val lines = (linesByRoutine[row.id] ?: emptyList()).mapNotNull { line ->
                val exercise = exercisesById[line.exerciseId] ?: return@mapNotNull null
                RoutineExercise(
                    id = line.id,
                    exercise = exercise,
                    sets = line.sets.toInt(),
                    targetReps = line.targetReps?.toInt(),
                    targetSeconds = line.targetSeconds?.toInt(),
                    restSeconds = line.restSeconds.toInt(),
                )
            }
            Routine(
                id = row.id,
                name = row.name,
                notes = row.notes,
                exercises = lines,
                createdAt = row.createdAt,
                updatedAt = row.updatedAt,
            )
        }
    }

    fun routine(id: String): Routine? = routines().firstOrNull { it.id == id }

    /** Inserts or updates [draft] and replaces its lines as a whole. Returns the routine id. */
    fun saveRoutine(draft: RoutineDraft): String {
        val clean = draft.normalized()
        require(clean.isValid) { "Invalid routine: ${clean.name}" }
        val now = clock()
        val id = clean.id ?: newId()
        val existing = q.selectRoutine(id).executeAsOneOrNull()
        db.transaction {
            if (existing == null) {
                q.insertRoutine(id, clean.name, clean.notes, now, now)
            } else {
                q.updateRoutine(clean.name, clean.notes, now, id)
            }
            q.deleteRoutineExercises(id)
            clean.exercises.forEachIndexed { index, line ->
                q.insertRoutineExercise(
                    newId(), id, line.exercise.id, index.toLong(), line.sets.toLong(),
                    line.targetReps?.toLong(), line.targetSeconds?.toLong(), line.restSeconds.toLong(),
                )
            }
        }
        return id
    }

    fun deleteRoutine(id: String) {
        db.transaction {
            q.deleteRoutineExercises(id)
            q.deleteRoutine(id)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Sessions

    fun finishedSessions(): List<WorkoutSession> =
        sessionsFrom(q.selectFinishedSessions().executeAsList().map {
            SessionRow(it.id, it.name, it.routineId, it.startedAt, it.finishedAt, it.notes)
        })

    fun activeSession(): WorkoutSession? {
        val row = q.selectActiveSession().executeAsOneOrNull() ?: return null
        return session(row.id)
    }

    fun session(id: String): WorkoutSession? {
        val row = q.selectSession(id).executeAsOneOrNull() ?: return null
        return sessionsFrom(listOf(SessionRow(row.id, row.name, row.routineId, row.startedAt, row.finishedAt, row.notes)))
            .firstOrNull()
    }

    private data class SessionRow(
        val id: String,
        val name: String,
        val routineId: String?,
        val startedAt: Long,
        val finishedAt: Long?,
        val notes: String,
    )

    private fun sessionsFrom(rows: List<SessionRow>): List<WorkoutSession> {
        if (rows.isEmpty()) return emptyList()
        val exercisesBySession = q.selectAllSessionExercises().executeAsList().groupBy { it.sessionId }
        val setsByExercise = q.selectAllSets().executeAsList().groupBy { it.sessionExerciseId }
        return rows.map { row ->
            WorkoutSession(
                id = row.id,
                name = row.name,
                routineId = row.routineId,
                startedAt = row.startedAt,
                finishedAt = row.finishedAt,
                notes = row.notes,
                exercises = (exercisesBySession[row.id] ?: emptyList()).map { exercise ->
                    exercise.toModel(setsByExercise[exercise.id] ?: emptyList())
                },
            )
        }
    }

    /** Starts a workout from [routine], or an empty one when [routine] is null. Placeholder sets included. */
    fun startSession(name: String, routine: Routine?): WorkoutSession {
        val sessionId = newId()
        val now = clock()
        db.transaction {
            q.insertSession(sessionId, name, routine?.id, now, null, "")
            routine?.exercises?.forEachIndexed { index, line ->
                insertSessionExercise(
                    sessionId = sessionId,
                    position = index,
                    exercise = line.exercise,
                    targetSets = line.sets,
                    targetReps = line.targetReps,
                    targetSeconds = line.targetSeconds,
                    restSeconds = line.restSeconds,
                )
            }
        }
        return requireNotNull(session(sessionId)) { "Session $sessionId was not stored" }
    }

    fun addExerciseToSession(sessionId: String, exercise: Exercise, restSeconds: Int) {
        val position = q.selectSessionExercises(sessionId).executeAsList().size
        db.transaction {
            insertSessionExercise(
                sessionId = sessionId,
                position = position,
                exercise = exercise,
                targetSets = 3,
                targetReps = if (exercise.measure == Measure.Reps) 8 else null,
                targetSeconds = if (exercise.measure == Measure.Seconds) 30 else null,
                restSeconds = restSeconds,
            )
        }
    }

    private fun insertSessionExercise(
        sessionId: String,
        position: Int,
        exercise: Exercise,
        targetSets: Int,
        targetReps: Int?,
        targetSeconds: Int?,
        restSeconds: Int,
    ) {
        val rowId = newId()
        q.insertSessionExercise(
            rowId, sessionId, exercise.id, exercise.name, exercise.measure.name, position.toLong(),
            targetSets.toLong(), targetReps?.toLong(), targetSeconds?.toLong(), restSeconds.toLong(), "",
        )
        repeat(targetSets.coerceAtLeast(1)) { setIndex ->
            q.insertSet(newId(), rowId, setIndex.toLong(), null, null, null)
        }
    }

    fun removeSessionExercise(id: String) {
        db.transaction {
            q.deleteSetsBySessionExercise(id)
            q.deleteSessionExercise(id)
        }
    }

    fun addSet(sessionExerciseId: String, setId: String) {
        val position = q.selectSets(sessionExerciseId).executeAsList().size
        q.insertSet(setId, sessionExerciseId, position.toLong(), null, null, null)
    }

    fun removeSet(setId: String) {
        q.deleteSet(setId)
    }

    fun updateSetValue(setId: String, reps: Int?, seconds: Int?) {
        q.updateSetValue(reps?.toLong(), seconds?.toLong(), setId)
    }

    fun setCompleted(setId: String, at: Long?) {
        q.setSetCompleted(at, setId)
    }

    fun updateSessionNotes(id: String, notes: String) {
        q.updateSessionNotes(notes, id)
    }

    fun updateSessionExerciseNotes(id: String, notes: String) {
        q.updateSessionExerciseNotes(notes, id)
    }

    /** Finishes [id], dropping sets that were never ticked and exercises left with no sets. */
    fun finishSession(id: String, notes: String) {
        val now = clock()
        db.transaction {
            q.deleteUnfinishedSets(id)
            q.deleteEmptySessionExercises(id)
            q.finishSession(now, notes, id)
        }
    }

    fun deleteSession(id: String) {
        db.transaction {
            q.deleteSetsBySession(id)
            q.deleteSessionExercises(id)
            q.deleteSession(id)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Settings

    fun loadSettings(): Map<String, String> =
        q.selectSettings().executeAsList().associate { it.name to it.content }

    fun saveSetting(name: String, value: String) {
        q.upsertSetting(name, value)
    }

    // ---------------------------------------------------------------------------------------------
    // Backup

    fun backup(settings: AppSettings): BackupFile =
        buildBackup(exercises(), routines(), finishedSessions(), settings, clock())

    fun existingIds(): ExistingIds = ExistingIds(
        exercises = q.selectExercises().executeAsList().map { it.id }.toSet(),
        routines = q.selectRoutines().executeAsList().map { it.id }.toSet(),
        routineExercises = q.selectAllRoutineExercises().executeAsList().map { it.id }.toSet(),
        sessions = q.selectFinishedSessions().executeAsList().map { it.id }.toSet(),
        sessionExercises = q.selectAllSessionExercises().executeAsList().map { it.id }.toSet(),
        sets = q.selectAllSets().executeAsList().map { it.id }.toSet(),
    )

    /** One transaction: exercises, routines with lines, sessions with exercises and sets, new settings. */
    fun importBackup(merge: MergeResult) {
        db.transaction {
            merge.exercises.forEach {
                q.insertExercise(
                    it.id, it.name, it.group, it.measure, if (it.builtin) 1L else 0L, clock(),
                    it.description.take(MAX_EXERCISE_DESCRIPTION), it.parentId, Muscle.toCsv(Muscle.fromNames(it.muscles)),
                )
            }
            merge.routines.forEach { routine ->
                q.insertRoutine(routine.id, routine.name, routine.notes, routine.createdAt, routine.updatedAt)
                routine.exercises.forEach { line ->
                    q.insertRoutineExercise(
                        line.id, routine.id, line.exerciseId, line.position.toLong(), line.sets.toLong(),
                        line.targetReps?.toLong(), line.targetSeconds?.toLong(), line.restSeconds.toLong(),
                    )
                }
            }
            merge.sessions.forEach { session ->
                q.insertSession(session.id, session.name, session.routineId, session.startedAt, session.finishedAt, session.notes)
                session.exercises.forEach { exercise ->
                    q.insertSessionExercise(
                        exercise.id, session.id, exercise.exerciseId, exercise.name, exercise.measure,
                        exercise.position.toLong(), exercise.targetSets.toLong(), exercise.targetReps?.toLong(),
                        exercise.targetSeconds?.toLong(), exercise.restSeconds.toLong(), exercise.notes,
                    )
                    exercise.sets.forEach { set ->
                        q.insertSet(
                            set.id, exercise.id, set.position.toLong(),
                            set.reps?.toLong(), set.seconds?.toLong(), set.completedAt,
                        )
                    }
                }
            }
            merge.settings.forEach { (key, value) -> q.upsertSetting(key, value) }
        }
    }
}

private fun DbSessionExercise.toModel(sets: List<DbWorkoutSet>): SessionExercise = SessionExercise(
    id = id,
    exerciseId = exerciseId,
    name = name,
    measure = Measure.fromStored(measure),
    notes = notes,
    targetSets = targetSets.toInt(),
    targetReps = targetReps?.toInt(),
    targetSeconds = targetSeconds?.toInt(),
    restSeconds = restSeconds.toInt(),
    sets = sets.map {
        WorkoutSet(id = it.id, reps = it.reps?.toInt(), seconds = it.seconds?.toInt(), completedAt = it.completedAt)
    },
)
