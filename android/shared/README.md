# shared (`:pkgshared`)

Common code for the Kotlin apps under `android/`: **multiplatform** (commonMain = plain Compose).
Used by VozReves, FuelLog, Recetario and SleepCycles. Has no Android resources: text is passed in from each app.

## How to include it

```kotlin
// the app's settings.gradle.kts (the path is relative to the app's folder)
include(":pkgshared")
project(":pkgshared").projectDir = file("../shared")

// app/build.gradle.kts (Android) or shared/build.gradle.kts (KMP)
implementation(project(":pkgshared"))
```

Requirements: Kotlin **2.4.21**, Compose Multiplatform **1.11.1**, Material 3 **1.9.0**, AGP **8.13.x**,
`compileSdk` 36, Gradle 8.14.x. Versions are pinned in this module's `build.gradle.kts`.

## Each app's text

At the root of each app, wrap the content:

```kotlin
CompositionLocalProvider(LocalSharedStrings provides SharedStrings(
    cancel = stringResource(R.string.cancel), back = …, close = …, delete = …, selectAll = …
)) { … }
```

## Pieces

| Piece | What for |
|---|---|
| `ui/SegmentedChoice` | Row selector with an optional icon (no check mark, no overlap). |
| `ui/ChoiceDropdown` | Dropdown for a single choice (language, currency, units…). |
| `ui/ConfirmDialog` | Confirmation before deleting or any irreversible action. |
| `ui/Selection` + `SelectionActions` | Multi-select: long press, select all, delete. |
| `ui/SubScreen`, `SectionTitle` | Top bar for a secondary screen and section titles. |
| `ui/SharedIcons` | Own Material icons (not material-icons, which isn't multiplatform). |
| `ui/SharedStrings` | Common text that each app fills in. |
| `reverseText` | Reverses text by code point (doesn't break emoji). |

Rule: what is **UX or behavior** (confirming deletes, multi-select, selectors, language)
lives here and is the same across every app. What is **identity** (colors, icon, name, own text)
lives in each app. A change in `android/shared/` makes CI rebuild every app.
