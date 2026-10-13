# pkg-lab — architecture notes, improvements, roadmap

Snapshot as of 2026-10-12. Covers the whole repo (`android/shared`, FuelLog, Recetario, VozReves,
CI). Per-app pending items that are already tracked are not repeated here — see
[`android/Recetario/README.md`](android/Recetario/README.md) ("Pendiente en escritorio",
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
  pinned with a dated reason in the README (1.12.1 needs compileSdk 37, which AGP 8 can't build).

## Shipped since this was written

- **Settings UX parity (Recetario):** theme is now `SegmentedChoice` and language is `ChoiceDropdown`
  (both from `:pkgshared`), matching FuelLog/VozReves instead of a hand-rolled radio list. Data
  section moved first.
- **Windows title bar theme (Recetario desktop):** the native title bar now follows the app's
  light/dark setting via `DwmSetWindowAttribute` (JNA), instead of staying whatever Windows defaults to.
- **Update checker (Recetario, Android + Windows/Linux/macOS):** on startup, the app does one GET to
  `api.github.com/repos/David7ce/pkg-lab/releases/tags/<android-Recetario-latest|desktop-recetario-latest>`
  (`logic/UpdateCheck.kt`), compares the version baked into the release's asset filenames against its
  own, and shows a dismissible "version N is available" banner with a link to the release — check
  and notify only, no auto-download or silent install. **No GitHub secret involved on the client
  side**, deliberately: these are public releases on a public repo, so an unauthenticated GET is
  enough. Baking any token into a shipped app would let anyone holding the binary extract and
  reuse it — secrets stay server-side, where `ANDROID_KEYSTORE_PASSWORD` already lives. This
  depends on the CI run-number versioning scheme (#4) staying monotonic per workflow; if that's
  ever replaced with hand-set semantic versions, `isNewer()` in `UpdateCheck.kt` needs to compare
  on the full version, not just the trailing number.
- **#1 — Shared version catalog.** One `android/gradle/libs.versions.toml` now, referenced by all
  three apps' `settings.gradle.kts` via `dependencyResolutionManagement.versionCatalogs`, and by
  `android/shared/build.gradle.kts` (Compose deps are now `libs.compose.*` instead of hardcoded
  strings). Verified by building and testing all three apps plus the desktop target individually.
- **#3 — Desktop DB migrations.** `DesktopMain.kt`'s `createAppState()` now uses
  `JdbcSqliteDriver`'s schema-aware constructor (`JdbcSqliteDriver(url, schema = Schema)`), which
  creates a fresh database at the latest version or migrates an existing one through
  `RecipeDatabase.Schema.migrate` — the same `.sqm` files Android already uses. No more manual
  `isNew` check.
- **#4 — Meaningful `versionName` + release notes, with a caveat.** Each Android app's
  `versionName` is now a hand-set string (starting at `"1.0"`); `versionCode` is untouched (still
  CI-run-number-based — that's what makes every build installable, not `versionName`). **Desktop's
  `packageVersion` was deliberately left as the CI-run-number scheme**: on Windows/macOS, that
  version number is also what the installer uses to detect an upgrade, so freezing it between hand
  bumps would make consecutive CI builds collide instead of cleanly upgrading. `UpdateCheck.kt`'s
  `isNewer()` was rewritten to compare version tuples segment-by-segment (so "2.0" correctly beats
  "1.9" — a trailing-segment-only compare would have gotten that backwards) and its regex now
  accepts both 2-part ("1.0") and 3-part ("1.0.42") versions. `android.yml`/`desktop.yml` now add a
  commit-log section to each release's notes (commits since that app's last release, scoped to its
  own paths) — this part is reasoned through carefully and YAML-validated, but **not live-tested**
  against the real `gh` CLI/API, since that needs an actual push to `main`. Worth watching the next
  real release for this specifically.
- **#2 — Smoke tests, Robolectric on the JVM.** One Compose smoke test per app (`MainActivitySmokeTest`
  for FuelLog/VozReves, `MainScreenSmokeTest` for Recetario), running in the existing
  `testDebugUnitTest` step — no CI changes, no emulator. Recetario's needed the real
  `AndroidSqliteDriver` instead of the JVM JDBC one the other repository tests use: `DriverManager`'s
  driver auto-registration doesn't cross Robolectric's sandboxed classloader. All three verified
  green locally; Compose-on-Robolectric is still less battle-tested than a real device, so a
  real-device sanity check is worth doing before leaning on these heavily.

## Improvements worth doing

Ranked by how concrete and repo-wide the payoff is — not a generic best-practices checklist.
(#1–#4 from the original list are done — see "Shipped since this was written" above.)

### 5. Signing secret has a single point of failure
The README itself flags this ("sin la contraseña... no podrán actualizarse"): one
`ANDROID_KEYSTORE_PASSWORD` secret, stored once in a password manager per the README's own
instruction. That instruction is the actual mitigation — nothing to build here, just confirm it's
actually been done, since every installed APK depends on it.

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

## Roadmap

Grouped by effort, not calendar dates (solo maintainer, CI-only — "when there's time" is the real
schedule).

**Next (cheap):**
- Confirm `ANDROID_KEYSTORE_PASSWORD` is actually saved in a password manager (#5) — the one open
  item from the original list; everything else on it (#1–#4) is done.
- Watch the first real release after the git-log release-notes change (#4) — reasoned through and
  YAML-validated, not live-tested against the real `gh` CLI.

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
