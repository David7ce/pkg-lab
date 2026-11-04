# pkg-lab — roadmap

Per-app pending work lives in each app's own README, not here (Recetario: "Desktop pending work",
"Known limitations"; FuelLog: its own notes).

## Open items

- **Watch the first CI runs of SleepCycles.** Written without a local Android build at the time, so
  the first `android.yml` and `desktop.yml` runs were its first compile. A durable Android SDK now
  exists on this machine (2026-11-04, via WSL2 -- see note below), so this and future apps can be
  built and unit-tested locally before relying on CI. Also watch the new per-app `desktop.yml` matrix
  and `sleepcycles-latest` end to end (and re-run Recetario through it), and check the app once on a
  real phone: Sleep now shortcut, 12-hour input, language switch.
- **Watch the first real release under the merged-release change.** `android.yml` and `desktop.yml`
  now both publish Recetario to the same `recetario-latest` release (Android + desktop together,
  see Reference below) instead of two separate ones, without ever deleting the other's assets.
  Reasoned through and syntax-checked (YAML + `bash -n`), the exact `gh` subcommands confirmed
  against `--help`, but not watched end-to-end against a real concurrent run of both workflows yet.
- **Watch the first CI runs of Calistenia.** Built and unit-tested locally, twice: once with a
  temporary Android SDK (debug + release APK, shared/desktop code on the JVM), and again on
  2026-11-04 with the durable WSL2 setup below -- `assembleDebug` succeeded and all 118 JVM unit
  tests passed on the Android test runner (not just desktop), `ExerciseFiguresTest` included. The
  first `android.yml`/`desktop.yml` CI runs, `calistenia-latest`, and a real-phone check are still
  pending: set logging, rest timer notification, backup export/import, language switch, theme.

- **Durable local Android SDK, no sudo (2026-11-04).** `wsl` (Ubuntu) on this machine now has a
  portable Temurin JDK 21 at `~/jdks/temurin-21` and an Android SDK (platform 36, build-tools
  36.0.0, platform-tools) at `~/android-sdk`, both downloaded as plain archives with no root
  needed -- `sudo` here needs an interactive password, so apt-installed JDKs were never an option.
  `android/<App>/local.properties` needs `sdk.dir=/home/d7/android-sdk` (gitignored, per-machine).
  To build or test any app from Windows: `wsl -e bash -lc 'export JAVA_HOME=~/jdks/temurin-21;
  export PATH="$JAVA_HOME/bin:$PATH"; export ANDROID_HOME=~/android-sdk; cd
  /mnt/d/Workspaces/.../android/<App>; bash gradlew assembleDebug'` (use `bash gradlew`, not
  `./gradlew` -- the extracted wrapper may not carry the exec bit, and `bash` avoids needing it).
  Building against `/mnt/d/...` works but is slower than a native Linux path (9p filesystem).
- **Calistenia: exercise animation, in progress.** Direction changed from "text-only": instead of 3D
  models, photos or video (storage, licensing, a rig per exercise), exercises get a code-drawn 2D
  profile stick figure (`ui/figure/`: `Rig` orientation + `Pose` angles + `FigureAnimation` keyframes,
  drawn by `Figure`), so there are no assets to ship. Push-up, squat, pull-up, dip, plank and L-sit are done (pull-up is the
  first bar exercise: hanging rig, and `Contact.Bar` now draws a thick bar with end caps via
  `contactStrokes`, distinct from the floor line); `figureFor(parentId)` is the registry, every other
  family is still text-only (dip is a `Contact.Bar` support rig registered for `builtin:parallel-bar-dip`;
  bench dip stays text-only; plank is the first static hold: a forearm plank via
  `FigureAnimation.hold` on a prone rig anchored at the elbow; L-sit is a hold on the dip's
  support rig, straight arms and legs raised to horizontal, registered for `builtin:l-sit`).
  Remaining: other holds, the rest of the families, and a real-device look at how the
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
