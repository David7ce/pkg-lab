package com.david7ce.calistenia.logic

import com.david7ce.calistenia.model.Exercise
import com.david7ce.calistenia.model.Measure
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
