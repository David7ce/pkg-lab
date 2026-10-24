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

/**
 * One exercise. [id] is stable forever: "builtin:<slug>" for the catalog seeded by the app, a random
 * id for a custom exercise. Built-ins are never edited or deleted.
 */
data class Exercise(
    val id: String,
    val name: String,
    val group: MuscleGroup,
    val measure: Measure,
    val builtin: Boolean,
) {
    val isCustom: Boolean get() = !builtin
}
