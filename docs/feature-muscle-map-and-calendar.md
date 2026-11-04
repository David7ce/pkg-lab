# Muscles and calendar — design before code

STATUS: active — designed 2026-11-04; **M1 done** (muscle data + tags + storage + backup), UI not started.

Two features the owner asked for after seeing the figure work:

1. a **muscle map** to say what you are training today, *before* picking exercises
   or a routine, so the list is filtered by that;
2. a **calendar** to mark routines on days, alongside the existing history.

This document classifies both: what data changes, what UI is added, what is
explicitly *not* touched, and in which order it ships. The rule from the app's
own README still holds — offline, no network, no new dependencies where an
existing one does the job.

---

## 1. What exists today (checked, not remembered)

| Piece | Where | Shape |
|---|---|---|
| `MuscleGroup` | `model/MuscleGroup.kt` | enum of **five** movement groups: `Push, Pull, Legs, Core, Skill` |
| `Exercise` | `model/Exercise.kt` | `id, name, group: MuscleGroup, measure, description, parentId` |
| `Routine` | `model/Routine.kt` | named list of `RoutineExercise(exercise, sets, target, rest)` |
| `WorkoutSession` | `model/Workout.kt` | `startedAt`, `finishedAt?` (null = in progress), `routineId?`, exercises with sets and `completedAt` |
| Screens | `ui/` | `ExercisesScreen`, `RoutinesScreen`, `HistoryScreen`, `RoutineEditorScreen`, `SettingsScreen` |
| Languages | `ui/Translations.kt` | six |

So history already answers "what did I do", and nothing answers "what am I
training today" or "when did I last do legs".

---

## 2. Muscles: the model that makes the map possible

### The mistake to avoid

**Do not replace `MuscleGroup` with a finer enum.** It is stored by name in
every session, exercise and backup; `fromStored` falls back to `Push` on anything
unknown, so shrinking or renaming it silently corrupts old data. It is also the
right taxonomy for browsing — five groups is what the library shows.

### What is added

A second, finer axis, **only for the map and the filter**:

```kotlin
enum class Muscle {          // 12 regions a person can point at on a body
    Chest, Lats, Shoulders, Biceps, Triceps, Forearms,
    Abs, Obliques, LowerBack, Glutes, Quads, Hamstrings, Calves
}
```

- `Exercise.muscles: Set<Muscle>` — additive, defaulted, never authoritative over
  `group`. Missing/unknown names in a backup are dropped, and an empty set is
  filled from the group by `musclesFor(group)`.
- `musclesFor(group)` is the single fallback and the seed for built-ins:
  - Push → Chest, Shoulders, Triceps
  - Pull → Lats, Biceps, Forearms
  - Legs → Quads, Hamstrings, Glutes, Calves
  - Core → Abs, Obliques, LowerBack
  - Skill → Shoulders, Abs, Forearms
- Built-ins that deserve better than the fallback get curated tags in
  `BuiltinExercises` (a dip is Triceps + Chest + Shoulders; a chin-up is
  Biceps + Lats; a glute bridge is Glutes + Hamstrings). This is a data edit per
  exercise, not new machinery.

**Why not "primary/secondary muscle weights":** the filter only needs
"does this exercise train X", and every attempt to model intensity per muscle
ends in a spreadsheet. One set of tags, one predicate.

### The map component

`ui/MuscleMap.kt`, drawn with Compose `Path`s in the same hand-drawn style as
`AppIcons.kt`: a front body and a back body, each region a tappable path.
Front: chest, shoulders, biceps, forearms, abs, obliques, quads.
Back: lats, shoulders, triceps, forearms, lower back, glutes, hamstrings, calves.
Selected = primary tint; unselected = surface variant; the two views share one
selection set. No raster asset, no new dependency, scales with the theme.

### The flow

```
Hoy  ──▶  ¿Qué entrenas hoy?  (body map + chips + "todo")
              │  selected: {Chest, Triceps}
              ▼
          Ejercicios / Rutinas  filtrados por esos músculos
              │
              ▼
          sesión normal (sin cambios)
```

- The selection is **ephemeral**: a filter for this browsing session, not a new
  stored entity. If it should survive restarts, that is a settings field, and a
  decision for later.
- Filter semantics: exercise passes if `muscles` **intersects** the selection
  (OR). Empty selection = everything, exactly as today.
- Routines: a routine passes if **any** of its exercises passes — a legs routine
  is relevant when you tap quads. Matching exercises are highlighted inside it.
- Custom exercises: no tags → group fallback, so they are never invisible.

### Acceptance

- [ ] `musclesFor` and the filter predicate are pure functions with unit tests.
- [ ] Every built-in exercise has tags (test: no built-in has an empty set).
- [ ] Backup round-trip survives unknown muscle names and older files.
- [ ] Selecting a muscle changes the visible exercise list; deselecting restores it.
- [ ] All 12 names exist in the six languages.

---

## 3. Calendar: a second view of the same sessions

### The mistake to avoid

**Do not create a second table of "workouts per day".** A completed session is
already a row with `startedAt`; a calendar that stores its own copy drifts from
history the first time a session is edited or deleted.

### What is added

Exactly one new entity, for the part history cannot know — **the future**:

```kotlin
data class PlannedWorkout(
    val id: String,
    val day: String,          // ISO "2026-11-04", local date, no time, no timezone math
    val routineId: String?,   // null = "something, I'll decide"
    val routineName: String,  // snapshot: a deleted routine must not erase the plan
    val note: String,
)
```

- `day` is a **local calendar date string**. Sessions keep `epoch millis`; the
  calendar derives each session's day with the same local conversion, one
  function used by both sides (`fun dayOf(epoch: Long): String`), so a session
  started at 23:50 belongs to the day it started.
- One plan per day (unique on `day`) — a second plan for the same day replaces
  the first, with an undo snackbar. More than one workout a day is a *session*
  fact, visible in the day sheet, not something the grid has to draw.
- Deleting a routine sets `routineId = null` on plans that point at it and keeps
  `routineName`; the cell still shows "Legs" greyed with "routine deleted".

### The view

`ui/CalendarScreen.kt`: month grid, Monday-first, arrows + "hoy".

| Cell state | Meaning |
|---|---|
| empty | nothing planned, nothing done |
| outline dot | planned, not done |
| filled dot | at least one finished session that day |
| ring + filled | planned and done |
| today | subtle border, always |
| in-progress | pulsing dot (a session with `finishedAt == null`) |

Tap a day → bottom sheet: plans (start / move / remove) and that day's sessions
(open in the existing detail). Long-press → "plan a routine here".

Navigation: `Historial` stays as it is; `Calendario` is a sibling tab, not a
replacement. Both read the same session store; only the plan store is new.

### Acceptance

- [ ] `dayOf` is one function, unit-tested around midnight and DST.
- [ ] Today shows planned + done independently.
- [ ] Deleting a routine leaves the plan readable.
- [ ] History and calendar never disagree about a session (asserted in a test
      that builds a session and reads both views).

---

## 4. Order of work

| Phase | Deliverable | Why this order |
|---|---|---|
| **M1** ✅ | `Muscle` enum, `musclesFor`, `Exercise.muscles`, built-in tags, backup handling, unit tests — **done 2026-11-04** (`MusclesTest`, 7 tests; storage v4 + `3.sqm`) | Data first: the map is useless without tags, and this phase has no UI risk |
| **M2** | `MuscleMap` component + "¿qué entrenas hoy?" surface + exercise filter | Visible, testable, and independent of routines |
| **M3** | Routine filtering + highlight inside the routine | Small once M2 exists |
| **M4** | `PlannedWorkout` store + calendar grid (read-only: plans + finished) | Read-only grid proves the day logic before editing exists |
| **M5** | Plan / move / remove from the day sheet, start from a plan | The write side, on top of a grid that already renders correctly |

Each phase compiles and passes the existing suite on its own; none of them
changes the session or routine format.

## 5. Out of scope (deliberate)

- Automatic programming ("you should train legs tomorrow") — that is a coach, not
  a filter.
- Muscle "fatigue" derived from history — needs a model of recovery the owner
  has not asked for; the map is a filter, not a tracker.
- Synchronisation, notifications per plan, streaks. Later, if ever.
- Replacing the five `MuscleGroup`s, or moving muscle data into routines.
- A separate "calendar" copy of sessions.
