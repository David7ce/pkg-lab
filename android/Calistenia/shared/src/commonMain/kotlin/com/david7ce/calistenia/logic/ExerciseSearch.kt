package com.david7ce.calistenia.logic

import com.david7ce.calistenia.model.Exercise
import com.david7ce.calistenia.model.Measure
import com.david7ce.calistenia.model.Muscle
import com.david7ce.calistenia.model.MuscleGroup

/** Accent folding, so "dominada" finds "Domináda" and "traction" is not blocked by casing. */
private val FOLD = mapOf(
    'á' to 'a', 'à' to 'a', 'â' to 'a', 'ä' to 'a', 'ã' to 'a', 'å' to 'a',
    'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e',
    'í' to 'i', 'ì' to 'i', 'î' to 'i', 'ï' to 'i',
    'ó' to 'o', 'ò' to 'o', 'ô' to 'o', 'ö' to 'o', 'õ' to 'o', 'ø' to 'o',
    'ú' to 'u', 'ù' to 'u', 'û' to 'u', 'ü' to 'u',
    'ç' to 'c', 'ñ' to 'n', 'ß' to 's',
)

fun foldText(text: String): String = buildString(text.length) {
    for (ch in text.lowercase()) append(FOLD[ch] ?: ch)
}

fun Exercise.matchesQuery(query: String): Boolean {
    val needle = foldText(query.trim())
    return needle.isEmpty() || foldText(name).contains(needle)
}

/** Library filter: free text + optional group + optional measure. */
fun filterExercises(
    exercises: List<Exercise>,
    query: String = "",
    group: MuscleGroup? = null,
    measure: Measure? = null,
): List<Exercise> = exercises.filter { exercise ->
    exercise.matchesQuery(query) &&
        (group == null || exercise.group == group) &&
        (measure == null || exercise.measure == measure)
}

/**
 * One top-level exercise and the variants that belong to it. The parent is always the first variant
 * (the standard version); the rest are sorted by name for a stable selector.
 */
data class ExerciseFamily(val parent: Exercise, val variants: List<Exercise>)

fun familiesOf(exercises: List<Exercise>): List<ExerciseFamily> {
    val childrenByParent = exercises
        .filter { it.parentId != null }
        .groupBy { it.parentId }
    return exercises.filter { it.isTopLevel }.map { parent ->
        val children = childrenByParent[parent.id].orEmpty()
            .filter { it.id != parent.id }
            .sortedBy { it.name }
        ExerciseFamily(parent, listOf(parent) + children)
    }
}

/** The top-level exercise that owns [exercise], or the exercise itself when it is already top-level. */
fun parentOf(exercise: Exercise, exercises: List<Exercise>): Exercise =
    exercise.parentId?.let { parentId -> exercises.firstOrNull { it.id == parentId } } ?: exercise

/**
 * The library filter, family-aware: a variant matches, and its family is shown. Group comes from the
 * parent (all variants share it); measure matches when any variant uses it. [muscles] is the body-map
 * selection: a family passes when any variant trains any selected muscle (OR); empty keeps everything.
 */
fun filterFamilies(
    exercises: List<Exercise>,
    query: String = "",
    group: MuscleGroup? = null,
    measure: Measure? = null,
    muscles: Set<Muscle> = emptySet(),
    favoriteIds: Set<String>? = null,
): List<ExerciseFamily> = familiesOf(exercises).filter { family ->
    (muscles.isEmpty() || family.variants.any { it.trainsAny(muscles) }) &&
        (group == null || family.parent.group == group) &&
        (measure == null || family.variants.any { it.measure == measure }) &&
        (query.isBlank() || family.variants.any { it.matchesQuery(query) }) &&
        (favoriteIds == null || family.variants.any { it.id in favoriteIds })
}
