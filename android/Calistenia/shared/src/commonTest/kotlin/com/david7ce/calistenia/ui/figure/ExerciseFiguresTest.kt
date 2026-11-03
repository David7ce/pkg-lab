package com.david7ce.calistenia.ui.figure

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExerciseFiguresTest {

    private fun samples(animation: FigureAnimation, count: Int = 64): List<FigureSkeleton> =
        (0 until count).map {
            val elapsed = if (animation.isStatic) 0L else animation.periodMs.toLong() * it / count
            anchoredSkeleton(animation.poseAt(elapsed), animation.rig)
        }

    @Test
    fun pushUpKeepsItsHandsAndFeetOnTheFloor() {
        var highestFoot = 0f
        for (skeleton in samples(PushUp)) {
            assertEquals(0f, skeleton.hand.y, "the anchored hand stays on the floor line")
            highestFoot = maxOf(highestFoot, abs(skeleton.ankle.y))
        }
        // The feet are allowed to hover a little mid-rep (linear angle blends cannot follow the
        // floor exactly), but not enough to read as floating.
        assertTrue(highestFoot < 0.12f, "the foot drifts $highestFoot torso lengths off the floor")
    }

    @Test
    fun theBodyNeverGoesThroughItsGroundLine() {
        for (skeleton in samples(PushUp)) {
            assertTrue(skeleton.points.maxOf { it.y } <= 0.02f, "a joint is below the floor: $skeleton")
        }
    }

    @Test
    fun theBoundsCoverEveryPoseInTheCycle() {
        val bounds = boundsFor(PushUp)
        for (skeleton in samples(PushUp)) {
            for (point in skeleton.points) {
                assertTrue(point.x >= bounds.minX && point.x <= bounds.maxX, "x $point outside $bounds")
                assertTrue(point.y >= bounds.minY && point.y <= bounds.maxY, "y $point outside $bounds")
            }
        }
        assertTrue(bounds.width > 0f && bounds.height > 0f)
    }

    @Test
    fun squatKeepsItsAnklePlantedAndDropsTheHips() {
        val all = samples(Squat)
        for (skeleton in all) {
            assertEquals(0f, skeleton.ankle.y, "the anchored ankle stays on the floor line")
            assertEquals(0f, skeleton.ankle.x, "the anchored ankle does not slide")
            assertTrue(skeleton.points.maxOf { it.y } <= 0.02f, "a joint is below the floor: $skeleton")
        }
        val standingHip = -all.minOf { it.hip.y }
        val deepestHip = -all.maxOf { it.hip.y }
        assertTrue(standingHip > 1.6f, "standing hip height $standingHip")
        assertTrue(deepestHip < standingHip * 0.6f, "the hips only drop to $deepestHip of $standingHip")
    }

    @Test
    fun squatStaysBalancedOverTheFoot() {
        for (skeleton in samples(Squat)) {
            // Midpoint of hip and shoulder is a crude centre of mass; it must stay near the foot.
            val comX = (skeleton.hip.x + skeleton.shoulder.x) / 2f
            assertTrue(abs(comX) < 0.6f, "centre of mass is ${comX} torso lengths from the foot")
        }
    }

    @Test
    fun onlyRegisteredFamiliesHaveAFigure() {
        assertEquals(PushUp, figureFor("builtin:push-up"))
        assertEquals(Squat, figureFor("builtin:squat"))
        assertEquals(null, figureFor("builtin:plank"))
        assertEquals(null, figureFor("custom-1"))
    }
}
