# SleepCycles — design

Native port of the PWA [David7ce/sleep-cycles-calc](https://github.com/David7ce/sleep-cycles-calc)
to pkg-lab: Android plus desktop (Windows, macOS, Linux), same Compose UI on every target.

## Decisions (agreed with the owner)

| Topic | Decision |
|---|---|
| Approach | Native rewrite in Kotlin Multiplatform + Compose Multiplatform, same layout as Recetario. Not a WebView. |
| Location / names | `android/SleepCycles/`, application id and namespace `com.david7ce.sleepcycles`, app name `SleepCycles`. |
| Languages | es, en, fr, de, pt, it. Device language by default, or chosen in Settings. |
| Look | Night-sky background (stars, mountains) drawn with Compose `Canvas` in dark mode; plain Material 3 in light. Theme: light / dark / system. |
| Scope | Only what the PWA has. No alarms, history or anything new. |
| Source repo | `sleep-cycles-calc` stays as is; the PWA keeps being published. |
| Rename | `com/deivon` source folders and the `vendor` field renamed to `david7ce` (done before this spec; path-only, packages were already `com.david7ce.*`). |
| License | PolyForm Strict 1.0.0 for all of pkg-lab (root `LICENSE`). Source-available, not OSI open source: readable and usable noncommercially, no modification or redistribution. The PWA repo keeps the Apache-2.0 it already shipped under. |

## Structure

```
android/SleepCycles/
├── app/        Android packaging only: id, version, signing, ABI splits, icon, manifest, shortcut
└── shared/     Kotlin Multiplatform library (androidTarget + jvm("desktop"))
    └── src/
        ├── commonMain/kotlin/com/david7ce/sleepcycles/
        │     logic/      SleepCalc, AgeRange, Rating, TimeFormat   (pure, no UI or platform)
        │     platform/   Services: SettingsStore, Clock            (interfaces)
        │     ui/         AppState, MainScreen, SettingsScreen, HelpDialog,
        │                 NightSky, RatingFace, Strings, Translations, Theme
        ├── androidMain/  SharedPreferences store, system clock, MainActivity, Application
        ├── desktopMain/  java.util.prefs store, system clock, DesktopMain, window title bar
        └── commonTest/   logic tests (also run on the desktop target)
```

Dependency direction `ui → logic`; platform pieces sit behind the interfaces in `platform/`.
`commonMain` has no Android or JVM-only API. Uses `:pkgshared` (`SegmentedChoice`, `ChoiceDropdown`,
`SubScreen`, `SectionTitle`, `LocalSharedStrings`). No SQLDelight, no network, no extra dependencies.

## Logic (exact port of `script.js`)

Time is minutes of the day (0..1439); results wrap modulo 1440. No date library.

- Cycle = 90 min. Suggestions for `n` in `[6, 5, 4, 3, 2, 1]` (hours = `n * 1.5`).
- Latency: minutes to fall asleep; invalid or negative input counts as 0.
- Mode *wake at* (input = wake time): bedtime = `wake − (latency + n·90)`.
- Mode *go to bed at* (input = bed time): wake time = `bed + latency + n·90`.
- Age ranges (recommended hours): child 9–11, teen 8–10, adult 7–9, senior 7–8; unknown → adult.
- Rating of `hours`: `good` if within `[lo, hi]`; `ok` if within `[lo − 1.5, hi + 1.5]`, or exactly 4.5; else `low`.
- Defaults: mode wake, wake 07:00, bed 23:00, age adult, latency 15, format 24 h.
- *Sleep now*: switch to bed mode, set bed time to the current time, persist it, calculate.
- Persisted: mode, wake time, bed time, age, latency, format (+ theme and language).

## UI

- **Main (single screen):** `SegmentedChoice` for the mode (each with its own accent); time picker (AM/PM
  toggle when format is 12 h); *Sleep now* button; six cards, each with time, "n cycles · X h" and a
  rating face. Hint line "1 cycle = 90 min · includes N min to fall asleep".
- **Settings** (`SubScreen`, most-used first): age (`ChoiceDropdown`), time to fall asleep, 12/24 h,
  theme, language ("System default" first; change recreates the activity on Android), version at the bottom.
- **How does it work?** a dialog.
- **Rating faces** are drawn with `Canvas` (as the PWA draws SVG faces), not emoji: color emoji render
  badly on desktop JVM/Linux. Selected state never relies on color alone.
- 24 dp gutters, 48 dp touch targets, times always left-to-right (also in Arabic-like locales), nothing
  overlapping at large font.
- Texts: `Strings` base + `Translations`, feeding `LocalSharedStrings` at the root, same as Recetario.

## Platform

- `SettingsStore`: string key/value. Android `SharedPreferences`; desktop `java.util.prefs`.
- `Clock`: current minutes of the day.
- Android static shortcut "Sleep now" (equivalent to the PWA's `?mode=sleep-now`).
- Desktop: `DesktopMain` window, Windows title bar follows the theme, like Recetario.

## CI and releases

- Android: `android.yml` detects the new folder; APKs arm64-v8a, armeabi-v7a, universal, shared signing key.
- Desktop: `desktop.yml` is Recetario-specific today; generalize it to a per-app matrix and add SleepCycles
  (`.msi`, Linux `tar.gz`, macOS `.dmg`). Android and desktop publish to one release, `sleepcycles-latest`,
  each touching only its own asset types (the Recetario pattern); title `SleepCycles <version>`.
- Desktop installer version `1.0.<run number>`, app version from `GITHUB_RUN_NUMBER` like Recetario.
- Docs: root README (structure, release names, license), ROADMAP (release-name reference), app README.
- `LICENSE` at the repo root: PolyForm Strict 1.0.0, official text, plus a short license note in the README.

## Testing

- `commonTest`: calculation in both modes (including wrap past midnight), latency 0 and invalid, every
  rating boundary (`lo`, `hi`, `lo−1.5`, `hi+1.5`, 4.5, below), unknown age, 12/24 h formatting and parsing.
- Translations test (every language has every key), like Recetario's `StringsTest`.
- `.\scripts\build-and-test.ps1 -App SleepCycles` locally; CI is the source of truth.
- Not verifiable here: behavior on a real Android device and on macOS/Linux desktops.

## Out of scope

Alarms or notifications, history, wearables, iOS, reproducing the PWA's service worker or install flow,
any change to the PWA repo.
