package com.david7ce.calistenia.ui.figure

/**
 * One instant of a parametric 2D stick figure, in profile (side) view. Angles are in degrees;
 * a family's animation is two [Pose]s (an "up" and a "down") that [Figure] interpolates between.
 *
 * All angles are relative to the figure's own baseline, not world gravity, so the same [Pose]
 * shape works whether the exercise is done standing, prone or hanging -- only the baseline
 * orientation [Figure] draws it at needs to change per family.
 */
data class Pose(
    /** Torso tilt away from the baseline body line (legs stay the reference at 0). */
    val torsoLean: Float,
    /** Thigh angle away from the torso line (0 = legs in line with the torso). */
    val hip: Float,
    /** Shin bend away from the thigh (0 = leg straight). */
    val knee: Float,
    /** Upper arm angle away from its resting line (0 = hanging straight from the shoulder). */
    val shoulder: Float,
    /** Forearm bend away from the upper arm (0 = arm straight). */
    val elbow: Float,
)
