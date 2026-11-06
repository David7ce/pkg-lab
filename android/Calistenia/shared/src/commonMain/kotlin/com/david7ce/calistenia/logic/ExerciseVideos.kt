package com.david7ce.calistenia.logic

/**
 * Built-in exercises with a Magnific-generated demonstration video ready to ship. Registering one
 * is adding its slug here once the processed file is committed at the path [videoPathFor] computes
 * for it -- the same one-line-per-entry shape as `figureFor` for drawn figures. Most exercises have
 * neither and stay text-only.
 */
private val exerciseVideoSlugs: Set<String> = setOf(
    "archer-push-up", "back-lever", "bear-crawl", "bench-dip",
    "bulgarian-split-squat", "burpee", "calf-raise", "chin-up",
    "commando-pull-up", "dead-hang", "diamond-push-up", "dragon-flag",
    "glute-bridge", "handstand-hold", "handstand-push-up", "hanging-knee-raise",
    "hanging-leg-raise", "high-knees", "hollow-body-hold", "incline-push-up",
    "inverted-row", "jump-squat", "jumping-jack", "knee-push-up",
    "l-sit", "leg-raise", "mountain-climber", "muscle-up",
    "negative-pull-up", "nordic-curl", "parallel-bar-dip", "pike-push-up",
    "plank", "plank-jack", "pseudo-planche-push-up", "pull-up",
    "push-up", "reverse-lunge", "scapular-pull-up", "side-plank",
    "single-leg-glute-bridge", "skater-jump", "split-squat", "squat",
    "tuck-front-lever", "tuck-planche", "wall-handstand",
)

/**
 * Compose-resources path of a built-in exercise's demonstration video, or null when it doesn't have
 * one (every custom exercise, and most built-ins today).
 *
 * Convention: "builtin:<slug>" maps to `files/exercise_videos/<slug>.mp4` under the shared module's
 * `commonMain/composeResources`, read at runtime with `Res.readBytes(path)` -- no database column,
 * no id scheme of its own, derived straight from the exercise's existing stable id.
 */
fun videoPathFor(exerciseId: String): String? {
    if (!exerciseId.startsWith("builtin:")) return null
    val slug = exerciseId.removePrefix("builtin:")
    return "files/exercise_videos/$slug.mp4".takeIf { slug in exerciseVideoSlugs }
}
