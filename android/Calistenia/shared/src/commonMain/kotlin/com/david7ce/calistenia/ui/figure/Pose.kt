package com.david7ce.calistenia.ui.figure

/**
 * One instant of a parametric 2D stick figure, in profile (side) view. Angles are in degrees and
 * are relative to the rig or to the joint above them, so the same [Pose] can describe a standing,
 * prone or hanging body -- the [Rig] supplies the orientation on screen.
 *
 * A [FigureAnimation] is a list of poses ("up", "down", intermediate) that [Figure] moves through;
 * [lerp] is only used to blend adjacent poses, never to reinterpret an angle.
 */
data class Pose(
    /** Torso tilt away from the rig's body axis; positive leans toward the head (forward). */
    val torsoLean: Float,
    /** Thigh angle away from the torso line; positive rotates the thigh clockwise on screen (for a rig facing +x with a vertical torso, that swings the knee back; a squat uses a negative value to bring it forward). */
    val hip: Float,
    /** Shin bend away from the thigh; positive folds the heel back. */
    val knee: Float,
    /** Upper arm angle away from the rig's arm rest; positive raises it toward the head. */
    val shoulder: Float,
    /** Forearm bend away from the upper arm; positive folds the hand toward the shoulder. */
    val elbow: Float,
)
