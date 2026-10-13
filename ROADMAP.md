# pkg-lab — roadmap

Per-app pending work lives in each app's own README, not here (Recetario: "Desktop pending work",
"Known limitations"; FuelLog: its own notes).

## Open items

- **Watch the first real release.** `android.yml`/`desktop.yml` add a commit-log section to each
  release's notes now — reasoned through and YAML-valid, not live-tested against the real `gh`
  CLI/API yet.
- **Compose-on-Robolectric smoke tests** pass locally; worth a real-device check too before
  relying on them as the only UI-level test.

## Not worth doing

Splitting `:pkgshared` into modules, or a monorepo build tool (Nx/Bazel) — three Gradle projects
and path-filtered CI already handle this fine. Revisit only if a 4th app needs it.

## Reference

- **Releases** are rolling `latest` tags per app (`android-<App>-latest`,
  `desktop-recetario-latest`), not a version history — intentional, not a gap.
- **Signing**: one key (alias `pkglab`), encrypted at `.github/signing/release.jks.enc`, password
  in the `ANDROID_KEYSTORE_PASSWORD` GitHub secret *and* a password manager (reset 2026-10-13 —
  the original was only ever in the secret, unrecoverable once set). Losing it again means every
  installed app needs a reinstall on the next release.
