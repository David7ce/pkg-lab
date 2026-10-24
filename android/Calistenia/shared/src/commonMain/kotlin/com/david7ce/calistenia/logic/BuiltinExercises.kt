package com.david7ce.calistenia.logic

import com.david7ce.calistenia.model.Exercise
import com.david7ce.calistenia.model.Measure
import com.david7ce.calistenia.model.Measure.Reps
import com.david7ce.calistenia.model.Measure.Seconds
import com.david7ce.calistenia.model.MuscleGroup
import com.david7ce.calistenia.model.MuscleGroup.Core
import com.david7ce.calistenia.model.MuscleGroup.Legs
import com.david7ce.calistenia.model.MuscleGroup.Pull
import com.david7ce.calistenia.model.MuscleGroup.Push
import com.david7ce.calistenia.model.MuscleGroup.Skill

/**
 * The exercise catalog every install starts with. Names stay in English (the common language of
 * calisthenics training); groups and the rest of the UI are translated. Ids are stable forever:
 * a later version may add entries, and the repository inserts missing ones without touching user data.
 */
object BuiltinExercises {

    private fun exercise(slug: String, name: String, group: MuscleGroup, measure: Measure) =
        Exercise(id = "builtin:$slug", name = name, group = group, measure = measure, builtin = true)

    val all: List<Exercise> = listOf(
        // Push
        exercise("push-up", "Push-up", Push, Reps),
        exercise("knee-push-up", "Knee Push-up", Push, Reps),
        exercise("incline-push-up", "Incline Push-up", Push, Reps),
        exercise("diamond-push-up", "Diamond Push-up", Push, Reps),
        exercise("archer-push-up", "Archer Push-up", Push, Reps),
        exercise("pseudo-planche-push-up", "Pseudo Planche Push-up", Push, Reps),
        exercise("bench-dip", "Bench Dip", Push, Reps),
        exercise("parallel-bar-dip", "Parallel Bar Dip", Push, Reps),
        exercise("pike-push-up", "Pike Push-up", Push, Reps),
        exercise("handstand-push-up", "Handstand Push-up", Push, Reps),
        // Pull
        exercise("dead-hang", "Dead Hang", Pull, Seconds),
        exercise("scapular-pull-up", "Scapular Pull-up", Pull, Reps),
        exercise("inverted-row", "Inverted Row", Pull, Reps),
        exercise("pull-up", "Pull-up", Pull, Reps),
        exercise("chin-up", "Chin-up", Pull, Reps),
        exercise("negative-pull-up", "Negative Pull-up", Pull, Seconds),
        exercise("commando-pull-up", "Commando Pull-up", Pull, Reps),
        exercise("muscle-up", "Muscle-up", Pull, Reps),
        // Legs
        exercise("squat", "Squat", Legs, Reps),
        exercise("split-squat", "Split Squat", Legs, Reps),
        exercise("bulgarian-split-squat", "Bulgarian Split Squat", Legs, Reps),
        exercise("reverse-lunge", "Reverse Lunge", Legs, Reps),
        exercise("nordic-curl", "Nordic Curl", Legs, Reps),
        exercise("glute-bridge", "Glute Bridge", Legs, Reps),
        exercise("single-leg-glute-bridge", "Single-leg Glute Bridge", Legs, Reps),
        exercise("calf-raise", "Calf Raise", Legs, Reps),
        // Core
        exercise("plank", "Plank", Core, Seconds),
        exercise("side-plank", "Side Plank", Core, Seconds),
        exercise("hollow-body-hold", "Hollow Body Hold", Core, Seconds),
        exercise("leg-raise", "Leg Raise", Core, Reps),
        exercise("hanging-knee-raise", "Hanging Knee Raise", Core, Reps),
        exercise("hanging-leg-raise", "Hanging Leg Raise", Core, Reps),
        exercise("dragon-flag", "Dragon Flag", Core, Reps),
        // Skill
        exercise("wall-handstand", "Wall Handstand", Skill, Seconds),
        exercise("handstand-hold", "Handstand Hold", Skill, Seconds),
        exercise("l-sit", "L-sit", Skill, Seconds),
        exercise("tuck-planche", "Tuck Planche", Skill, Seconds),
        exercise("tuck-front-lever", "Tuck Front Lever", Skill, Seconds),
        exercise("back-lever", "Back Lever", Skill, Seconds),
    )

    private val byId = all.associateBy { it.id }

    fun find(id: String): Exercise? = byId[id]
}
