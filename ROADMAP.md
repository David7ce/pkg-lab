# pkg-lab — roadmap

Per-app pending work lives in each app's own README, not here (Recetario: "Desktop pending work",
"Known limitations"; FuelLog: its own notes).

## Open items

- **Watch the first real release under the merged-release change.** `android.yml` and `desktop.yml`
  now both publish Recetario to the same `recetario-latest` release (Android + desktop together,
  see Reference below) instead of two separate ones, without ever deleting the other's assets.
  Reasoned through and syntax-checked (YAML + `bash -n`), the exact `gh` subcommands confirmed
  against `--help`, but not watched end-to-end against a real concurrent run of both workflows yet.
- **Compose-on-Robolectric smoke tests** pass locally; worth a real-device check too before
  relying on them as the only UI-level test.

## Not worth doing

Splitting `:pkgshared` into modules, or a monorepo build tool (Nx/Bazel) — three Gradle projects
and path-filtered CI already handle this fine. Revisit only if a 4th app needs it.

## Reference

- **Releases** are rolling `latest` tags per app (`android-<App>-latest`; Recetario's is
  `recetario-latest`, shared by Android and desktop), not a version history — intentional, not a
  gap. Titles are `<App> <version>`, no platform suffix — the asset filenames already say that.
- **Signing**: one key (alias `pkglab`), encrypted at `.github/signing/release.jks.enc`, password
  in the `ANDROID_KEYSTORE_PASSWORD` GitHub secret *and* a password manager (reset 2026-10-13 —
  the original was only ever in the secret, unrecoverable once set). Losing it again means every
  installed app needs a reinstall on the next release.
