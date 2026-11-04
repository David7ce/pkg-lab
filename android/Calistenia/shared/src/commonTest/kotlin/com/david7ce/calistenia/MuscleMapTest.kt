package com.david7ce.calistenia

import com.david7ce.calistenia.logic.BuiltinExercises
import com.david7ce.calistenia.logic.filterFamilies
import com.david7ce.calistenia.logic.toggled
import com.david7ce.calistenia.logic.trainsAny
import com.david7ce.calistenia.model.Exercise
import com.david7ce.calistenia.model.Measure
import com.david7ce.calistenia.model.Muscle
import com.david7ce.calistenia.model.MuscleGroup
import com.david7ce.calistenia.ui.BodyView
import com.david7ce.calistenia.ui.muscleAt
import com.david7ce.calistenia.ui.musclesOnMap
import com.david7ce.calistenia.ui.regionsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The body map and the filter it drives (M2). Pure logic: the drawing itself is not rendered here. */
class MuscleMapTest {

    private fun families(muscles: Set<Muscle>) = filterFamilies(BuiltinExercises.all, muscles = muscles)

    @Test
    fun everyMuscleCanBePickedOnTheMapFromOneSideOrTheOther() {
        assertEquals(Muscle.entries.toSet(), musclesOnMap(BodyView.Front) + musclesOnMap(BodyView.Back))
    }

    @Test
    fun theFrontAndBackSplitMatchesTheDesign() {
        assertEquals(
            setOf(Muscle.Chest, Muscle.Shoulders, Muscle.Biceps, Muscle.Forearms, Muscle.Abs, Muscle.Obliques, Muscle.Quads),
            musclesOnMap(BodyView.Front),
        )
        assertEquals(
            setOf(
                Muscle.Lats, Muscle.Shoulders, Muscle.Triceps, Muscle.Forearms,
                Muscle.LowerBack, Muscle.Glutes, Muscle.Hamstrings, Muscle.Calves,
            ),
            musclesOnMap(BodyView.Back),
        )
    }

    @Test
    fun tappingResolvesToTheMuscleUnderTheFingerOnBothSidesOfTheBody() {
        assertEquals(Muscle.Chest, muscleAt(BodyView.Front, 58f, 46f))
        assertEquals(Muscle.Chest, muscleAt(BodyView.Front, 42f, 46f), "left half mirrors the right")
        assertEquals(Muscle.Quads, muscleAt(BodyView.Front, 58f, 120f))
        assertEquals(Muscle.Glutes, muscleAt(BodyView.Back, 42f, 100f))
        assertEquals(Muscle.Calves, muscleAt(BodyView.Back, 58f, 165f))
        assertNull(muscleAt(BodyView.Front, 5f, 5f), "background")
        assertNull(muscleAt(BodyView.Front, 54f, 15f), "the head is drawn but is not a muscle")
        assertNull(muscleAt(BodyView.Front, 58f, 165f), "front shins are not selectable")
    }

    @Test
    fun theZonesOfOneViewDoNotOverlapEnoughToMakeATapAmbiguous() {
        // Every muscle zone's own centroid must resolve to that muscle: a zone swallowed by a
        // neighbour would be unreachable on the map.
        BodyView.entries.forEach { view ->
            regionsOf(view).filter { it.muscle != null }.forEach { region ->
                val cx = region.points.map { it.first }.average().toFloat()
                val cy = region.points.map { it.second }.average().toFloat()
                assertEquals(region.muscle, muscleAt(view, cx, cy), "$view ${region.muscle} centroid")
            }
        }
    }

    @Test
    fun selectingAMuscleNarrowsTheListAndDeselectingRestoresIt() {
        val all = families(emptySet())
        assertEquals(BuiltinExercises.all.count { it.isTopLevel }, all.size)

        val selection = emptySet<Muscle>().toggled(Muscle.Calves)
        val calves = families(selection)
        assertTrue(calves.isNotEmpty())
        assertTrue(calves.size < all.size, "selecting a muscle must change the visible list")
        calves.forEach { family ->
            assertTrue(family.variants.any { it.trainsAny(selection) }, "${family.parent.id} does not train calves")
        }

        assertEquals(all, families(selection.toggled(Muscle.Calves)), "deselecting restores everything")
    }

    @Test
    fun theSelectionIsAnOrUnion() {
        val chest = families(setOf(Muscle.Chest)).map { it.parent.id }.toSet()
        val calves = families(setOf(Muscle.Calves)).map { it.parent.id }.toSet()
        val both = families(setOf(Muscle.Chest, Muscle.Calves)).map { it.parent.id }.toSet()
        assertEquals(chest + calves, both)
    }

    @Test
    fun aCustomExerciseWithoutTagsSurvivesTheFilterThroughItsGroup() {
        val custom = Exercise(
            id = "custom-row", name = "My row", group = MuscleGroup.Pull,
            measure = Measure.Reps, builtin = false,
        )
        val shown = filterFamilies(listOf(custom), muscles = setOf(Muscle.Biceps))
        assertEquals(listOf("custom-row"), shown.map { it.parent.id })
    }

    @Test
    fun toggleAddsThenRemoves() {
        val once = emptySet<Muscle>().toggled(Muscle.Abs)
        assertEquals(setOf(Muscle.Abs), once)
        assertEquals(emptySet(), once.toggled(Muscle.Abs))
    }
}
