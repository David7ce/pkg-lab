package com.david7ce.calistenia

import com.david7ce.calistenia.logic.BuiltinExercises
import com.david7ce.calistenia.logic.effectiveMuscles
import com.david7ce.calistenia.logic.musclesFor
import com.david7ce.calistenia.logic.toDto
import com.david7ce.calistenia.logic.toModel
import com.david7ce.calistenia.logic.trainsAny
import com.david7ce.calistenia.model.Exercise
import com.david7ce.calistenia.model.Measure
import com.david7ce.calistenia.model.Muscle
import com.david7ce.calistenia.model.MuscleGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The fine muscle axis: the data the body map and the pre-workout filter read.
 * No UI here — these are the rules the map cannot get wrong.
 */
class MusclesTest {

    @Test
    fun everyGroupFallsBackToItsObviousMembers() {
        MuscleGroup.entries.forEach { group ->
            assertTrue(musclesFor(group).isNotEmpty(), "$group has no fallback muscles")
        }
        assertEquals(setOf(Muscle.Chest, Muscle.Shoulders, Muscle.Triceps), musclesFor(MuscleGroup.Push))
        assertEquals(setOf(Muscle.Lats, Muscle.Biceps, Muscle.Forearms), musclesFor(MuscleGroup.Pull))
        assertEquals(setOf(Muscle.Quads, Muscle.Hamstrings, Muscle.Glutes, Muscle.Calves), musclesFor(MuscleGroup.Legs))
        assertEquals(setOf(Muscle.Abs, Muscle.Obliques, Muscle.LowerBack), musclesFor(MuscleGroup.Core))
    }

    @Test
    fun aCustomExerciseWithoutTagsStillHasMuscles() {
        val custom = Exercise(
            id = "custom-1", name = "My row", group = MuscleGroup.Pull,
            measure = Measure.Reps, builtin = false,
        )
        assertTrue(custom.muscles.isEmpty(), "nothing was tagged")
        assertEquals(musclesFor(MuscleGroup.Pull), custom.effectiveMuscles())
    }

    @Test
    fun curatedTagsWinOverTheGroupFallback() {
        val calfRaise = BuiltinExercises.find("builtin:calf-raise")
        assertTrue(calfRaise != null)
        assertEquals(setOf(Muscle.Calves), calfRaise.effectiveMuscles(), "a calf raise is not a whole leg day")
        val chinUp = BuiltinExercises.find("builtin:chin-up")
        assertTrue(chinUp != null)
        assertTrue(Muscle.Biceps in chinUp.effectiveMuscles())
        assertFalse(Muscle.Calves in chinUp.effectiveMuscles())
    }

    @Test
    fun everyBuiltinEndsUpWithAtLeastOneMuscle() {
        BuiltinExercises.all.forEach { exercise ->
            assertTrue(
                exercise.effectiveMuscles().isNotEmpty(),
                "${exercise.id} would be invisible in every filtered list",
            )
        }
    }

    @Test
    fun theFilterIsOrAndAnEmptySelectionKeepsEverything() {
        val pushUp = BuiltinExercises.find("builtin:push-up")!!
        assertTrue(pushUp.trainsAny(setOf(Muscle.Chest)))
        assertTrue(pushUp.trainsAny(setOf(Muscle.Calves, Muscle.Triceps)), "OR, not AND")
        assertFalse(pushUp.trainsAny(setOf(Muscle.Calves, Muscle.Hamstrings)))
        assertTrue(pushUp.trainsAny(emptySet()), "no selection means no filter")
    }

    @Test
    fun backupKeepsKnownMusclesAndDropsUnknownOnes() {
        val exercise = BuiltinExercises.find("builtin:push-up")!!
        val dto = exercise.toDto()
        assertEquals(listOf(Muscle.Chest.name, Muscle.Shoulders.name, Muscle.Triceps.name), dto.muscles)

        // A backup from a newer version may name muscles this build does not know.
        val imported = dto.copy(muscles = listOf("Chest", "Neck", "chest")).toModel()
        assertEquals(setOf(Muscle.Chest), imported.muscles)
    }

    @Test
    fun storageRoundTripsThroughTheCsvColumn() {
        val muscles = setOf(Muscle.Glutes, Muscle.Hamstrings)
        assertEquals(muscles, Muscle.parseCsv(Muscle.toCsv(muscles)))
        assertEquals(emptySet(), Muscle.parseCsv(""), "empty means derive from the group")
        assertEquals(setOf(Muscle.Calves), Muscle.parseCsv("Calves, not-a-muscle"))
    }
}
