package com.david7ce.calistenia.logic

import com.david7ce.calistenia.model.Exercise
import com.david7ce.calistenia.model.Measure

/** One line of a starter routine; the measure comes from the exercise itself. */
data class StarterExercise(
    val exerciseId: String,
    val sets: Int,
    val reps: Int? = null,
    val seconds: Int? = null,
    val restSeconds: Int,
)

/**
 * A ready-made routine offered in the Routines tab. It is a template, never seeded as user data:
 * adding it copies it with a new id, so the user can edit or delete it freely and backups never
 * collide with it. Names and notes are localized at the moment it is added.
 */
data class StarterRoutine(
    val key: String,
    val name: String,
    val notes: String,
    /** ISO 639 -> (name, notes) for the five non-English UI languages. */
    val translations: Map<String, Pair<String, String>>,
    val exercises: List<StarterExercise>,
) {
    fun name(languageCode: String): String =
        translations[languageCode.lowercase().take(2)]?.first ?: name

    fun notes(languageCode: String): String =
        translations[languageCode.lowercase().take(2)]?.second ?: notes
}

object BuiltinRoutines {

    private fun line(exerciseId: String, sets: Int, reps: Int? = null, seconds: Int? = null, rest: Int) =
        StarterExercise(exerciseId, sets, reps, seconds, rest)

    val all: List<StarterRoutine> = listOf(
        StarterRoutine(
            key = "morning",
            name = "Morning routine",
            notes = "A gentle full-body start: squat, push, row, plank and glute bridge. About 20 minutes.",
            translations = mapOf(
                "es" to ("Rutina matutina" to
                    "Un comienzo suave de cuerpo completo: sentadilla, empuje, remo, plancha y puente de glúteos. Unos 20 minutos."),
                "fr" to ("Routine du matin" to
                    "Un début doux pour tout le corps : squat, poussée, tirage, planche et pont fessier. Environ 20 minutes."),
                "de" to ("Morgenroutine" to
                    "Ein sanfter Ganzkörperstart: Kniebeuge, Drücken, Rudern, Plank und Glute Bridge. Etwa 20 Minuten."),
                "pt" to ("Rotina matinal" to
                    "Um começo suave de corpo inteiro: agachamento, empurrar, remada, prancha e ponte de glúteos. Cerca de 20 minutos."),
                "it" to ("Routine del mattino" to
                    "Un inizio dolce per tutto il corpo: squat, spinta, trazione, plank e ponte glutei. Circa 20 minuti."),
            ),
            exercises = listOf(
                line("builtin:squat", sets = 3, reps = 12, rest = 60),
                line("builtin:incline-push-up", sets = 3, reps = 8, rest = 60),
                line("builtin:inverted-row", sets = 3, reps = 8, rest = 60),
                line("builtin:plank", sets = 3, seconds = 30, rest = 45),
                line("builtin:glute-bridge", sets = 3, reps = 12, rest = 45),
            ),
        ),
        StarterRoutine(
            key = "full-body",
            name = "Full body",
            notes = "The basics in one session: pull-up, push-up, squat, core and glutes. Three rounds, about 30 minutes.",
            translations = mapOf(
                "es" to ("Cuerpo completo" to
                    "Lo básico en una sesión: dominada, flexión, sentadilla, core y glúteos. Tres rondas, unos 30 minutos."),
                "fr" to ("Corps entier" to
                    "Les bases en une séance : traction, pompe, squat, gainage et fessiers. Trois tours, environ 30 minutes."),
                "de" to ("Ganzkörper" to
                    "Die Grundlagen in einer Einheit: Klimmzug, Liegestütz, Kniebeuge, Rumpf und Gesäß. Drei Runden, etwa 30 Minuten."),
                "pt" to ("Corpo inteiro" to
                    "O básico numa sessão: barra fixa, flexão, agachamento, core e glúteos. Três rondas, cerca de 30 minutos."),
                "it" to ("Corpo intero" to
                    "Le basi in una sessione: trazione, flessione, squat, core e glutei. Tre giri, circa 30 minuti."),
            ),
            exercises = listOf(
                line("builtin:pull-up", sets = 3, reps = 5, rest = 120),
                line("builtin:push-up", sets = 3, reps = 10, rest = 90),
                line("builtin:squat", sets = 3, reps = 15, rest = 90),
                line("builtin:hanging-knee-raise", sets = 3, reps = 10, rest = 60),
                line("builtin:glute-bridge", sets = 3, reps = 15, rest = 60),
            ),
        ),
        StarterRoutine(
            key = "strength",
            name = "Strength",
            notes = "Low reps, long rests: pull-up, dip, archer push-up, Bulgarian split squat and hanging leg raise. About 40 minutes.",
            translations = mapOf(
                "es" to ("Fuerza" to
                    "Pocas repeticiones y descansos largos: dominada, fondo, flexión archer, sentadilla búlgara y elevación de piernas colgado. Unos 40 minutos."),
                "fr" to ("Force" to
                    "Peu de répétitions et longs repos : traction, dip, pompe archer, squat bulgare et relevé de jambes suspendu. Environ 40 minutes."),
                "de" to ("Kraft" to
                    "Wenig Wiederholungen, lange Pausen: Klimmzug, Dip, Archer-Liegestütz, Bulgarian Split Squat und hängendes Beinheben. Etwa 40 Minuten."),
                "pt" to ("Força" to
                    "Poucas repetições e descansos longos: barra fixa, dip, flexão archer, agachamento búlgaro e elevação de pernas pendurado. Cerca de 40 minutos."),
                "it" to ("Forza" to
                    "Poche ripetizioni e recuperi lunghi: trazione, dip, flessione archer, squat bulgaro e sollevamento gambe appeso. Circa 40 minuti."),
            ),
            exercises = listOf(
                line("builtin:pull-up", sets = 5, reps = 3, rest = 180),
                line("builtin:parallel-bar-dip", sets = 5, reps = 3, rest = 180),
                line("builtin:archer-push-up", sets = 4, reps = 5, rest = 150),
                line("builtin:bulgarian-split-squat", sets = 4, reps = 6, rest = 120),
                line("builtin:hanging-leg-raise", sets = 4, reps = 6, rest = 90),
            ),
        ),
        StarterRoutine(
            key = "push",
            name = "Push day",
            notes = "Push-up, pike push-up, dip, diamond push-up and plank. About 30 minutes.",
            translations = mapOf(
                "es" to ("Día de empuje" to
                    "Flexión, pike push-up, fondo, flexión diamante y plancha. Unos 30 minutos."),
                "fr" to ("Jour de poussée" to
                    "Pompe, pike push-up, dip, pompe diamant et planche. Environ 30 minutes."),
                "de" to ("Drücktag" to
                    "Liegestütz, Pike-Liegestütz, Dip, Diamond-Liegestütz und Plank. Etwa 30 Minuten."),
                "pt" to ("Dia de empurrar" to
                    "Flexão, pike push-up, dip, flexão diamante e prancha. Cerca de 30 minutos."),
                "it" to ("Giorno di spinta" to
                    "Flessione, pike push-up, dip, flessione diamante e plank. Circa 30 minuti."),
            ),
            exercises = listOf(
                line("builtin:push-up", sets = 4, reps = 10, rest = 90),
                line("builtin:pike-push-up", sets = 3, reps = 8, rest = 90),
                line("builtin:parallel-bar-dip", sets = 3, reps = 8, rest = 90),
                line("builtin:diamond-push-up", sets = 3, reps = 8, rest = 90),
                line("builtin:plank", sets = 3, seconds = 45, rest = 45),
            ),
        ),
        StarterRoutine(
            key = "pull-core",
            name = "Pull and core",
            notes = "Pull-up, inverted row, chin-up, dead hang and hanging knee raise. About 30 minutes.",
            translations = mapOf(
                "es" to ("Tracción y core" to
                    "Dominada, remo invertido, dominada supina, cuelgue muerto y elevación de rodillas colgado. Unos 30 minutos."),
                "fr" to ("Tirage et gainage" to
                    "Traction, rowing inversé, traction supination, suspension et relevé de genoux suspendu. Environ 30 minutes."),
                "de" to ("Zug und Rumpf" to
                    "Klimmzug, invertiertes Rudern, Untergriff-Klimmzug, Dead Hang und hängendes Knieheben. Etwa 30 Minuten."),
                "pt" to ("Puxar e core" to
                    "Barra fixa, remo invertido, barra fixa supinada, cuelgue morto e elevação de joelhos pendurado. Cerca de 30 minutos."),
                "it" to ("Trazione e core" to
                    "Trazione, rematore invertito, trazione supina, sospensione e sollevamento ginocchia appeso. Circa 30 minuti."),
            ),
            exercises = listOf(
                line("builtin:pull-up", sets = 4, reps = 5, rest = 120),
                line("builtin:inverted-row", sets = 3, reps = 10, rest = 90),
                line("builtin:chin-up", sets = 3, reps = 5, rest = 120),
                line("builtin:dead-hang", sets = 3, seconds = 30, rest = 60),
                line("builtin:hanging-knee-raise", sets = 3, reps = 10, rest = 60),
            ),
        ),
    )
}

/**
 * The editable draft for a starter routine, with its name and notes in [languageCode]. Exercises
 * missing from the catalog are skipped; null when nothing usable is left.
 */
fun StarterRoutine.toDraft(
    catalog: List<Exercise>,
    languageCode: String,
    newKey: () -> String,
): RoutineDraft? {
    val byId = catalog.associateBy { it.id }
    val lines = exercises.mapNotNull { line ->
        val exercise = byId[line.exerciseId] ?: return@mapNotNull null
        RoutineDraftExercise(
            key = newKey(),
            exercise = exercise,
            sets = line.sets,
            targetReps = if (exercise.measure == Measure.Reps) line.reps ?: 1 else null,
            targetSeconds = if (exercise.measure == Measure.Seconds) line.seconds ?: 1 else null,
            restSeconds = line.restSeconds,
        )
    }
    if (lines.isEmpty()) return null
    return RoutineDraft(
        name = name(languageCode),
        notes = notes(languageCode),
        exercises = lines,
    )
}
