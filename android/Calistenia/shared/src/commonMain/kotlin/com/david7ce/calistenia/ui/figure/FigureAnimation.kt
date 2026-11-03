package com.david7ce.calistenia.ui.figure

/** How a [Keyframe]'s outgoing movement is eased. */
enum class Easing {
    Linear,

    /** Smoothstep: slow at both ends, fast through the middle. A rep reads better than linear. */
    EaseInOut;

    fun transform(t: Float): Float = when (this) {
        Linear -> t
        EaseInOut -> t * t * (3f - 2f * t)
    }
}

/**
 * One pose in a loop. The figure holds this pose for [holdMs] (a pause at the top or bottom of a
 * rep), then moves to the next frame over [moveMs], easing with [easing]. The last frame moves back
 * to the first, so the list is a cycle and the first frame's `holdMs` is also its pause at the top.
 */
data class Keyframe(
    val pose: Pose,
    val holdMs: Int = 0,
    val moveMs: Int = 0,
    val easing: Easing = Easing.EaseInOut,
) {
    init {
        require(holdMs >= 0) { "holdMs must not be negative; got $holdMs." }
        require(moveMs >= 0) { "moveMs must not be negative; got $moveMs." }
    }
}

/**
 * A looping form animation: a [rig] plus the keyframes a rep passes through. Pure timing (no
 * Compose), so sampling can be tested: [poseAt] maps elapsed milliseconds to the blended [Pose],
 * and [sampledPoses] gives a stable set for the layout bounds.
 *
 * A single keyframe is a hold (plank, L-sit): [isStatic] and [periodMs] is 0.
 */
data class FigureAnimation(
    val rig: Rig,
    val frames: List<Keyframe>,
) {
    init {
        require(frames.isNotEmpty()) { "A figure animation needs at least one keyframe." }
        if (frames.size > 1) {
            require(frames.all { it.moveMs > 0 }) {
                "Every keyframe of a moving figure needs a positive moveMs; got ${frames.map { it.moveMs }}."
            }
        }
    }

    val periodMs: Int = if (frames.size == 1) 0 else frames.sumOf { it.holdMs + it.moveMs }
    val isStatic: Boolean = frames.size == 1

    /** The pose at [elapsedMs] in the loop; wraps in both directions. */
    fun poseAt(elapsedMs: Long): Pose {
        if (isStatic) return frames.first().pose
        val period = periodMs.toLong()
        val t = ((elapsedMs % period) + period) % period
        var cursor = 0L
        for (i in frames.indices) {
            val frame = frames[i]
            if (t < cursor + frame.holdMs) return frame.pose
            cursor += frame.holdMs
            if (t < cursor + frame.moveMs) {
                val progress = (t - cursor).toFloat() / frame.moveMs
                val next = frames[(i + 1) % frames.size]
                return lerp(frame.pose, next.pose, frame.easing.transform(progress))
            }
            cursor += frame.moveMs
        }
        // Only reachable if the modulo above ever lands exactly on the period, which it cannot.
        return frames.first().pose
    }

    /**
     * Keyframes plus a uniform sweep of the movement segments, enough to take a stable bounding box
     * (joint rotations can bow outward between two poses).
     */
    fun sampledPoses(samplesPerCycle: Int = 64): List<Pose> {
        if (isStatic) return listOf(frames.first().pose)
        val poses = frames.map { it.pose }.toMutableList()
        for (i in 0 until samplesPerCycle) {
            poses += poseAt(periodMs.toLong() * i / samplesPerCycle)
        }
        return poses
    }

    companion object {
        /** A still diagram with no rep cycle. */
        fun hold(rig: Rig, pose: Pose): FigureAnimation =
            FigureAnimation(rig, listOf(Keyframe(pose)))
    }
}
