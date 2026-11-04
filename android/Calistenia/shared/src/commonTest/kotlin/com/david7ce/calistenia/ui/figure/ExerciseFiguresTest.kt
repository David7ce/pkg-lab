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
    fun pullUpKeepsItsHandsOnTheBarAndTheBodyBelowIt() {
        for (skeleton in samples(PullUp)) {
            assertEquals(0f, skeleton.hand.x, "the anchored hand does not slide")
            assertEquals(0f, skeleton.hand.y, "the anchored hand stays on the bar line")
            assertTrue(skeleton.points.minOf { it.y } >= -0.3f, "a joint is above the bar: $skeleton")
            assertTrue(skeleton.elbow.y >= -0.02f, "the elbow went over the bar: $skeleton")
        }
    }

    @Test
    fun pullUpRaisesTheBodyUntilTheHeadReachesTheBar() {
        val all = samples(PullUp)
        val hangingHead = all.maxOf { it.headCenter.y }
        val topHead = all.minOf { it.headCenter.y }
        // Hanging: shoulder 0.87 below the bar, the head centre ~0.29 above the shoulder.
        assertTrue(hangingHead > 0.5f, "hanging head depth $hangingHead")
        // The head circle (radius 0.22) must overlap the bar line at the top.
        assertTrue(topHead - all.first().headRadius < 0.05f, "head only reaches $topHead below the bar")
        assertTrue(topHead < hangingHead * 0.5f, "the head only rises from $hangingHead to $topHead")
    }

    @Test
    fun pullUpBoundsCoverEveryPoseInTheCycle() {
        val bounds = boundsFor(PullUp)
        for (skeleton in samples(PullUp)) {
            for (point in skeleton.points) {
                assertTrue(point.x >= bounds.minX && point.x <= bounds.maxX, "x $point outside $bounds")
                assertTrue(point.y >= bounds.minY && point.y <= bounds.maxY, "y $point outside $bounds")
            }
        }
    }

    @Test
    fun onlyRegisteredFamiliesHaveAFigure() {
        assertEquals(PushUp, figureFor("builtin:push-up"))
        assertEquals(Squat, figureFor("builtin:squat"))
        assertEquals(PullUp, figureFor("builtin:pull-up"))
        assertEquals(Contact.Bar, PullUp.rig.contact)
        assertEquals(null, figureFor("builtin:plank"))
        assertEquals(null, figureFor("custom-1"))
    }
}
