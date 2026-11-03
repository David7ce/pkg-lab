package com.david7ce.calistenia.ui.figure

/**
 * Push-up: a straight plank, only the arms move. Up has the arms nearly straight (the support
 * line close to vertical); down bends them forward and down. Spike for `builtin:push-up` and its
 * variants only -- see `categories/design-art/animation/gym-body-animation.md` in AgentHub for
 * the rest of the families this is meant to grow into.
 */
val PushUp = listOf(
    Pose(torsoLean = 0f, hip = 0f, knee = 0f, shoulder = 10f, elbow = 0f), // up
    Pose(torsoLean = 0f, hip = 0f, knee = 0f, shoulder = 35f, elbow = 90f), // down
)
