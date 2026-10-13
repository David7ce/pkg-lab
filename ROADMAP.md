# pkg-lab — architecture notes, improvements, roadmap

Snapshot as of 2026-10-13. Covers the whole repo (`android/shared`, FuelLog, Recetario, VozReves,
CI). Per-app pending items that are already tracked are not repeated here — see
[`android/Recetario/README.md`](android/Recetario/README.md) ("Desktop pending work",
"Known limitations") and [`android/FuelLog/README.md`](android/FuelLog/README.md).

## What's already right

No rewrite needed here — these are deliberate, working patterns, worth keeping as the bar for new
code:

- **No local build required.** Everything compiles and releases through GitHub Actions
  ([`.github/workflows/android.yml`](.github/workflows/android.yml),
  [`desktop.yml`](.github/workflows/desktop.yml)); path filters build only the app that changed.
- **One shared UI module** (`:pkgshared`) for cross-app UX (confirm dialogs, multi-select,
  selectors) while identity (colors, icon, strings) stays per app — a real, enforced boundary, not
  just a convention.
- **One version catalog** (`android/gradle/libs.versions.toml`) for Kotlin/AGP/Compose/Material3 —
  the versions that have to match across all three apps and `:pkgshared` to compile together.
- **Dependency direction is explicit and tested against**: `ui → data → logic → model` in
  Recetario, `ui → logic → data` in FuelLog. `logic/` has no platform imports, so it runs as plain
  JVM unit tests.
- **Deliberately undependent.** Each README has a "why not X" table (no Room, no image/HTTP/nav/DI
  libraries) with a concrete reason, not "didn't get to it." That's the right default for
  single-maintainer apps this size — don't add any of those back without a real pain point.
- **Integer money/quantity math** in FuelLog (cents, millilitres, thousandths) — no float drift.
- **Validate-then-write, single transaction** for every import/restore path (Recetario backups,
  foreign-format imports, FuelLog restore) — partial writes are structurally impossible, not just
  avoided by convention.
- **Version pinning is intentional**, not stale: Compose Multiplatform 1.11.1 and AGP 8.13.x are
  pinned with a dated reason (1.12.1 needs compileSdk 37, which AGP 8 can't build).
- **Update checker (Recetario)**: a background GET to GitHub Releases on startup, a dismissible
  banner if newer — no secret needed client-side (public releases on a public repo), no
  auto-install. `logic/UpdateCheck.kt`.
- **One Compose smoke test per app**, Robolectric on the JVM — no emulator, runs in the existing
  `testDebugUnitTest` step.

## Open items

- **Watch the first real release.** `android.yml`/`desktop.yml` now add a commit-log section to
  each release's notes (scoped to that app's own paths, since the last release of it). This was
  YAML-validated and reasoned through, but not live-tested against the real `gh` CLI/API — worth
  a glance after the next push to `main` actually cuts a release.
- **Compose-on-Robolectric is less battle-tested than a real device.** The three smoke tests pass
  locally; worth a real-device sanity pass before leaning on them as the only UI-level check.

## Not worth doing (so it's not re-proposed later)

- **DI framework, navigation library, image/HTTP libraries** — each README already justifies
  skipping these with a concrete reason tied to app size. Revisit only if a specific app grows a
  feature that the hand-rolled version can't cover (e.g. Recetario's screen stack hitting real nav
  edge cases), not preemptively.
- **Splitting `:pkgshared` into multiple modules** — one small shared module with ~7 pieces doesn't
  need module boundaries yet; add them when a 4th app needs a *different* subset than the other
  three.
- **Monorepo build tool (Nx/Bazel/etc.)** — three independent Gradle projects plus one shared
  module is exactly what path-filtered GitHub Actions already handles well. Don't add build
  tooling to solve a problem CI doesn't have.
- **A CHANGELOG.md** — git log already is one at this size; what shipped and why lives in commit
  messages, not here. This file stays forward-looking.

## Roadmap

**Per-app items already tracked, not repeated here:**
- Recetario desktop: EXIF rotation, keyboard shortcuts (Esc/Alt+←), PDF export via PDFBox.
- Recetario: AGP 9 migration (`com.android.library` → `com.android.kotlin.multiplatform.library`)
  when bumping Compose Multiplatform past 1.11.1.
- FuelLog: none currently open beyond what's in its README's own notes.

**Someday / only if a real need shows up:**
- A 4th platform folder (`windows/`, `linux/`, `web/` placeholders already exist in the root
  README) — build the workflow when there's an actual app for it, following the same
  detect → matrix → release pattern `android.yml` already uses.
- iOS for Recetario — blocked on an Apple Developer account, already noted in its README; not a
  technical task.

## Release process (current state, for reference)

- **Android:** tag `android-<App>-latest` per app, three ABI-split APKs, replaced on every build
  that touches that app's folder (or `android/shared/`, which rebuilds all three).
- **Desktop:** tag `desktop-recetario-latest`, `.msi`/`.tar.gz`/`.dmg`, replaced on every build
  touching `android/Recetario/` or `android/shared/`.
- Both are rolling "latest" releases, not a version history — there is no per-version GitHub
  Release. That's consistent with "latest APK download" being the actual use case (personal apps,
  not a store listing), so it's listed here as a fact, not a gap to fix.
- **Signing:** one shared key (alias `pkglab`) for every app, encrypted at
  `.github/signing/release.jks.enc`, password in the `ANDROID_KEYSTORE_PASSWORD` GitHub secret
  **and** a password manager (reset 2026-10-13 after the original password was lost — it was only
  ever in the GitHub secret, which can't be read back once set). Losing it again means every
  installed app needs a reinstall on the next release.
