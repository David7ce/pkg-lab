package com.david7ce.calistenia.model

/**
 * The five movement groups the library is organized by. Stored by name; reading an unknown value
 * falls back to [Push] instead of crashing on a corrupt database or an older/newer backup.
 */
enum class MuscleGroup {
    Push, Pull, Legs, Core, Skill;

    companion object {
        fun fromStored(value: String): MuscleGroup =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Push
    }
}

/** How a set is measured: repetitions or a hold in seconds. */
enum class Measure {
    Reps, Seconds;

    companion object {
        fun fromStored(value: String): Measure =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Reps
    }
}

/** Longest how-to text a custom exercise may store; also the import validation cap. */
const val MAX_EXERCISE_DESCRIPTION = 2000

/**
 * One exercise. [id] is stable forever: "builtin:<slug>" for the catalog seeded by the app, a random
 * id for a custom exercise. Built-ins are never edited or deleted. [description] is the detailed
 * how-to text (English, like the built-in names); custom exercises may leave it empty.
 *
 * [parentId] groups variants under the standard exercise: the parent is a normal exercise with a null
 * parent, and its variants point at it. Every exercise (parent included) is a variant that can be
 * logged, with its own progress; the parent only decides what the library shows as one family.
 */
data class Exercise(
    val id: String,
    val name: String,
    val group: MuscleGroup,
    val measure: Measure,
    val builtin: Boolean,
    val description: String = "",
    val parentId: String? = null,
) {
    val isCustom: Boolean get() = !builtin
    val isTopLevel: Boolean get() = parentId == null
}
