package com.david7ce.calistenia

import com.david7ce.calistenia.logic.AppSettings
import com.david7ce.calistenia.logic.BACKUP_FORMAT
import com.david7ce.calistenia.logic.BACKUP_VERSION
import com.david7ce.calistenia.logic.BackupFile
import com.david7ce.calistenia.logic.BackupProblem
import com.david7ce.calistenia.logic.BackupResult
import com.david7ce.calistenia.logic.BuiltinExercises
import com.david7ce.calistenia.logic.ExistingIds
import com.david7ce.calistenia.logic.RoutineExerciseDto
import com.david7ce.calistenia.logic.buildBackup
import com.david7ce.calistenia.logic.encodeBackup
import com.david7ce.calistenia.logic.parseBackup
import com.david7ce.calistenia.logic.planMerge
import com.david7ce.calistenia.logic.toDto
import com.david7ce.calistenia.logic.toModel
import com.david7ce.calistenia.logic.toStored
import com.david7ce.calistenia.model.Routine
import com.david7ce.calistenia.model.RoutineExercise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackupTest {

    private val pushUp = BuiltinExercises.find("builtin:push-up")!!
    private val routine = Routine(
        id = "r1",
        name = "Push day",
        notes = "Slow tempo",
        exercises = listOf(
            RoutineExercise("l1", pushUp, sets = 3, targetReps = 8, targetSeconds = null, restSeconds = 90),
        ),
        createdAt = 1,
        updatedAt = 2,
    )
    private val session = testSession("s1", startedAt = 1_000, sets = listOf(8, 7), routineId = "r1")
    private val settings = AppSettings()

    private fun file(): BackupFile =
        buildBackup(listOf(pushUp), listOf(routine), listOf(session), settings, now = 999)

    private fun parseOrFail(file: BackupFile): BackupFile {
        val result = parseBackup(encodeBackup(file))
        assertIs<BackupResult.Valid>(result, "expected valid, got $result")
        return result.file
    }

    private fun problemOf(file: BackupFile): BackupProblem {
        val result = parseBackup(encodeBackup(file))
        assertIs<BackupResult.Invalid>(result, "expected invalid, got $result")
        return result.problem
    }

    @Test
    fun roundTripKeepsEverything() {
        val decoded = parseOrFail(file())
        assertEquals(BACKUP_FORMAT, decoded.format)
        assertEquals(BACKUP_VERSION, decoded.version)
        assertEquals(999L, decoded.exportedAt)
        assertEquals(listOf(pushUp.id), decoded.exercises.map { it.id })
        assertTrue(decoded.exercises.single().description.length >= 100)
        assertEquals("Push day", decoded.routines.single().name)
        assertEquals(3, decoded.routines.single().exercises.single().sets)
        assertEquals(listOf(8, 7), decoded.sessions.single().exercises.single().sets.map { it.reps })
        assertEquals("Slow tempo", decoded.routines.single().notes)
    }

    @Test
    fun exportIncludesOnlyFinishedSessions() {
        val unfinished = testSession("s2", startedAt = 2_000, sets = listOf(5), finished = false)
        val exported = buildBackup(listOf(pushUp), listOf(routine), listOf(session, unfinished), settings, now = 0)
        assertEquals(listOf("s1"), exported.sessions.map { it.id })
    }

    @Test
    fun rejectsAnotherFormatAndNewerVersions() {
        assertEquals(BackupProblem.WrongFormat, problemOf(file().copy(format = "recetario")))
        assertEquals(BackupProblem.NewerVersion, problemOf(file().copy(version = BACKUP_VERSION + 1)))
        assertEquals(BackupProblem.WrongFormat, problemOf(file().copy(version = 0)))
    }

    @Test
    fun rejectsPlainText() {
        assertIs<BackupResult.Invalid>(parseBackup("not json at all"))
    }

    @Test
    fun version1BackupsStillImportWithoutDescriptions() {
        val v1 = """
            {"format":"calistenia","version":1,"exportedAt":0,"app":"Calistenia",
             "exercises":[{"id":"custom:x","name":"Lever row","group":"Pull","measure":"Reps","builtin":true}],
             "routines":[],"sessions":[],"settings":{}}
        """.trimIndent()
        val parsed = parseBackup(v1)
        assertIs<BackupResult.Valid>(parsed)
        assertEquals("", parsed.file.exercises.single().description)
        assertNull(parsed.file.exercises.single().parentId)
    }

    @Test
    fun rejectsDuplicateIds() {
        val dto = session.toDto()!!
        assertEquals(BackupProblem.DuplicateId, problemOf(file().copy(sessions = listOf(dto, dto))))
    }

    @Test
    fun rejectsRoutinesPointingAtUnknownExercises() {
        val line = RoutineExerciseDto(
            id = "l2", exerciseId = "custom:not-there", position = 0, sets = 3,
            targetReps = 8, restSeconds = 90,
        )
        val bad = file().copy(routines = listOf(routine.toDto().copy(exercises = listOf(line))))
        assertEquals(BackupProblem.InvalidRoutine, problemOf(bad))
    }

    @Test
    fun rejectsUnknownMeasures() {
        val dto = session.toDto()!!
        val bad = dto.copy(exercises = dto.exercises.map { it.copy(measure = "Yards") })
        assertEquals(BackupProblem.InvalidSession, problemOf(file().copy(sessions = listOf(bad))))
    }

    @Test
    fun rejectsACompletedSetWithoutAValue() {
        val dto = session.toDto()!!
        val bad = dto.copy(
            exercises = dto.exercises.map { exercise ->
                exercise.copy(sets = exercise.sets.map { it.copy(reps = null, seconds = null, completedAt = 1) })
            },
        )
        assertEquals(BackupProblem.InvalidSession, problemOf(file().copy(sessions = listOf(bad))))
    }

    @Test
    fun mergeSkipsWhatIsAlreadyThere() {
        val all = ExistingIds(
            exercises = setOf(pushUp.id),
            routines = setOf(routine.id),
            sessions = setOf(session.id),
        )
        val merge = planMerge(file(), all, settings.toStored().keys)
        assertEquals(0, merge.added)
        assertEquals(3, merge.alreadyPresent)
        assertTrue(merge.isEmpty)
        assertTrue(merge.exercises.isEmpty() && merge.routines.isEmpty() && merge.sessions.isEmpty())
        assertTrue(merge.settings.isEmpty())
    }

    @Test
    fun mergeAddsEverythingOnAFreshDeviceButKeepsExistingSettings() {
        val merge = planMerge(file(), ExistingIds(), existingSettings = setOf("theme"))
        assertEquals(3, merge.added)
        assertEquals(0, merge.alreadyPresent)
        assertEquals(1, merge.routines.single().exercises.size)
        assertEquals(1, merge.sessions.single().exercises.size)
        assertEquals(2, merge.sessions.single().exercises.single().sets.size)
        assertTrue("theme" !in merge.settings)
        assertTrue("language" in merge.settings)
    }

    @Test
    fun dtosMapBackToModels() {
        val back = pushUp.toDto().toModel()
        assertEquals(pushUp, back)
    }

    @Test
    fun roundTripKeepsTheFamilyLink() {
        val knee = BuiltinExercises.find("builtin:knee-push-up")!!
        val decoded = parseOrFail(buildBackup(listOf(knee), emptyList(), emptyList(), settings, now = 0))
        assertEquals("builtin:push-up", decoded.exercises.single().parentId)
        assertEquals(knee.description, decoded.exercises.single().description)
    }

    @Test
    fun rejectsAVariantWithAnUnknownParent() {
        val knee = BuiltinExercises.find("builtin:knee-push-up")!!.copy(parentId = "custom:not-there")
        val bad = buildBackup(listOf(knee), emptyList(), emptyList(), settings, now = 0)
        assertEquals(BackupProblem.InvalidExercise, problemOf(bad))
    }
}
