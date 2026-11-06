package com.david7ce.calistenia.logic

/**
 * Built-in exercises with a Magnific-generated demonstration video ready to ship. Registering one
 * is adding its slug here once the processed file is committed at the path [videoPathFor] computes
 * for it -- the same one-line-per-entry shape as `figureFor` for drawn figures. Most exercises have
 * neither and stay text-only.
 */
private val exerciseVideoSlugs: Set<String> = setOf(
    "pull-up",
    "push-up",
    "squat",
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
