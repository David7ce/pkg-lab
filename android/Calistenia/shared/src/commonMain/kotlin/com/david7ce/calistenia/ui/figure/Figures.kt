package com.david7ce.calistenia.ui.figure

/**
 * Push-up: a straight plank with the hands and toes on the floor, only the arms (and a small hip
 * correction so the feet stay planted) move.
 *
 * The prone rig's body axis is tilted 18.4° above horizontal because that is the angle at which the
 * hands reach the floor: the shoulder is `sin(18.4°)` above the hip, the legs add `1.75 × sin` more
 * down to the foot, and `2.75 × sin(18.4°) ≈ 0.87` is the straight arm length. The arms therefore
 * hang vertically and both contacts land on the same floor line. (The spike used a horizontal body
 * and pinned the hip, which left the feet floating far above the hands.)
 */
private val PronePushUpRig = Rig(
    bodyAxisDeg = -18.4f,
    armRestDeg = 108.4f, // 90° on screen: straight down from the shoulder
    anchor = Joint.Hand,
    contact = Contact.Ground,
)

private val PushUpUp = Pose(torsoLean = 0f, hip = 0f, knee = 0f, shoulder = 0f, elbow = 0f)

/**
 * Bottom of the rep: the shoulder drops to 0.42 above the hand (from 0.87 at the top) by folding
 * the arm — upper arm down-back, forearm down-forward, hand still under the shoulder. The 15° hip
 * tilt is what keeps the toes on the floor once the body has dropped; without it the feet lift.
 */
private val PushUpDown = Pose(torsoLean = 0f, hip = 15f, knee = 0f, shoulder = 61f, elbow = -122f)

val PushUp = FigureAnimation(
    rig = PronePushUpRig,
    frames = listOf(
        Keyframe(PushUpUp, holdMs = 250, moveMs = 850),
        Keyframe(PushUpDown, holdMs = 120, moveMs = 750),
    ),
)

/**
 * Air squat: a standing rig (head up, facing +x) anchored at the ankle so the feet stay planted
 * while the hips drop. Down: torso leaning 35° forward, thigh level with the floor
 * (`90 + 35 - 125 = 0°`), shin angled back 100° from the thigh, arms raised in front as a
 * counterbalance. Because the hip ends up ~0.75 torso lengths behind the ankle, the centre of
 * mass stays over the foot, which is what makes it read as a balanced squat.
 */
private val StandingSquatRig = Rig(
    bodyAxisDeg = -90f,
    armRestDeg = 180f, // straight down from the shoulder when the torso is vertical
    anchor = Joint.Ankle,
    contact = Contact.Ground,
)

private val SquatUp = Pose(torsoLean = 0f, hip = 0f, knee = 0f, shoulder = 0f, elbow = 0f)
private val SquatDown = Pose(torsoLean = 35f, hip = -125f, knee = 100f, shoulder = -125f, elbow = 0f)

val Squat = FigureAnimation(
    rig = StandingSquatRig,
    frames = listOf(
        Keyframe(SquatUp, holdMs = 300, moveMs = 1000),
        Keyframe(SquatDown, holdMs = 250, moveMs = 900),
    ),
)

/**
 * Pull-up: a dead hang from the bar (head up, arms straight overhead) anchored at the hand, so the
 * bar stays put while the body rises. The hang is `armRestDeg = 0` (arm along the torso toward the
 * head) with the hands on the bar line, hence everything else is below `y = 0`.
 *
 * Top: the torso leans back 30° (the bar ends up in front of the chest), the upper arm is folded
 * to horizontal-forward and the forearm back up to the bar, so the shoulder rises from 0.87 to 0.41
 * below the bar and the head reaches the bar line. The knees bend 30° so the legs do not trail
 * stiffly. These angles were solved numerically so the hand stays on the bar with the elbow at
 * shoulder height: the closest the 0.87 arm length allows to "elbows down" without the shoulder
 * passing the bar.
 */
private val HangingPullUpRig = Rig(
    bodyAxisDeg = -90f,
    armRestDeg = 0f,
    anchor = Joint.Hand,
    contact = Contact.Bar,
)

private val PullUpHang = Pose(torsoLean = 0f, hip = 0f, knee = 0f, shoulder = 0f, elbow = 0f)
private val PullUpTop = Pose(torsoLean = -30f, hip = 0f, knee = 30f, shoulder = 120f, elbow = -105f)

val PullUp = FigureAnimation(
    rig = HangingPullUpRig,
    frames = listOf(
        Keyframe(PullUpHang, holdMs = 350, moveMs = 1300),
        Keyframe(PullUpTop, holdMs = 250, moveMs = 1100),
    ),
)

/**
 * The animation for an exercise family, by its parent exercise id, or null when that family is
 * still text-only. This is the one place a new figure gets registered.
 */
fun figureFor(parentId: String): FigureAnimation? = when (parentId) {
    "builtin:push-up" -> PushUp
    "builtin:squat" -> Squat
    "builtin:pull-up" -> PullUp
    else -> null
}
