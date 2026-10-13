---
name: pkg-lab-icons
description: Use when adding, changing or using an icon in any Kotlin app under android/ (FuelLog, VozReves, Recetario, SleepCycles, or a new one). Says where an icon lives (SharedIcons vs the app), how to name and define it, and what not to do (no material-icons library, no per-icon files, no duplicates).
---

# pkg-lab icons

One icon vocabulary for every app. Generic UI icons live once, in `android/shared` (`SharedIcons`);
only icons that mean something inside one app's domain live in that app.

## Where an icon goes

| The icon means… | Put it in | Example |
|---|---|---|
| A generic UI action or concept (add, back, search, settings, play, star, download, language…) | `android/shared/src/commonMain/kotlin/com/david7ce/shared/ui/SharedIcons.kt` | `SharedIcons.Settings` |
| Something tied to one app's subject (a vehicle, fuel, a dish, reverse playback) | that app's `AppIcons` (FuelLog, VozReves) or `ui/Icons.kt` (Recetario) | `AppIcons.Car`, `AppIcons.Dish` |

Test: *would a second app plausibly want this icon with the same meaning?* If yes, it is shared. When in
doubt, share it: a shared icon costs a few lines, a duplicate costs a divergence.

Current domain icons (the only ones outside `SharedIcons`):
FuelLog — Boat, Bus, Car, Drop, Fuel, Motorcycle, Payments, Plane, Plate, Speed, TrendingDown, TrendingUp,
Truck, Van (+ `AppIcons.of(VehicleType)`). VozReves — Mic, PlayReverse, Quote. Recetario — Dish.

## Steps to use or add one

1. **Look in `SharedIcons` first** (`val X by lazy` entries, alphabetical). Use it:
   `Icon(SharedIcons.Settings, contentDescription = s.settings)` with
   `import com.david7ce.shared.ui.SharedIcons`.
2. **Not there and it is generic → add it to `SharedIcons`**, in alphabetical position:
   ```kotlin
   val Bookmark by lazy {
       icon(
           "Bookmark",
           "M17,3H7c-1.1,0 -1.99,0.9 -1.99,2L5,21l7,-3 7,3V5c0,-1.1 -0.9,-2 -2,-2z",
       )
   }
   ```
   The path is the Material Icons 24 dp path data (from fonts.google.com/icons → SVG → the `d` attribute).
   Break a long path into `"…" +` chunks of about 100 characters.
3. **Not there and domain-specific → add it to the app's own icons object**, same `icon(...)` shape.
4. A change under `android/shared/` rebuilds every app in CI. That is expected; check CI is green.

## Naming: one concept, one name

Use the Material name in PascalCase. Do not add a second name for something that already exists:
back is `ArrowBack` (not `Back`), the minus sign is `Minus` (not `Remove`), the clock-with-arrow is
`History` (not `Restore`). Variants are `Star` / `StarOutline`, `Favorite` / `FavoriteOutline`.

## Rules

- **No `material-icons-extended`** (several MB before shrinking) and no `material-icons-core` in
  `commonMain` (not multiplatform). Paths as data only.
- **No one-file-per-icon and no XML drawables** for in-app icons: they are 3 to 4 lines each and drawables are
  not available in `commonMain`. The Android launcher icon is the one exception (it must be a resource).
- **Tint and size belong to the caller.** Icons are 24 dp with a black fill; the app applies `tint`.
- **Every icon has a `contentDescription` from the app's strings**, or `null` when the adjacent text
  already says it (see the kotlin-android-app-standards skill, accessibility).
- **Do not copy an icon into an app** because it is quicker. If it exists in `SharedIcons`, use it; if it
  does not, add it there.

## Checklist before pushing

1. No icon path data appears twice in the repo (`grep` a distinctive part of the path).
2. Any new generic icon is in `SharedIcons`, alphabetical, `by lazy`.
3. No `AppIcons.X` where `X` is also in `SharedIcons`.
4. CI is green for every app (a `SharedIcons` change touches all of them).
