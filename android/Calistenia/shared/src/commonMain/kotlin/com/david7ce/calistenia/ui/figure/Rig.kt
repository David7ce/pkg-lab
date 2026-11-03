package com.david7ce.calistenia.ui.figure

/** A joint of the skeleton, usable as a [Rig]'s anchor. */
enum class Joint {
    Head,
    Shoulder,
    Hip,
    Elbow,
    Hand,
    Knee,
    Ankle,
}

/** The line drawn through a [Rig]'s anchor: the floor, a pull-up bar, or nothing at all. */
enum class Contact {
    None,
    Ground,
    Bar,
}

/**
 * The orientation that turns a [Pose] into a picture, shared by every exercise in the same
 * family/archetype.
 *
 * The spike hard-coded a prone baseline and kept the hip at the origin. That does not generalise:
 * a squat stands, a pull-up hangs, and pinning the hip makes the floor move under the feet. A rig
 * instead declares where the body axis and the arms point, and which joint stays fixed.
 *
 * [bodyAxisDeg] is the screen direction from hip to shoulder: 0 = +x (head to the right), 90 = +y
 * (down, since this is screen space). [armRestDeg] is the upper arm direction at
 * `Pose.shoulder == 0`, relative to the torso: 0 = along the torso toward the head, 180 = straight
 * down in a standing rig, 90 = hanging down from a prone torso.
 *
 * [anchor] is translated to the origin after the forward kinematics, so it is the one point that
 * stays still for the whole animation; [contact] says what to draw through it.
 */
data class Rig(
    val bodyAxisDeg: Float,
    val armRestDeg: Float,
    val anchor: Joint,
    val contact: Contact = Contact.None,
)
