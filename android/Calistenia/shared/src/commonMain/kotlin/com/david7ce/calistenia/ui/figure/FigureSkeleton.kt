package com.david7ce.calistenia.ui.figure

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A point in the figure's own normalized space: one unit = one hip-to-shoulder torso length. */
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
) {
    fun point(joint: Joint): FigurePoint = when (joint) {
        Joint.Head -> headCenter
        Joint.Shoulder -> shoulder
        Joint.Hip -> hip
        Joint.Elbow -> elbow
        Joint.Hand -> hand
        Joint.Knee -> knee
        Joint.Ankle -> ankle
    }

    /** Every drawable point, head padding excluded (see [boundsOf]). */
    val points: List<FigurePoint>
        get() = listOf(headCenter, shoulder, hip, elbow, hand, knee, ankle)

    fun translated(dx: Float, dy: Float) = copy(
        headCenter = headCenter.translated(dx, dy),
        shoulder = shoulder.translated(dx, dy),
        hip = hip.translated(dx, dy),
        elbow = elbow.translated(dx, dy),
        hand = hand.translated(dx, dy),
        knee = knee.translated(dx, dy),
        ankle = ankle.translated(dx, dy),
    )
}

private fun FigurePoint.translated(dx: Float, dy: Float) = FigurePoint(x + dx, y + dy)

/** Segment lengths as a fraction of the torso (hip-to-shoulder), proportioned for an adult figure. */
private const val TORSO = 1f
private const val HEAD_RADIUS = 0.22f
private const val UPPER_ARM = 0.45f
private const val FOREARM = 0.42f
private const val THIGH = 0.9f
private const val SHIN = 0.85f

private fun walk(from: FigurePoint, length: Float, angleDeg: Float): FigurePoint {
    val rad = angleDeg * PI.toFloat() / 180f
    return FigurePoint(from.x + length * cos(rad), from.y + length * sin(rad))
}

/**
 * Forward-kinematics a [Pose] into drawable points for [rig], with the hip at the origin and the
 * body still in the rig's screen orientation. Pure and platform-independent, so the geometry is
 * unit-testable without a Compose harness.
 *
 * Screen angles: 0 = +x (right), 90 = +y (down). The torso is rotated by [Rig.bodyAxisDeg] plus
 * `Pose.torsoLean`; legs continue from the torso (straight body at zero hip/knee), and arms start
 * at [Rig.armRestDeg] from the torso.
 */
fun skeletonFor(pose: Pose, rig: Rig): FigureSkeleton {
    val torsoDeg = rig.bodyAxisDeg + pose.torsoLean
    val shoulder = walk(FigurePoint(0f, 0f), TORSO, torsoDeg)
    val headCenter = walk(shoulder, HEAD_RADIUS * 1.3f, torsoDeg)
    val thighDeg = torsoDeg + 180f + pose.hip
    val knee = walk(FigurePoint(0f, 0f), THIGH, thighDeg)
    val ankle = walk(knee, SHIN, thighDeg + pose.knee)
    val upperArmDeg = torsoDeg + rig.armRestDeg + pose.shoulder
    val elbow = walk(shoulder, UPPER_ARM, upperArmDeg)
    val hand = walk(elbow, FOREARM, upperArmDeg + pose.elbow)
    return FigureSkeleton(
        headCenter = headCenter,
        headRadius = HEAD_RADIUS,
        shoulder = shoulder,
        hip = FigurePoint(0f, 0f),
        elbow = elbow,
        hand = hand,
        knee = knee,
        ankle = ankle,
    )
}

/**
 * [skeletonFor] translated so [Rig.anchor] sits at the origin. This is what [Figure] draws: the
 * anchor is the point that must not move while the rest of the body does (a hand on the floor, a
 * foot under a squat, a hand on the bar).
 */
fun anchoredSkeleton(pose: Pose, rig: Rig): FigureSkeleton {
    val raw = skeletonFor(pose, rig)
    val anchor = raw.point(rig.anchor)
    return raw.translated(-anchor.x, -anchor.y)
}

/** Linear interpolation between two poses, 0 = [a], 1 = [b]. */
fun lerp(a: Pose, b: Pose, t: Float): Pose = Pose(
    torsoLean = a.torsoLean + (b.torsoLean - a.torsoLean) * t,
    hip = a.hip + (b.hip - a.hip) * t,
    knee = a.knee + (b.knee - a.knee) * t,
    shoulder = a.shoulder + (b.shoulder - a.shoulder) * t,
    elbow = a.elbow + (b.elbow - a.elbow) * t,
)

/** A bounding box in figure space, already padded by the head radius. */
data class FigureBounds(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
) {
    val width get() = maxX - minX
    val height get() = maxY - minY
    val centerX get() = (minX + maxX) / 2f
    val centerY get() = (minY + maxY) / 2f
}

fun boundsOf(skeletons: Iterable<FigureSkeleton>): FigureBounds {
    val list = skeletons.toList()
    require(list.isNotEmpty()) { "Cannot take the bounds of no skeletons." }
    val points = list.flatMap { it.points }
    val headPad = list.maxOf { it.headRadius }
    return FigureBounds(
        minX = points.minOf { it.x } - headPad,
        maxX = points.maxOf { it.x } + headPad,
        minY = points.minOf { it.y } - headPad,
        maxY = points.maxOf { it.y } + headPad,
    )
}

/**
 * A fixed box around every pose the animation can show. It must be computed from the whole cycle,
 * not the current frame, or the figure would visibly resize and jump while it moves.
 */
fun boundsFor(animation: FigureAnimation): FigureBounds =
    boundsOf(animation.sampledPoses().map { anchoredSkeleton(it, animation.rig) })
