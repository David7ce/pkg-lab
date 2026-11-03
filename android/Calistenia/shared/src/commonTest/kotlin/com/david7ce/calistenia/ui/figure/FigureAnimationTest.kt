package com.david7ce.calistenia.ui.figure

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FigureAnimationTest {

    private val rig = Rig(bodyAxisDeg = 0f, armRestDeg = 90f, anchor = Joint.Hip)
    private val a = Pose(torsoLean = 0f, hip = 0f, knee = 0f, shoulder = 0f, elbow = 0f)
    private val b = Pose(torsoLean = 10f, hip = 20f, knee = 30f, shoulder = 40f, elbow = 50f)

    private fun cycle() = FigureAnimation(
        rig = rig,
        frames = listOf(
            Keyframe(a, holdMs = 100, moveMs = 900, easing = Easing.Linear),
            Keyframe(b, holdMs = 50, moveMs = 500, easing = Easing.Linear),
        ),
    )

    private fun near(expected: Float, actual: Float, tolerance: Float = 1e-4f) =
        assertTrue(kotlin.math.abs(expected - actual) <= tolerance, "expected $expected, got $actual")

    @Test
    fun aFrameIsHeldForItsHoldTime() {
        val animation = cycle()
        assertEquals(a, animation.poseAt(0))
        assertEquals(a, animation.poseAt(99))
        assertEquals(a, animation.poseAt(100))
        assertEquals(b, animation.poseAt(100 + 900))
        assertEquals(b, animation.poseAt(100 + 900 + 49))
    }

    @Test
    fun aFrameBlendsTowardTheNextDuringItsMoveTime() {
        val animation = cycle()
        val middle = animation.poseAt(100 + 450)
        near((a.shoulder + b.shoulder) / 2f, middle.shoulder)
        near((a.hip + b.hip) / 2f, middle.hip)

        val backMiddle = animation.poseAt(100 + 900 + 50 + 250)
        near((b.shoulder + a.shoulder) / 2f, backMiddle.shoulder)
    }

    @Test
    fun theCycleWrapsAtItsPeriod() {
        val animation = cycle()
        assertEquals(1550, animation.periodMs)
        assertEquals(a, animation.poseAt(1550))
        assertEquals(a, animation.poseAt(1550 + 99))
        assertEquals(a, animation.poseAt(-1550))
    }

    @Test
    fun easeInOutIsSmoothAtTheEndsAndSymmetricInTheMiddle() {
        assertEquals(0f, Easing.EaseInOut.transform(0f))
        assertEquals(1f, Easing.EaseInOut.transform(1f))
        assertEquals(0.5f, Easing.EaseInOut.transform(0.5f))
        assertTrue(Easing.EaseInOut.transform(0.1f) < 0.1f)
        assertTrue(Easing.EaseInOut.transform(0.9f) > 0.9f)
        assertTrue(Easing.Linear.transform(0.37f) == 0.37f)
    }

    @Test
    fun aSingleKeyframeIsAStaticHold() {
        val hold = FigureAnimation.hold(rig, a)
        assertTrue(hold.isStatic)
        assertEquals(0, hold.periodMs)
        assertEquals(a, hold.poseAt(0))
        assertEquals(a, hold.poseAt(123_456))
        assertEquals(listOf(a), hold.sampledPoses())
    }

    @Test
    fun sampledPosesCoverTheWholeCycleAndKeepTheKeyframes() {
        val animation = cycle()
        val samples = animation.sampledPoses(samplesPerCycle = 16)
        assertEquals(2 + 16, samples.size)
        assertTrue(samples.contains(a))
        assertTrue(samples.contains(b))
    }

    @Test
    fun anAnimationNeedsFramesAndMovingFramesNeedTime() {
        assertFailsWith<IllegalArgumentException> { FigureAnimation(rig, emptyList()) }
        assertFailsWith<IllegalArgumentException> {
            FigureAnimation(rig, listOf(Keyframe(a, moveMs = 100), Keyframe(b, moveMs = 0)))
        }
        assertFailsWith<IllegalArgumentException> { Keyframe(a, holdMs = -1) }
        assertFailsWith<IllegalArgumentException> { Keyframe(a, moveMs = -1) }
    }
}
