package com.david7ce.calistenia

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.david7ce.calistenia.data.CalisteniaRepository
import com.david7ce.calistenia.db.CalisteniaDatabase
import com.david7ce.calistenia.logic.BackupResult
import com.david7ce.calistenia.logic.BuiltinExercises
import com.david7ce.calistenia.logic.RoutineDraft
import com.david7ce.calistenia.logic.RoutineDraftExercise
import com.david7ce.calistenia.logic.encodeBackup
import com.david7ce.calistenia.logic.parseBackup
import com.david7ce.calistenia.logic.planMerge
import com.david7ce.calistenia.logic.settingsFrom
import com.david7ce.calistenia.logic.toStored
import com.david7ce.calistenia.model.Measure
import com.david7ce.calistenia.model.MuscleGroup
import com.david7ce.shared.ui.ThemeMode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The repository against real SQLite on the JVM, not mocks. */
class RepositoryTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repo: CalisteniaRepository
    private var ids = 0
    private var clock = 1_000L

    private val pushUp get() = BuiltinExercises.find("builtin:push-up")!!
    private val plank get() = BuiltinExercises.find("builtin:plank")!!

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CalisteniaDatabase.Schema.create(driver)
        repo = CalisteniaRepository(
            db = CalisteniaDatabase(driver),
            clock = { clock },
            newId = { "id-${++ids}" },
        )
        repo.seedBuiltins()
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun draft(
        name: String = "Push day",
        steps: List<RoutineDraftExercise> = listOf(
            RoutineDraftExercise(key = "k1", exercise = pushUp, sets = 3, targetReps = 8, restSeconds = 90),
        ),
        id: String? = null,
    ) = RoutineDraft(id = id, name = name, exercises = steps)

    private fun seedFinishedWorkout(): String {
        val routineId = repo.saveRoutine(draft())
        val session = repo.startSession("Push day", repo.routine(routineId))
        val exercise = repo.session(session.id)!!.exercises.single()
        val set = exercise.sets.first()
        repo.updateSetValue(set.id, 12, null)
        repo.setCompleted(set.id, 2_000)
        repo.finishSession(session.id, "Felt good")
        return session.id
    }

    @Test
    fun seedingIsIdempotent() {
        val count = repo.exercises().size
        assertTrue(count >= 30)
        repo.seedBuiltins()
        assertEquals(count, repo.exercises().size)
        assertTrue(repo.exercises().all { it.builtin })
        assertTrue(repo.exercises().all { it.description.length >= 100 })
        assertEquals(23, repo.exercises().count { it.isTopLevel })
        assertEquals("builtin:push-up", repo.exercises().first { it.id == "builtin:knee-push-up" }.parentId)
    }

    @Test
    fun customExercisesCanBeAddedEditedAndDeleted() {
        val custom = repo.addExercise(
            "Front lever row", MuscleGroup.Pull, Measure.Reps,
            description = "Hang under a bar and pull your chest up while keeping the body straight.",
        )
        assertTrue(custom.isCustom)
        assertEquals("Hang under a bar and pull your chest up while keeping the body straight.", custom.description)

        repo.updateExercise(
            custom.id, "Front lever row (feet)", MuscleGroup.Pull, Measure.Seconds,
            description = "Keep the heels on the floor and pull slowly.",
        )
        val updated = repo.exercises().first { it.id == custom.id }
        assertEquals("Front lever row (feet)", updated.name)
        assertEquals(Measure.Seconds, updated.measure)
        assertEquals("Keep the heels on the floor and pull slowly.", updated.description)

        repo.deleteExercise(custom.id)
        assertNull(repo.exercises().firstOrNull { it.id == custom.id })
    }

    @Test
    fun builtInsCannotBeDeleted() {
        repo.deleteExercise(pushUp.id)
        assertNotNull(repo.exercises().firstOrNull { it.id == pushUp.id })
    }

    @Test
    fun deletingAnExerciseRemovesItFromRoutines() {
        val custom = repo.addExercise("Lever row", MuscleGroup.Pull, Measure.Reps)
        val routineId = repo.saveRoutine(
            draft(steps = listOf(RoutineDraftExercise(key = "c", exercise = custom, sets = 3, targetReps = 6))),
        )
        assertEquals(1, repo.routine(routineId)!!.exercises.size)

        repo.deleteExercise(custom.id)
        assertTrue(repo.routine(routineId)!!.exercises.isEmpty())
    }

    @Test
    fun routinesAreSavedAndTheirLinesReplaced() {
        val routineId = repo.saveRoutine(draft())
        val saved = repo.routine(routineId)!!
        assertEquals("Push day", saved.name)
        assertEquals(listOf(pushUp.id), saved.exercises.map { it.exercise.id })

        clock = 5_000
        repo.saveRoutine(
            draft(
                id = routineId,
                name = "Push day v2",
                steps = listOf(
                    RoutineDraftExercise(key = "a", exercise = pushUp, sets = 5, targetReps = 10, restSeconds = 60),
                    RoutineDraftExercise(key = "b", exercise = plank, sets = 3, targetSeconds = 45, restSeconds = 45),
                ),
            ),
        )
        val updated = repo.routine(routineId)!!
        assertEquals("Push day v2", updated.name)
        assertEquals(2, updated.exercises.size)
        assertEquals(listOf(pushUp.id, plank.id), updated.exercises.map { it.exercise.id })
        assertEquals(5_000L, updated.updatedAt)
    }

    @Test
    fun startingARoutinePreFillsItsSetsAndIsResumable() {
        val routineId = repo.saveRoutine(
            draft(
                steps = listOf(
                    RoutineDraftExercise(key = "a", exercise = pushUp, sets = 4, targetReps = 8, restSeconds = 90),
                ),
            ),
        )
        val session = repo.startSession("Push day", repo.routine(routineId))
        assertEquals(1, session.exercises.size)
        assertEquals(4, session.exercises.single().sets.size)
        assertTrue(session.exercises.single().sets.all { it.reps == null })
        assertNull(session.finishedAt)

        clock = 9_000
        assertEquals(session.id, repo.activeSession()?.id)
    }

    @Test
    fun finishingKeepsOnlyTickedSetsAndExercises() {
        val session = repo.startSession("Free workout", null)
        repo.addExerciseToSession(session.id, pushUp, restSeconds = 60)
        repo.addExerciseToSession(session.id, plank, restSeconds = 60)

        val reloaded = repo.session(session.id)!!
        assertEquals(2, reloaded.exercises.size)
        val pushSets = reloaded.exercises.first { it.exerciseId == pushUp.id }.sets
        repo.updateSetValue(pushSets[0].id, 12, null)
        repo.setCompleted(pushSets[0].id, 2_000)
        repo.updateSetValue(pushSets[1].id, 10, null)
        repo.setCompleted(pushSets[1].id, 3_000)
        // The plank exercise is left untouched and must not reach history.

        repo.finishSession(session.id, "Felt good")

        val finished = repo.finishedSessions().single()
        assertEquals(session.id, finished.id)
        assertEquals("Felt good", finished.notes)
        assertEquals(1, finished.exercises.size)
        assertEquals(listOf(12, 10), finished.exercises.single().sets.map { it.reps })
        assertEquals(2, finished.totalSets)
        assertNull(repo.activeSession())
    }

    @Test
    fun historySurvivesDeletingTheRoutine() {
        val sessionId = seedFinishedWorkout()
        val routineId = repo.routines().single().id
        repo.deleteRoutine(routineId)

        val session = repo.finishedSessions().first { it.id == sessionId }
        assertEquals("Push day", session.name)
        assertEquals(routineId, session.routineId)
        assertEquals("Push-up", session.exercises.single().name)
        assertNull(repo.routine(routineId))
    }

    @Test
    fun historySurvivesDeletingACustomExercise() {
        val custom = repo.addExercise("Lever row", MuscleGroup.Pull, Measure.Reps)
        val session = repo.startSession("Free workout", null)
        repo.addExerciseToSession(session.id, custom, restSeconds = 60)
        val set = repo.session(session.id)!!.exercises.single().sets.first()
        repo.updateSetValue(set.id, 6, null)
        repo.setCompleted(set.id, 2_000)
        repo.finishSession(session.id, "")

        repo.deleteExercise(custom.id)

        val finished = repo.finishedSessions().single()
        assertEquals("Lever row", finished.exercises.single().name)
        assertNull(repo.exercises().firstOrNull { it.id == custom.id })
    }

    @Test
    fun deletingASessionRemovesItsChildren() {
        val sessionId = seedFinishedWorkout()
        repo.deleteSession(sessionId)
        assertTrue(repo.finishedSessions().isEmpty())
        assertNull(repo.session(sessionId))
    }

    @Test
    fun settingsRoundTrip() {
        repo.saveSetting("theme", "Dark")
        repo.saveSetting("language", "Es")
        val settings = settingsFrom(repo.loadSettings())
        assertEquals(ThemeMode.Dark, settings.theme)
        assertEquals("Es", settings.language.name)
        assertEquals(90, settings.defaultRestSeconds)
    }

    @Test
    fun backupRestoresIntoAFreshDatabaseAndImportingTwiceChangesNothing() {
        seedFinishedWorkout()
        val routineName = repo.routines().single().name
        val custom = repo.addExercise(
            "Front lever row", MuscleGroup.Pull, Measure.Reps,
            description = "Hang under a bar and pull your chest up, keeping the body straight.",
        )
        repo.saveSetting("theme", "Dark")

        val exported = repo.backup(settingsFrom(repo.loadSettings()))
        val parsed = parseBackup(encodeBackup(exported))
        assertIs<BackupResult.Valid>(parsed)

        val otherDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            CalisteniaDatabase.Schema.create(otherDriver)
            var otherIds = 0
            val restored = CalisteniaRepository(
                db = CalisteniaDatabase(otherDriver),
                clock = { 10_000L },
                newId = { "restored-${++otherIds}" },
            )
            restored.seedBuiltins()
            val merge = planMerge(parsed.file, restored.existingIds(), restored.loadSettings().keys)
            assertEquals(3, merge.added, "one custom exercise + one routine + one session")
            restored.importBackup(merge)

            assertEquals(repo.exercises().map { it.id }.sorted(), restored.exercises().map { it.id }.sorted())
            assertEquals(
                "Hang under a bar and pull your chest up, keeping the body straight.",
                restored.exercises().first { it.id == custom.id }.description,
            )
            assertEquals(1, restored.routines().size)
            assertEquals(routineName, restored.routines().single().name)
            assertEquals(repo.finishedSessions(), restored.finishedSessions())
            assertEquals("Dark", restored.loadSettings()["theme"])

            val again = planMerge(parsed.file, restored.existingIds(), restored.loadSettings().keys)
            assertTrue(again.isEmpty)
            restored.importBackup(again)
            assertEquals(1, restored.finishedSessions().size)
        } finally {
            otherDriver.close()
        }
    }
}
