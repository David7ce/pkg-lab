package com.david7ce.calistenia.ui.figure

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A point in the figure's own normalized space: hip at the origin, one unit = one head-to-hip torso length. */
data class FigurePoint(val x: Float, val y: Float)

/** Every joint [Figure] needs to draw, plus the head's radius (same unit as the points). */
data class FigureSkeleton(
    val headCenter: FigurePoint,
    val headRadius: Float,
    val shoulder: FigurePoint,
    val hip: FigurePoint,
    val elbow: FigurePoint,
    val hand: FigurePoint,
    val knee: FigurePoint,
    val ankle: FigurePoint,
)

/** Segment lengths as a fraction of the torso (hip-to-shoulder), proportioned for an adult figure. */
private const val HEAD_RADIUS = 0.22f
private const val UPPER_ARM = 0.45f
private const val FOREARM = 0.42f
private const val THIGH = 0.9f
private const val SHIN = 0.85f

/**
 * The resting direction of each limb when its [Pose] angle is zero, in degrees, measured the same
 * way [walk] measures `angleDeg`: 0 points along +x, 90 points along +y (down, since this is
 * screen space). Chosen for a prone, straight-plank baseline (push-ups and friends): the body line
 * runs along +x (hip to shoulder), legs continue along -x, arms hang toward +y (down, to the
 * ground). A family with a different baseline (standing, hanging) needs different resting
 * directions -- this spike only covers the plank case.
 */
private const val BODY_AXIS_DEG = 0f
private const val LEG_REST_DEG = 180f
private const val ARM_REST_DEG = 90f

private fun walk(from: FigurePoint, length: Float, angleDeg: Float): FigurePoint {
    val rad = angleDeg * PI.toFloat() / 180f
    return FigurePoint(from.x + length * cos(rad), from.y + length * sin(rad))
}

/**
 * Forward-kinematics the five [Pose] angles into drawable points, hip at the origin. Pure and
 * platform-independent so it can be unit-tested without a Compose UI test harness.
 */
fun skeletonFor(pose: Pose): FigureSkeleton {
    val hip = FigurePoint(0f, 0f)
    val shoulder = walk(hip, 1f, BODY_AXIS_DEG - pose.torsoLean)
    val headCenter = walk(shoulder, HEAD_RADIUS * 1.3f, BODY_AXIS_DEG - pose.torsoLean)
    val knee = walk(hip, THIGH, LEG_REST_DEG + pose.hip)
    val ankle = walk(knee, SHIN, LEG_REST_DEG + pose.hip + pose.knee)
    val elbow = walk(shoulder, UPPER_ARM, ARM_REST_DEG + pose.shoulder)
    val hand = walk(elbow, FOREARM, ARM_REST_DEG + pose.shoulder + pose.elbow)
    return FigureSkeleton(
        headCenter = headCenter,
        headRadius = HEAD_RADIUS,
        shoulder = shoulder,
        hip = hip,
        elbow = elbow,
        hand = hand,
        knee = knee,
        ankle = ankle,
    )
}

/** Linear interpolation between two poses, 0 = [a], 1 = [b]. */
fun lerp(a: Pose, b: Pose, t: Float): Pose = Pose(
    torsoLean = a.torsoLean + (b.torsoLean - a.torsoLean) * t,
    hip = a.hip + (b.hip - a.hip) * t,
    knee = a.knee + (b.knee - a.knee) * t,
    shoulder = a.shoulder + (b.shoulder - a.shoulder) * t,
    elbow = a.elbow + (b.elbow - a.elbow) * t,
)
