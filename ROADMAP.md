# pkg-lab — roadmap

Per-app pending work lives in each app's own README, not here (Recetario: "Desktop pending work",
"Known limitations"; FuelLog: its own notes).

## Open items

- **Watch the first CI runs of SleepCycles.** Written without an Android SDK or any local build (this
  machine has none), so the first `android.yml` and `desktop.yml` runs are its first compile. Also watch
  the new per-app `desktop.yml` matrix and `sleepcycles-latest` end to end (and re-run Recetario through
  it), and check the app once on a real phone: Sleep now shortcut, 12-hour input, language switch.
- **Watch the first real release under the merged-release change.** `android.yml` and `desktop.yml`
  now both publish Recetario to the same `recetario-latest` release (Android + desktop together,
  see Reference below) instead of two separate ones, without ever deleting the other's assets.
  Reasoned through and syntax-checked (YAML + `bash -n`), the exact `gh` subcommands confirmed
  against `--help`, but not watched end-to-end against a real concurrent run of both workflows yet.
- **Watch the first CI runs of Calistenia.** Built and unit-tested locally (temporary Android SDK,
  debug + release APK, and the shared/desktop code compiled on the JVM), but the first `android.yml` and
  `desktop.yml` runs, `calistenia-latest`, and a real-phone check are still pending: set logging, rest
  timer notification, backup export/import, language switch, theme.
- **Calistenia: exercise animation, in progress.** Direction changed from "text-only": instead of 3D
  models, photos or video (storage, licensing, a rig per exercise), exercises get a code-drawn 2D
  profile stick figure (`ui/figure/`: `Rig` orientation + `Pose` angles + `FigureAnimation` keyframes,
  drawn by `Figure`), so there are no assets to ship. Push-up, squat and pull-up are done (pull-up is the
  first bar exercise: hanging rig, and `Contact.Bar` now draws a thick bar with end caps via
  `contactStrokes`, distinct from the floor line); `figureFor(parentId)` is the registry, every other
  family is still text-only. Remaining: dip (bar/parallel-bars rig), plank
  and other holds (`FigureAnimation.hold`), the rest of the families, and a real-device look at how the
  figure reads (never checked visually, only geometry-tested). Photos/video stay out of scope.
- **Compose-on-Robolectric smoke tests** pass locally; worth a real-device check too before
  relying on them as the only UI-level test.

## Not worth doing

- **A SleepCycles background that follows the hours of daylight.** It needs the user location (a
  permission, and a network or GPS source) to know sunrise and sunset by latitude, for a purely cosmetic
  effect. Plain light and dark, shared with the other apps, was chosen instead.

Splitting `:pkgshared` into modules, or a monorepo build tool (Nx/Bazel) — four Gradle projects
and path-filtered CI already handle this fine. Revisit only if a 5th app needs it.

## Reference

- **Releases** are rolling `latest` tags per app (`android-<App>-latest`; Recetario's is
  `recetario-latest`, SleepCycles has `sleepcycles-latest`, Calistenia has `calistenia-latest`, each
  shared by Android and desktop), not a version history — intentional, not a gap. Titles are
  `<App> <version>`, no platform suffix — the asset filenames already say that.
- **Signing**: one key (alias `pkglab`), encrypted at `.github/signing/release.jks.enc`, password
  in the `ANDROID_KEYSTORE_PASSWORD` GitHub secret *and* a password manager (reset 2026-10-13 —
  the original was only ever in the secret, unrecoverable once set). Losing it again means every
  installed app needs a reinstall on the next release.
