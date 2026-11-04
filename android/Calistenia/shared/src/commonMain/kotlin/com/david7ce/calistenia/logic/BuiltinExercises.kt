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
 * The exercise catalog every install starts with. Names and how-to descriptions stay in English
 * (the common language of calisthenics training); groups and the rest of the UI are translated.
 * Ids are stable forever: a later version may add entries, and the repository inserts missing ones
 * and refreshes built-in descriptions without touching user data.
 */
object BuiltinExercises {

    /**
     * Variants grouped under the standard exercise. Ids are stable; the parent is always a built-in
     * that appears in the catalog too. Everything not listed here is a top-level family of one.
     */
    private val parentBySlug: Map<String, String> = mapOf(
        "knee-push-up" to "builtin:push-up",
        "incline-push-up" to "builtin:push-up",
        "diamond-push-up" to "builtin:push-up",
        "archer-push-up" to "builtin:push-up",
        "pseudo-planche-push-up" to "builtin:push-up",
        "pike-push-up" to "builtin:push-up",
        "handstand-push-up" to "builtin:push-up",
        "bench-dip" to "builtin:parallel-bar-dip",
        "dead-hang" to "builtin:pull-up",
        "scapular-pull-up" to "builtin:pull-up",
        "inverted-row" to "builtin:pull-up",
        "negative-pull-up" to "builtin:pull-up",
        "chin-up" to "builtin:pull-up",
        "commando-pull-up" to "builtin:pull-up",
        "muscle-up" to "builtin:pull-up",
        "split-squat" to "builtin:squat",
        "bulgarian-split-squat" to "builtin:squat",
        "reverse-lunge" to "builtin:squat",
        "single-leg-glute-bridge" to "builtin:glute-bridge",
        "side-plank" to "builtin:plank",
        "hanging-knee-raise" to "builtin:leg-raise",
        "hanging-leg-raise" to "builtin:leg-raise",
        "dragon-flag" to "builtin:leg-raise",
        "wall-handstand" to "builtin:handstand-hold",
    )

    private fun exercise(
        slug: String,
        name: String,
        group: MuscleGroup,
        measure: Measure,
        description: String,
    ) = Exercise(
        id = "builtin:$slug",
        name = name,
        group = group,
        measure = measure,
        builtin = true,
        description = description,
        parentId = parentBySlug[slug],
    )

    val all: List<Exercise> = listOf(
        // Push
        exercise(
            "push-up", "Push-up", Push, Reps,
            "Start in a high plank with the hands slightly wider than the shoulders and the body in one line. " +
                "Lower the chest between the hands, keeping the elbows at roughly 45° from the torso, then press " +
                "back up without letting the hips sag. Cue: squeeze the glutes and brace the core so the chest and " +
                "hips lower together. Make it easier with an incline or the knees; make it harder with a slower " +
                "lowering or the feet elevated.",
        ),
        exercise(
            "knee-push-up", "Knee Push-up", Push, Reps,
            "Kneel with the hands under the shoulders, knees on the floor and a straight line from the knees to " +
                "the head. Lower the chest between the hands and press up without bending at the hips. It builds " +
                "the same pushing pattern as a full push-up; keep the core braced so the lower back does not arch. " +
                "Move to a full push-up once you can do several clean sets.",
        ),
        exercise(
            "incline-push-up", "Incline Push-up", Push, Reps,
            "Place the hands on a bench, table or step and walk the feet back until the body is straight. Lower " +
                "the chest to the edge and press away. The higher the surface, the easier the set; lower it as you " +
                "get stronger. Keep the elbows tracking back at about 45° and the body in one line.",
        ),
        exercise(
            "diamond-push-up", "Diamond Push-up", Push, Reps,
            "Set the hands close together under the chest, with the index fingers and thumbs forming a diamond. " +
                "Keep the elbows close to the ribs as you lower, then press up. It shifts work to the triceps and " +
                "the inner chest. Do not let the hips pike up or the shoulders shrug toward the ears.",
        ),
        exercise(
            "archer-push-up", "Archer Push-up", Push, Reps,
            "Take a wide hand position and shift your weight over one arm as you lower, keeping the other arm " +
                "nearly straight. Press back to the center and alternate sides. It is a step toward the one-arm " +
                "push-up; keep the hips square instead of rotating, and let the working elbow stay close to the body.",
        ),
        exercise(
            "pseudo-planche-push-up", "Pseudo Planche Push-up", Push, Reps,
            "Place the hands by the hips with the fingers turned out and lean the shoulders forward over the " +
                "hands. Lower and press while keeping that forward lean. It loads the shoulders and the core " +
                "heavily. Keep the body in one line and do not let the lower back sag or the elbows flare wide.",
        ),
        exercise(
            "bench-dip", "Bench Dip", Push, Reps,
            "Sit on a bench, place the hands beside the hips and slide the hips off the edge with the legs in " +
                "front. Bend the elbows to lower until the upper arms are about parallel to the floor, then press " +
                "up. Keep the shoulders down and the back close to the bench. Do not sink into the shoulders or " +
                "bounce at the bottom.",
        ),
        exercise(
            "parallel-bar-dip", "Parallel Bar Dip", Push, Reps,
            "Support yourself on parallel bars with straight arms, shoulders down and legs hanging or bent. Lower " +
                "until the upper arms are about parallel to the ground, then press up. Lean slightly forward to " +
                "load the chest or stay upright for the triceps. Skip it if the shoulders hurt, and build up with " +
                "bench dips first.",
        ),
        exercise(
            "pike-push-up", "Pike Push-up", Push, Reps,
            "From a push-up position, walk the feet toward the hands and lift the hips into an inverted V. Bend " +
                "the elbows to lower the head toward the floor between the hands, then press back. It builds the " +
                "overhead press for handstand work. Keep the elbows tracking forward rather than flaring wide, and " +
                "keep the hips high.",
        ),
        exercise(
            "handstand-push-up", "Handstand Push-up", Push, Reps,
            "Kick up to a handstand against a wall with the hands slightly wider than the shoulders. Lower the " +
                "head toward the floor under control and press back to a straight handstand. Only attempt it when " +
                "you can hold a wall handstand and do pike push-ups. Keep the core tight and the ribs down instead " +
                "of arching the back.",
        ),

        // Pull
        exercise(
            "dead-hang", "Dead Hang", Pull, Seconds,
            "Hang from a bar with an overhand grip, arms straight and the shoulders slightly pulled down away " +
                "from the ears. Hold for time while breathing normally. It builds grip and shoulder endurance. Do " +
                "not swing, and do not let the shoulders shrug up around the ears; come down before the grip fails " +
                "completely.",
        ),
        exercise(
            "scapular-pull-up", "Scapular Pull-up", Pull, Reps,
            "Hang with straight arms, then pull the shoulder blades down and back so the body rises a few " +
                "centimeters without bending the elbows. Pause, then lower under control. It teaches the first part " +
                "of the pull-up. Keep the ribs down and avoid swinging; think of moving the shoulder blades, not " +
                "the hands.",
        ),
        exercise(
            "inverted-row", "Inverted Row", Pull, Reps,
            "Set a bar at waist height and hang underneath it with a straight body and the heels on the floor. " +
                "Pull the chest to the bar, squeezing the shoulder blades, then lower under control. The lower the " +
                "bar, the harder it is. Keep the hips up and the body in one line instead of leading with the chin.",
        ),
        exercise(
            "pull-up", "Pull-up", Pull, Reps,
            "Hang from a bar with an overhand grip slightly wider than the shoulders. Pull the chest toward the " +
                "bar by driving the elbows down, then lower to a full hang under control. Avoid kipping or " +
                "swinging. If you cannot do one yet, use negatives or inverted rows and keep practicing the full " +
                "range.",
        ),
        exercise(
            "chin-up", "Chin-up", Pull, Reps,
            "Hang with an underhand grip about shoulder-width apart. Pull until the chin clears the bar, then " +
                "lower all the way. The underhand grip uses more biceps than a pull-up. Keep the shoulders active " +
                "and the ribs down instead of arching the back to reach the top.",
        ),
        exercise(
            "negative-pull-up", "Negative Pull-up", Pull, Seconds,
            "Start at the top of a pull-up, with the chin over the bar, by jumping or stepping up. Lower yourself " +
                "as slowly as possible, aiming for three to five seconds, until the arms are straight. It builds " +
                "strength for the first pull-up. Reset by stepping up again, never by swinging.",
        ),
        exercise(
            "commando-pull-up", "Commando Pull-up", Pull, Reps,
            "Hang from a bar with a neutral grip, one hand in front of the other. Pull up so one shoulder passes " +
                "the bar, then lower and alternate sides. Keep the body close to the bar and avoid twisting. It is " +
                "a demanding pull-up variation; master the standard pull-up first.",
        ),
        exercise(
            "muscle-up", "Muscle-up", Pull, Reps,
            "Hang from the bar, pull explosively and transition the elbows over the bar into a straight-arm " +
                "support. Lower under control or reverse the movement. It needs a strong pull-up and dip. Learn the " +
                "transition on a low bar and with a false grip before adding height or speed.",
        ),

        // Legs
        exercise(
            "squat", "Squat", Legs, Reps,
            "Stand with the feet about shoulder-width apart and the toes slightly out. Sit the hips back and down, " +
                "keeping the chest up and the knees tracking over the toes, then stand. Go as low as you can without " +
                "the lower back rounding. Drive through the whole foot, not only the toes.",
        ),
        exercise(
            "split-squat", "Split Squat", Legs, Reps,
            "Step one foot forward and lower the back knee toward the floor, keeping the front shin roughly " +
                "vertical. Press through the front foot to stand. Keep the torso upright and the hips square. The " +
                "back knee should come close to the floor without slamming into it; hold a wall or chair if balance " +
                "is the limit.",
        ),
        exercise(
            "bulgarian-split-squat", "Bulgarian Split Squat", Legs, Reps,
            "Place the top of the back foot on a bench behind you and stand on the front leg. Lower straight down " +
                "until the front thigh is near parallel to the floor, then press up. It is a strong single-leg " +
                "builder. Keep the front knee over the foot and the torso tall; start with a low bench if the " +
                "position feels unstable.",
        ),
        exercise(
            "reverse-lunge", "Reverse Lunge", Legs, Reps,
            "Stand tall, step one foot back and lower both knees until the back knee is just above the floor. Push " +
                "through the front foot to return. Stepping back is easier on the knees than stepping forward. Keep " +
                "the torso upright and the front knee tracking over the toes.",
        ),
        exercise(
            "nordic-curl", "Nordic Curl", Legs, Reps,
            "Kneel with the ankles anchored and the body in a straight line from the knees to the head. Lower the " +
                "torso forward as slowly as possible using the hamstrings, then push back with the hands if needed. " +
                "It is very demanding. Start with a short range and increase it over time; never let the hips break " +
                "the straight line.",
        ),
        exercise(
            "glute-bridge", "Glute Bridge", Legs, Reps,
            "Lie on your back with the knees bent and the feet flat, close to the hips. Press through the heels and " +
                "lift the hips until the body forms a line from the knees to the shoulders, then lower. Squeeze the " +
                "glutes at the top. Do not arch the lower back to get higher.",
        ),
        exercise(
            "single-leg-glute-bridge", "Single-leg Glute Bridge", Legs, Reps,
            "Lie on your back with one foot flat and the other leg extended or the knee hugged toward the chest. " +
                "Lift the hips with the working leg, keeping the pelvis level, then lower. It exposes side-to-side " +
                "weakness. Do not let the hip on the free side drop; reduce the range before you lose alignment.",
        ),
        exercise(
            "calf-raise", "Calf Raise", Legs, Reps,
            "Stand with the balls of the feet on a step and the heels hanging off the edge. Rise as high as " +
                "possible onto the toes, pause, then lower the heels below the step for a stretch. Keep the knees " +
                "straight but not locked. Move slowly instead of bouncing, and hold something for balance if needed.",
        ),

        // Core
        exercise(
            "plank", "Plank", Core, Seconds,
            "Set the forearms on the floor under the shoulders and extend the legs, with the body in one straight " +
                "line. Brace the core, squeeze the glutes and breathe normally. Do not let the hips sag or pike up. " +
                "End the set when the lower back starts to ache, not when the clock says so.",
        ),
        exercise(
            "side-plank", "Side Plank", Core, Seconds,
            "Lie on one side with the elbow under the shoulder and the feet stacked. Lift the hips so the body " +
                "forms a straight line, then hold. Keep the top shoulder over the bottom one and do not let the hips " +
                "roll back. Switch sides for equal time; drop to the bottom knee if the hips cannot stay up.",
        ),
        exercise(
            "hollow-body-hold", "Hollow Body Hold", Core, Seconds,
            "Lie on your back, press the lower back into the floor and lift the shoulder blades and legs a few " +
                "centimeters. Hold with the ribs down and the arms by the ears or across the chest. If the lower " +
                "back lifts off the floor, bend the knees or raise the legs higher until it stays down.",
        ),
        exercise(
            "leg-raise", "Leg Raise", Core, Reps,
            "Lie on your back with the legs straight and the hands beside the hips or under the glutes. Lift the " +
                "legs to vertical, then lower them slowly without letting the lower back arch. Bend the knees if the " +
                "back starts to lift. A smaller range with control beats a low range with momentum.",
        ),
        exercise(
            "hanging-knee-raise", "Hanging Knee Raise", Core, Reps,
            "Hang from a bar with active shoulders. Draw the knees toward the chest, curl the pelvis slightly and " +
                "lower under control. Avoid swinging. If the body starts to swing, pause at the bottom until it " +
                "stops before the next repetition.",
        ),
        exercise(
            "hanging-leg-raise", "Hanging Leg Raise", Core, Reps,
            "Hang from a bar and lift straight legs to hip height or higher, tipping the pelvis back at the top. " +
                "Lower slowly without swinging. It is the harder version of the knee raise. Start with bent knees " +
                "and straighten them as you gain control.",
        ),
        exercise(
            "dragon-flag", "Dragon Flag", Core, Reps,
            "Lie on a bench and grip behind your head, then lift the body up to the shoulders and lower it as one " +
                "straight line under control. Keep the ribs down and the glutes tight. It is an advanced movement. " +
                "Start with tuck or negative versions and never let the lower back arch.",
        ),

        // Skill
        exercise(
            "jumping-jack", "Jumping Jack", Skill, Reps,
            "Stand tall with the arms at the sides and the feet together. Jump the feet out wider than the " +
                "shoulders while sweeping the arms overhead, then jump back to the start in one rhythm. Stay on " +
                "the balls of the feet, keep the knees soft and the core braced. Use it as a warm-up: the point is " +
                "the heart rate and the shoulders, not speed. Step the feet out instead of jumping if the impact " +
                "bothers the knees.",
        ),
        exercise(
            "wall-handstand", "Wall Handstand", Skill, Seconds,
            "Place the hands about 15 to 20 cm from a wall, kick or walk up until the chest faces the wall and the " +
                "body is straight. Push through the shoulders, keep the ribs down and hold. Come down under " +
                "control. Once you can hold it comfortably, practice facing away from the wall with the heels " +
                "resting on it.",
        ),
        exercise(
            "handstand-hold", "Handstand Hold", Skill, Seconds,
            "Kick up to a freestanding handstand with the hands shoulder-width apart, fingers spread and shoulders " +
                "pushed tall. Find balance with small finger and wrist adjustments, keeping the body in one line. " +
                "Practice near a wall and learn to bail forward or sideways. It takes many short attempts rather " +
                "than one long hold.",
        ),
        exercise(
            "l-sit", "L-sit", Skill, Seconds,
            "Support yourself on parallettes or the floor with straight arms, shoulders down and legs lifted to a " +
                "90° angle. Hold with the core braced and the toes pointed. If the legs cannot stay up, tuck the " +
                "knees or raise them one at a time. Do not shrug into the shoulders; press the floor away.",
        ),
        exercise(
            "tuck-planche", "Tuck Planche", Skill, Seconds,
            "Place the hands by the hips with the fingers turned out, lean the shoulders forward over the hands and " +
                "lift the knees toward the chest with the hips high. Hold with straight arms and a rounded upper " +
                "back. It is a planche progression. Keep the forward lean and do not let the feet touch down " +
                "between attempts.",
        ),
        exercise(
            "tuck-front-lever", "Tuck Front Lever", Skill, Seconds,
            "Hang from a bar and pull the shoulder blades down, then lift the knees to the chest and lean back until " +
                "the back is roughly parallel to the floor. Hold with straight arms and a tight tuck. It is a " +
                "front-lever progression. Keep the hips from dropping and the arms locked; open the tuck only when " +
                "the hold is solid.",
        ),
        exercise(
            "back-lever", "Back Lever", Skill, Seconds,
            "From a German hang or an inverted position on a bar, lower the body behind the bar with straight arms " +
                "until it is roughly parallel to the floor. Keep the shoulders active and the whole body tight. It " +
                "is an advanced movement. Build up with tuck and straddle versions and warm up the shoulders " +
                "thoroughly.",
        ),
    )

    private val byId = all.associateBy { it.id }

    fun find(id: String): Exercise? = byId[id]
}
