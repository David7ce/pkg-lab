# Calistenia

A local-only calisthenics training log: exercises, reusable routines, set-by-set workouts with a rest
timer, history and per-exercise progress. Kotlin Multiplatform + Compose Multiplatform: Android,
Windows, Linux and macOS. Spanish, English, French, German, Portuguese and Italian (device language by
default, or chosen in Settings).

- **Library:** 39 built-in bodyweight exercises (push, pull, legs, core, skill) with English names and a
  detailed how-to text (setup, execution, a cue and a regression/progression), plus custom exercises you
  name and describe yourself. Search by name (accents ignored), filter by group and by how the exercise
  is measured (reps or seconds).
- **Routines:** ordered exercises, planned sets, a target (reps or a hold in seconds) and the rest after
  each set. Create, edit, reorder, duplicate, delete. Starting a routine pre-fills every set.
- **Workout:** start from a routine or empty; type reps/seconds and tick each set. Only ticked sets reach
  history. Add sets or exercises mid-workout, notes per exercise and for the session, elapsed time,
  finish or discard with confirmation. An info button on each exercise shows its how-to text without
  leaving the workout. The unfinished session is stored as you go, so closing the app never loses it;
  the Train tab offers to continue.
- **Rest timer:** starts when a set is ticked, with that exercise's rest (or the default in Settings).
  Pause, +15 s, skip. On Android it also posts a notification when the app is in the background (sound
  and vibration follow Settings; the permission is asked the first time a rest timer starts, on
  Android 13+).
- **History:** finished workouts with duration, sets and exercises; detail with every set and note;
  delete with confirmation. Deleting a routine or a custom exercise never damages history.
- **Progress:** per exercise: best set, done sets, total reps or hold time, number of sessions, last
  done and a bar per each of the last ten sessions.
- **Backup:** one JSON file with exercises, routines and finished workouts (Settings → Export/Import).
  Import validates the whole file first and merges by id in one transaction; it never overwrites what is
  already there, so a re-import adds nothing and restoring on a new device is just importing.
- Light/dark/system theme (shared logic with the other pkg-lab apps). No accounts, no network, no ads.
  The only Android permissions are POST_NOTIFICATIONS (optional, rest timer) and VIBRATE.

## Structure

```
Calistenia/
├── app/                          Android packaging only: id, version, signing, ABI splits, icon, manifest
└── shared/                       Kotlin Multiplatform library (androidTarget + jvm("desktop"))
    └── src/
        ├── commonMain/
        │   ├── kotlin/…/model/       Exercise, Routine, WorkoutSession (rows as plain data)
        │   ├── kotlin/…/logic/       pure logic, no platform or UI code:
        │   │     BuiltinExercises · ExerciseSearch · WorkoutMath (progress, week summary)
        │   │     RestTimer (absolute-time state machine) · RoutineDraft (editor + validation)
        │   │     SessionLog (immutable set/exercise edits) · Backup (JSON, validation, merge plan)
        │   │     Formats · UserSettings · AppLanguage
        │   ├── kotlin/…/data/        CalisteniaRepository (SQLDelight; one transaction per write)
        │   ├── kotlin/…/platform/    Services (clock, ids, time text, rest notifier)
        │   ├── kotlin/…/ui/          AppState (navigation + actions), screens, Strings, Translations,
        │   │                         Theme, AppIcons, PlatformUi
        │   └── sqldelight/…/db/      Calistenia.sq (schema version 1)
        ├── jvmCommonMain/            date/time formatting (JVM, both targets)
        ├── androidMain/…/platform/   Application, MainActivity, SQLite driver, pickers, notification
        ├── desktopMain/…/platform/   DesktopMain, JDBC driver, AWT file dialogs, Windows title bar
        ├── commonTest/               logic, strings and backup tests
        └── androidUnitTest/          repository and backup/restore against real SQLite
```

Dependency direction: `ui → data → logic → model`; `platform` interfaces are implemented per target.
`commonMain` has no Android (or JVM-only) API.

## Data

`calistenia.db`, schema version 2 (`Calistenia.sq`; `1.sqm` adds `exercise.description`):
`exercise`, `routine`, `routine_exercise`, `session`, `session_exercise`, `workout_set` and `setting`
(theme, language, default rest, notification options, keep-screen-on). There are no foreign keys:
sessions snapshot the routine name and each exercise's name, measure and targets, so deleting either
never breaks history, and deletes remove children explicitly in one transaction. Children are replaced
as a whole when a routine is saved, so positions stay dense. Built-ins are seeded with
`INSERT OR IGNORE` from stable ids (`builtin:push-up`), and their descriptions are refreshed on every
start, so a later version can add exercises or improve their how-to text without touching user data.

A workout is a normal `session` row with `finishedAt` null; sets are written as they are ticked. On
finish, unticked sets and exercises left with no sets are dropped, then `finishedAt` is set.

## Backup format

`calistenia-backup-YYYY-MM-DD.json`:

```json
{ "format": "calistenia", "version": 2, "exportedAt": 1760000000000, "app": "Calistenia",
  "exercises": [ { "id": "builtin:push-up", "name": "Push-up", "group": "Push", "measure": "Reps",
                   "builtin": true, "description": "Start in a high plank…" } ],
  "routines": [ { "id": "…", "name": "Push day", "notes": "", "createdAt": 0, "updatedAt": 0,
                  "exercises": [ { "id": "…", "exerciseId": "builtin:push-up", "position": 0, "sets": 3,
                                   "targetReps": 8, "targetSeconds": null, "restSeconds": 90 } ] } ],
  "sessions": [ { "id": "…", "name": "Push day", "routineId": "…", "startedAt": 0, "finishedAt": 0,
                  "notes": "Felt good",
                  "exercises": [ { "id": "…", "exerciseId": "builtin:push-up", "name": "Push-up",
                                   "measure": "Reps", "position": 0, "targetSets": 3, "targetReps": 8,
                                   "targetSeconds": null, "restSeconds": 90, "notes": "",
                                   "sets": [ { "id": "…", "position": 0, "reps": 12, "completedAt": 0 } ] } ] } ],
  "settings": { "theme": "System", "language": "System", "defaultRestSeconds": "90" } }
```

Only finished sessions are exported. Newer `version`s are rejected (an older app would otherwise drop
fields silently); unknown fields are ignored. Version 2 adds `exercise.description`; version 1 backups
still import, with an empty description. Import checks the format, version, unique ids, every routine
reference, measures, bounds and the size cap (10 MB), then merges in one transaction: existing top-level
records are skipped with their children, and only settings keys not already stored are written.

## Build and test

CI (`.github/workflows/android.yml` and `desktop.yml`) runs, from this folder:

```
./gradlew assembleRelease -Pabi=<arm64-v8a|armeabi-v7a|universal> --no-daemon
./gradlew testReleaseUnitTest --no-daemon
./gradlew :shared:desktopTest --no-daemon
./gradlew :shared:packageMsi   (Windows) | :shared:packageDmg (macOS) | :shared:createDistributable (Linux)
```

Locally the same commands need JDK 17+ and an Android SDK with platform 36. Downloads:
*Releases → `calistenia-latest`* (APKs, `.msi`, `.tar.gz`, `.dmg`). Desktop data lives in
`%APPDATA%\Calistenia` on Windows and `~/.calistenia` elsewhere.

Tests cover the catalog (ids, descriptions) and search, progress math, the rest timer state machine,
routine validation, immutable set editing, backup parsing/validation/merge, date formats and all six
string sets; the Android unit tests run the repository, the v1 → v2 migration and the full export →
fresh database → import path against real SQLite.

## Known limitations

- Not exercised on physical Android hardware or on macOS/Linux desktops yet. Verified by local debug
  and release builds, the JVM desktop compile and 69 unit tests (including the repository, the v1 → v2
  migration and real SQLite).
- The background rest notification is a delayed job in the app process: it fires while the app is in the
  background, but not if Android kills the process. It is a training aid, not an alarm; no exact-alarm
  permissions are requested.
- Two notification channels are used (alert and silent): turning off both sound and vibration posts the
  silent one; if only one is off, the alert channel may still do both.
- Built-in exercise names and descriptions stay in English (the common language of calisthenics
  training); groups, units and the whole UI are translated. Images, videos and 3D models are not
  included yet.
- No per-side tracking (left/right), no added weight, no body measurements, no charts beyond the last
  ten sessions. Deliberately out of this version.
