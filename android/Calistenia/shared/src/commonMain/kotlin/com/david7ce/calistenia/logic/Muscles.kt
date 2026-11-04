package com.david7ce.calistenia.logic

import com.david7ce.calistenia.model.Exercise
import com.david7ce.calistenia.model.Muscle
import com.david7ce.calistenia.model.MuscleGroup

/**
 * Which muscles an exercise trains, for the body map and the pre-workout filter.
 *
 * Two rules keep this honest:
 *  - [Exercise.muscles] is the curated answer when it exists;
 *  - otherwise the five [MuscleGroup]s fall back to their obvious members, so a
 *    custom exercise (or a built-in nobody has tagged yet) is never invisible in
 *    a filtered list.
 *
 * The fallback is deliberately generous: a filter that hides a relevant exercise
 * is worse than one that shows an extra.
 */
fun musclesFor(group: MuscleGroup): Set<Muscle> = when (group) {
    MuscleGroup.Push -> setOf(Muscle.Chest, Muscle.Shoulders, Muscle.Triceps)
    MuscleGroup.Pull -> setOf(Muscle.Lats, Muscle.Biceps, Muscle.Forearms)
    MuscleGroup.Legs -> setOf(Muscle.Quads, Muscle.Hamstrings, Muscle.Glutes, Muscle.Calves)
    MuscleGroup.Core -> setOf(Muscle.Abs, Muscle.Obliques, Muscle.LowerBack)
    MuscleGroup.Skill -> setOf(Muscle.Shoulders, Muscle.Abs, Muscle.Forearms)
}

/** The muscles to show and filter by: the curated tags, or the group's fallback. */
fun Exercise.effectiveMuscles(): Set<Muscle> =
    muscles.ifEmpty { musclesFor(group) }

/** True when the exercise trains at least one of the selected muscles (OR semantics). */
fun Exercise.trainsAny(selection: Set<Muscle>): Boolean =
    selection.isEmpty() || effectiveMuscles().any { it in selection }
