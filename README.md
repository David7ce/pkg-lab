# pkg-lab

A lab for building and testing apps in the cloud with GitHub Actions, no PC required.
Organized by OS so it can grow to other platforms.

```
pkg-lab/
├── .github/workflows/
│   └── android.yml        ← builds everything under android/
├── android/
│   ├── shared/            ← common module (Kotlin Multiplatform, Compose): shared UI components
│   ├── VozReves/          ← each app is its own independent Gradle project
│   ├── FuelLog/           ← fuel refill and consumption tracker
│   └── Recetario/         ← personal recipe book (Kotlin Multiplatform: ready for Windows/Linux)
├── linux/                 ← (future) with its own linux.yml
├── windows/               ← (future)
└── web/                   ← (future)
```

## Android

- **Adding an app:** create `android/<AppName>/` with a Gradle project (with `gradlew`). No need to touch the workflow.
- **Tests:** if the app has unit tests (`app/src/test`, or its modules' own), they run on every build
  and the count shows up as a notice on the run.
- **Errors:** if a build or a test fails, Kotlin/Gradle errors show up as annotations on the
  run page, no need to open the full log.
- **Multiplatform apps:** a Kotlin Multiplatform app lives the same way under `android/<App>/`, with its common code in a
  `shared` module and `app` only for packaging (see Recetario). CI doesn't change.
- **Shared module (`android/shared`, `:pkgshared`):** selectors, confirmation dialogs, multi-select,
  sub-screens and common text. Every app includes it and shares the same UX behavior; only
  colors, icon and app-specific features differ. Changes in that folder rebuild every app. Details and how to
  include it in `android/shared/README.md`.
- **Common versions:** Kotlin 2.4.21, Compose Multiplatform 1.11.1 (Material 3 1.9.0), AGP 8.13.2, Gradle 8.14.3,
  compileSdk 36, minSdk 26. Apps must use the same version to be able to share the module.
- **Building:** every push to `main` builds only the apps that changed. Also by hand from
  *Actions → Android → Run workflow* (type the folder name, or leave it empty for all of them).
- **Downloading:** *Releases → `android-<AppName>-latest`*. Each app builds in parallel for
  `arm64-v8a` (almost every phone), `armeabi-v7a` (old 32-bit) and `universal`.
  For an app to produce per-architecture APKs, its `app/build.gradle.kts` must read `-Pabi`
  (see VozReves); otherwise all three come out identical.
- **APK naming:** `<App>-android-<version>-<architecture>.apk` (the version is read from the APK itself).

### Signing (recommended)

Without a keystore, every build is signed with a different debug key and Android won't let you update
over it: you'd have to uninstall first. That's why every app is signed with a shared key:

- `.github/signing/release.jks.enc`: keystore encrypted with AES-256 (alias `pkglab`). It's safe
  to keep in a public repo; without the password it's useless.
- Secret `ANDROID_KEYSTORE_PASSWORD` (*Settings → Secrets and variables → Actions*): the password.
  **Also save it in a password manager**: if it's lost, installed apps won't be able to
  update with a new key.

To decrypt it locally: `openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 -in release.jks.enc -out release.jks`.
Apps read the signing config from `SIGNING_*` env vars in their `app/build.gradle.kts` (see VozReves as an example).

## Other platforms

For a new platform: a `<os>/` folder + a `<os>.yml` workflow with `paths: ["<os>/**"]`,
following the same pattern (detect changes → build in a matrix → release `<os>-<App>-latest`).
