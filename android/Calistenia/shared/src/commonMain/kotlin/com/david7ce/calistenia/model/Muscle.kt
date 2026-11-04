package com.david7ce.calistenia.model

/**
 * The fine-grained muscles the map and the filter work with.
 *
 * This is a *second* axis, not a replacement for [MuscleGroup]. The five groups
 * are stored by name in every session, exercise and backup, so renaming or
 * shrinking them would silently corrupt old data; they also remain the right
 * taxonomy for browsing the library. A [Muscle] is what a person points at on a
 * body diagram, and an exercise may train several.
 *
 * Stored by name. Reading an unknown value drops it instead of failing: a
 * backup from a newer version must still import.
 */
enum class Muscle {
    Chest, Lats, Shoulders, Biceps, Triceps, Forearms,
    Abs, Obliques, LowerBack, Glutes, Quads, Hamstrings, Calves;

    companion object {
        fun fromStored(value: String): Muscle? =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }

        /** Comma-separated storage, the shape the exercise column uses. */
        fun parseCsv(value: String): Set<Muscle> =
            value.split(',').mapNotNull { fromStored(it) }.toSet()

        fun toCsv(muscles: Set<Muscle>): String =
            entries.filter { it in muscles }.joinToString(",") { it.name }

        /** Drops unknown names from an imported list, keeping order and de-duplicating. */
        fun fromNames(names: List<String>): Set<Muscle> =
            entries.filter { muscle -> names.any { it.equals(muscle.name, ignoreCase = true) } }.toSet()
    }
}
