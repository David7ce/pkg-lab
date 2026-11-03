package com.david7ce.calistenia.ui.figure

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FigureSkeletonTest {

    private fun distance(a: FigurePoint, b: FigurePoint) = hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble())

    private fun near(expected: Double, actual: Double, tolerance: Double = 1e-4) =
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected, got $actual")

    @Test
    fun hipIsAlwaysTheOrigin() {
        for (pose in PushUp) {
            val skeleton = skeletonFor(pose)
            assertEquals(0f, skeleton.hip.x)
            assertEquals(0f, skeleton.hip.y)
        }
    }

    @Test
    fun limbLengthsAreRigidAcrossAnyAngle() {
        val bent = Pose(torsoLean = 20f, hip = 35f, knee = 50f, shoulder = 60f, elbow = 70f)
        for (pose in PushUp + bent) {
            val skeleton = skeletonFor(pose)
            near(1.0, distance(skeleton.hip, skeleton.shoulder))
            near(0.9, distance(skeleton.hip, skeleton.knee))
            near(0.85, distance(skeleton.knee, skeleton.ankle))
            near(0.45, distance(skeleton.shoulder, skeleton.elbow))
            near(0.42, distance(skeleton.elbow, skeleton.hand))
        }
    }

    @Test
    fun zeroAnglesPutTheWholeBodyOnOneLine() {
        val straight = skeletonFor(Pose(torsoLean = 0f, hip = 0f, knee = 0f, shoulder = 0f, elbow = 0f))
        // Shoulder and knee are opposite directions from the hip on the same axis.
        near(straight.shoulder.y.toDouble(), straight.knee.y.toDouble())
        assertTrue(straight.shoulder.x > straight.hip.x)
        assertTrue(straight.knee.x < straight.hip.x)
        // The ankle continues past the knee on the same line as hip->knee.
        near(straight.knee.y.toDouble(), straight.ankle.y.toDouble())
    }

    @Test
    fun lerpAtTheEndpointsReturnsTheOriginalPoses() {
        val (up, down) = PushUp
        assertEquals(up, lerp(up, down, 0f))
        assertEquals(down, lerp(up, down, 1f))
    }

    @Test
    fun lerpAtTheMidpointAveragesEachAngle() {
        val (up, down) = PushUp
        val mid = lerp(up, down, 0.5f)
        near(((up.shoulder + down.shoulder) / 2f).toDouble(), mid.shoulder.toDouble())
        near(((up.elbow + down.elbow) / 2f).toDouble(), mid.elbow.toDouble())
    }

    @Test
    fun pushUpOnlyAnimatesTheArms() {
        val (up, down) = PushUp
        assertEquals(up.torsoLean, down.torsoLean)
        assertEquals(up.hip, down.hip)
        assertEquals(up.knee, down.knee)
        assertTrue(up.shoulder != down.shoulder)
        assertTrue(up.elbow != down.elbow)
    }
}
