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

## Improvements worth doing

Ranked by how concrete and repo-wide the payoff is — not a generic best-practices checklist.

### 1. Version numbers live in 4 places by hand
`android/Recetario` uses a Gradle version catalog (`gradle/libs.versions.toml`). FuelLog and
VozReves hardcode the same Kotlin/AGP versions directly in their root `build.gradle.kts`, and
`android/shared/build.gradle.kts` hardcodes its own Compose/Material3 versions again. The README
already states the rule ("las apps deben usar la misma versión para poder compartir el módulo")
but nothing enforces it — a future Compose bump means editing 4 files and hoping none is missed.
- **Fix:** one `gradle/libs.versions.toml` at the repo root (or in `android/shared`, included via
  `dependencyResolutionManagement` from each app's `settings.gradle.kts`), referenced by all three
  apps and `shared`. Mechanical change, no behavior change — low risk, worth doing before the next
  version bump rather than during one.

### 2. No instrumented/UI tests anywhere
All three apps test `logic/`/`data/` on the JVM only (confirmed: no `androidTest` directory in the
repo). That's the right call for pure logic, but screen-level regressions (a dialog that doesn't
open, a button that's unreachable after a refactor) aren't caught by CI and rely on the manual
"test on a real phone before calling it good" note in VozReves's README.
- **Fix, scoped:** not a full Compose UI test suite (that's real ongoing cost for a solo-maintainer
  lab). A handful of `createAndroidComposeRule` smoke tests per app — app launches, main screen
  renders, one add/edit/delete round-trip — would catch the regressions that actually happen
  (crash on launch, broken navigation) without chasing full coverage.

### 3. Desktop (Recetario) has no DB migration path
Per its own README: "Migraciones de esquema de la base de datos de escritorio (hoy solo se crea la
base nueva)." Android has versioned `.sqm` migrations (v1→v2→v3); desktop does not. Harmless today
(schema v3, no desktop users yet with old data), but it's the kind of gap that's cheap to close
*before* the first desktop schema change ships, and expensive to retrofit after users have data on
disk.
- **Fix:** point `desktopMain`'s SQLDelight driver at the same `.sqm` migration files Android
  already has (they're schema-only, not Android-specific) instead of "create new" — likely a small
  diff since the migrations already exist.

### 4. `versionName` isn't meaningful
Every app sets `versionName = "1.0.$build"` where `$build` is the CI run number. It satisfies
Android's "must increase to update" requirement, but it means nothing to a user reading
Settings/About, and GitHub Releases notes just say "compilada desde \<sha\>" — there's no changelog
connecting a version number to what changed.
- **Fix, cheap:** keep the CI-run-number scheme for `versionCode` (it works, don't touch it), but
  let each app set a real `versionName` (e.g. `"1.2"`) bumped by hand on meaningful changes, and
  add one line to the release notes step in `android.yml`/`desktop.yml` that surfaces the commits
  since the last release tag (`git log <last-tag>..HEAD --oneline`). Skip a full CHANGELOG.md —
  the git log already is a changelog for a repo this size; just surface it at release time.

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
schedule). Items reference the numbered improvements above.

**Next (cheap, do before the next version-sensitive change):**
- Shared version catalog (#1) — do this *before* the next Compose/AGP bump, not during.
- Desktop DB migrations (#3) — do this *before* Recetario's next schema change (v4), while it's
  still free of real desktop user data.

**Soon (adds real safety, moderate effort):**
- A few Compose smoke tests per app (#2).
- Meaningful `versionName` + commit-log release notes (#4).

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
