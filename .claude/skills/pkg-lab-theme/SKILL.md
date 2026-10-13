---
name: pkg-lab-theme
description: Use when adding or changing the light/dark/system theme choice, its persistence, or the colour scheme of any Kotlin app under android/ (FuelLog, VozReves, Recetario, SleepCycles, or a new one). Says what is shared in :pkgshared (ThemeMode, isDark) and what stays in each app (palette), and the pitfalls (invisible content on a surface-less background, stored ints).
---

# pkg-lab theme

Every app offers the same choice: **System / Light / Dark**. The choice and the "should I draw dark?" decision
are shared; only the colours are the app's identity.

## What is shared (`android/shared`, `com.david7ce.shared.ui`)

| Piece | What for |
|---|---|
| `ThemeMode { System, Light, Dark }` | The choice. `code` is 0 / 1 / 2; `ThemeMode.fromCode(int)` falls back to `System` for unknown values. |
| `isDark(mode)` (`@Composable`) | Whether to draw the dark palette. `System` follows the device. |

Do **not** declare another `ThemeMode` enum or write `when (theme) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }`.

## What stays in the app

- The `lightColorScheme` / `darkColorScheme` palettes and the `<App>Theme(dark, content)` wrapper.
  (VozReves may use dynamic colour on Android 12+; that is its identity, not shared logic.)
- Where the choice is stored: keep each app's existing format so saved settings keep working.
  FuelLog and VozReves store the **int** (`theme.code`); Recetario and SleepCycles store `ThemeMode.name`.
  Read it back with `ThemeMode.fromCode(int)` or `ThemeMode.entries.firstOrNull { it.name == s } ?: System`,
  never `valueOf` (a corrupt value must not crash).
- The label strings ("System", "Light", "Dark") come from the app's own translations.

## Steps

1. Settings: a `SegmentedChoice` of the three modes (see the kotlin-android-app-standards skill), selected =
   the stored mode, `onSelect` persists it.
2. Root: `val dark = isDark(mode)`, pass it to the app theme, and use it for the system bar icons
   (`enableEdgeToEdge` with `SystemBarStyle.auto(...) { dark }`) so they follow the app theme, not only the system.
3. Anything that needs to know light/dark outside the theme (an icon tint, a platform picker) takes `dark` as a
   parameter from step 2. Do not call `isSystemInDarkTheme()` again.

## Pitfalls

- **No `Surface`, no content colour.** Compose Material 3 defaults `LocalContentColor` to black. Text and icons
  drawn directly on a custom background (a `Box` with a gradient or image) are black on a dark palette, which
  is invisible. Put the screen in a `Surface(color = background, contentColor = onBackground)`.
- **Hard-coded colours** must exist for both modes. A colour that works on the dark palette (a pale accent) is
  often unreadable on the light one; pick it from `MaterialTheme.colorScheme` or provide both.
- **State is not colour.** Selected state also changes shape or weight (see the standards skill), because a
  palette swap must not erase the information.
- **A change in `android/shared` rebuilds every app.** Check all four are green in CI.

## Checklist before pushing

1. The app imports `com.david7ce.shared.ui.ThemeMode` and `isDark`; no local copy.
2. The persisted theme value format is unchanged for an existing app.
3. Both modes checked on the main screen: title, icons, cards, dialogs readable.
4. CI green for every app.
