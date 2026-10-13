# SleepCycles Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship SleepCycles, a native port of the `sleep-cycles-calc` PWA, as an Android app plus Windows/Linux/macOS desktop installers inside pkg-lab, and put the whole repo under PolyForm Strict 1.0.0.

**Architecture:** `android/SleepCycles/` mirrors Recetario: `app/` is Android packaging only, `shared/` is a Kotlin Multiplatform library (`androidTarget` + `jvm("desktop")`) holding pure logic, platform interfaces and the Compose UI. It includes `:pkgshared` for `SegmentedChoice`, `ChoiceDropdown`, `SubScreen`, `SectionTitle`, `LocalSharedStrings`. State is a Compose `AppState`; persistence is a tiny key/value `SettingsStore` per platform.

**Tech Stack:** Kotlin 2.4.21, Compose Multiplatform 1.11.1, Material 3 1.9.0, AGP 8.13.2, Gradle 8.14.3, compileSdk 36, minSdk 26 (all from `android/gradle/libs.versions.toml`). No SQLDelight, serialization, or network.

**Spec:** [docs/superpowers/specs/2026-10-13-sleepcycles-design.md](../specs/2026-10-13-sleepcycles-design.md)

## Global Constraints

- Application id and namespace `com.david7ce.sleepcycles` (shared library: `com.david7ce.sleepcycles.shared`); source folders `com/david7ce/...`. Never `deivon`.
- App name `SleepCycles`; release tag `sleepcycles-latest` (Android and desktop share it); release title `SleepCycles <version>`, no platform suffix.
- Languages: es, en, fr, de, pt, it. Device language by default, or chosen in Settings. Missing language falls back to English.
- Versions come only from `android/gradle/libs.versions.toml`; do not add the androidx Compose BOM.
- `commonMain` has no Android or JVM-only imports. JVM-only code lives in `jvmCommonMain`, Android in `androidMain`, desktop in `desktopMain`.
- Texts never live in `:pkgshared`; the app fills `LocalSharedStrings` at its root.
- 24 dp side gutters, 48 dp touch targets, times always laid out left to right.
- Time is minutes of the day (0..1439); no date library.
- License: PolyForm Strict 1.0.0 for all of pkg-lab (root `LICENSE`).
- Commits end with `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- There is **no Android SDK on the dev machine**: nothing can be compiled or tested locally. Code is written only with APIs already used elsewhere in this repo, and verified by CI (Task 14). Say so in every status report; never claim "tests pass" before a CI run says so.

## Review Focus

Behaviors the spec implies but a happy-path test would miss; each has a test in the task named.

1. **Latency field garbage** (empty, `-5`, `abc`, `99999999999`, `500`): empty/garbage/negative → 0, above the PWA's input max → 120. No overflow, no crash. (Task 4)
2. **Midnight wrap**: anchor `00:00` and `23:59` in both modes produce valid `00:00..23:59` times, never negative or ≥ 24:00. (Task 3)
3. **12-hour edge cases**: `12:xx AM` is `00:xx`, `12:xx PM` is `12:xx`; typed hour `0` or `13` in 12-hour mode, or `24` in 24-hour mode, is rejected and keeps the last valid time. (Task 4)
4. **Corrupt or missing stored settings** (unknown enum name, non-numeric time, time ≥ 1440, latency `999`, key missing after an update): fall back per field to defaults, no crash. (Task 5)
5. **Unsupported device language** (`ja`, `ar`, `pt-BR`, empty) resolves to English or the right base language, and time stays left-to-right under an RTL layout direction. (Tasks 7 and 9)

## File Structure

```
.github/workflows/android.yml            modify: multi-platform tag + notes
.github/workflows/desktop.yml            rewrite: detect job + per-app matrix
LICENSE                                  create: PolyForm Strict 1.0.0
README.md, ROADMAP.md                    modify
scripts/build-and-test.ps1               modify: -Desktop/-Package for any desktop-capable app
android/SleepCycles/
  settings.gradle.kts, build.gradle.kts, gradle.properties, gradlew, gradlew.bat, gradle/wrapper/*
  README.md
  app/build.gradle.kts
  app/src/main/AndroidManifest.xml
  app/src/main/res/{values,values-es,values-fr,values-de,values-pt,values-it,values-night,xml,drawable,mipmap-anydpi-v26}/…
  shared/build.gradle.kts
  shared/src/commonMain/kotlin/com/david7ce/sleepcycles/
    logic/SleepCalc.kt          Mode, AgeRange, Rating, Suggestion, suggestions(), rate(), clampLatency(), wrapDay()
    logic/TimeFormat.kt         ClockFormat, TimeParts, timeParts(), parseTime(), formatTime(), formatHours(), parseLatency()
    logic/UserSettings.kt       ThemeMode, AppLanguage, UserSettings, SettingsStore.load()/save()
    platform/Services.kt        SettingsStore, Clock, Services
    ui/AppState.kt              Screen, AppState
    ui/Strings.kt               Strings (English base), LocalStrings
    ui/Translations.kt          Es/Fr/De/Pt/It strings, stringsFor()
    ui/Theme.kt                 SleepCyclesTheme, isDark()
    ui/AppIcons.kt              Settings, Info, Close icons
    ui/NightSky.kt              night background (gradient, stars, mountains)
    ui/RatingFace.kt            Canvas faces
    ui/TimeField.kt             hh:mm typed input (+ AM/PM)
    ui/MainScreen.kt            main screen
    ui/SettingsScreen.kt        settings + latency field
    ui/HelpDialog.kt            "How does it work?"
    SleepCyclesApp.kt           root composable
  shared/src/commonTest/kotlin/com/david7ce/sleepcycles/…  SleepCalcTest, TimeFormatTest, UserSettingsTest, AppStateTest, StringsTest, TestSupport
  shared/src/jvmCommonMain/kotlin/com/david7ce/sleepcycles/platform/LocalClock.kt
  shared/src/androidMain/kotlin/com/david7ce/sleepcycles/platform/{SleepCyclesApplication,MainActivity,PreferencesSettingsStore}.kt
  shared/src/desktopMain/kotlin/com/david7ce/sleepcycles/platform/{DesktopMain,JavaPreferencesStore,WindowsTitleBar}.kt
```

Branch: all work happens on `feat/sleepcycles` (Task 0). Commit after every task. Do not push without the owner's say-so.

---

### Task 0: Branch

**Files:** none.

- [ ] **Step 1: Create the branch**

```bash
cd /c/Users/tener/Workspaces/repos/pkg-lab
git switch -c feat/sleepcycles
git status --short
```
Expected: `Switched to a new branch 'feat/sleepcycles'`, clean tree.

---

### Task 1: License (PolyForm Strict 1.0.0)

**Files:**
- Create: `LICENSE`
- Modify: `README.md` (add a License section at the end)

**Interfaces:** none.

- [ ] **Step 1: Download the official text**

```bash
cd /c/Users/tener/Workspaces/repos/pkg-lab
curl -fsSL https://raw.githubusercontent.com/polyformproject/polyform-licenses/1.0.0/PolyForm-Strict-1.0.0.md -o "$TMPDIR/strict.md" || curl -fsSL https://polyformproject.org/licenses/strict/1.0.0/ -o "$TMPDIR/strict.md"
head -5 "$TMPDIR/strict.md"
```
Expected: the first lines include `PolyForm Strict License 1.0.0`. If neither URL works, stop and ask the owner for the file; **never write the license text from memory.**

- [ ] **Step 2: Create `LICENSE`**: a required-notice line, a blank line, then the downloaded text unchanged

```bash
{ printf 'Required Notice: Copyright (c) 2026 David7ce (https://github.com/David7ce)\n\n'; cat "$TMPDIR/strict.md"; } > LICENSE
head -8 LICENSE
```

- [ ] **Step 3: Add the README section**

Append to `README.md`:

```markdown
## License

Source-available under the [PolyForm Strict License 1.0.0](LICENSE): you may read the code and use the
apps for noncommercial purposes, but not modify or redistribute them. Third-party dependencies keep
their own licenses. The original web version, `sleep-cycles-calc`, was published under Apache-2.0 and
those published versions keep that license.
```

- [ ] **Step 4: Commit**

```bash
git add LICENSE README.md
git commit -m "Add PolyForm Strict 1.0.0 license for the whole repo

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Gradle project, Android packaging and a stub that builds

**Files:**
- Create: everything under `android/SleepCycles/` except `shared/src/commonMain|commonTest` (stub only here)

**Interfaces:**
- Produces: Gradle projects `:app`, `:shared`, `:pkgshared`; source set `jvmCommonMain` (shared by Android and desktop); desktop `mainClass` `com.david7ce.sleepcycles.platform.DesktopMainKt` (created in Task 11); constant `DESKTOP_VERSION` (generated).

- [ ] **Step 1: Copy the wrapper and static files from Recetario**

```bash
cd /c/Users/tener/Workspaces/repos/pkg-lab/android
mkdir -p SleepCycles/gradle SleepCycles/app/src/main/res SleepCycles/shared/src
cp -r Recetario/gradle/wrapper SleepCycles/gradle/wrapper
cp Recetario/gradlew Recetario/gradlew.bat Recetario/gradle.properties SleepCycles/
git update-index --add --chmod=+x SleepCycles/gradlew 2>/dev/null || true
```

- [ ] **Step 2: `android/SleepCycles/settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    // Shared by every app under android/ and android/shared (see android/gradle/libs.versions.toml).
    versionCatalogs {
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}

rootProject.name = "SleepCycles"
include(":app", ":shared", ":pkgshared")
// Components shared by every app (android/shared).
project(":pkgshared").projectDir = file("../shared")
```

- [ ] **Step 3: `android/SleepCycles/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
}
```

- [ ] **Step 4: `android/SleepCycles/app/build.gradle.kts`**

```kotlin
// Packaging only: application id, version, signing, ABI splits, launcher icon.
// All code lives in :shared (commonMain + androidMain).
plugins {
    alias(libs.plugins.android.application)
}

// -Pabi=arm64-v8a | armeabi-v7a | universal (the CI workflow builds each one in parallel).
val targetAbi: String? = (findProperty("abi") as String?)?.takeIf { it != "universal" }
val abiCodes = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2)

android {
    namespace = "com.david7ce.sleepcycles"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.david7ce.sleepcycles"
        minSdk = 26
        targetSdk = 36
        // CI run number ×10 + ABI code: every build installs over the previous one.
        val build = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = build * 10 + (abiCodes[targetAbi] ?: 0)
        // Hand-bumped on meaningful changes; versionCode (above) is what makes every build installable.
        versionName = "1.0"
    }

    androidResources {
        localeFilters += listOf("en", "es", "fr", "de", "pt", "it")
    }

    splits {
        abi {
            isEnable = targetAbi != null
            if (targetAbi != null) { reset(); include(targetAbi) }
            isUniversalApk = false
        }
    }

    // Shared pkg-lab signing key, provided by the CI workflow; debug key otherwise.
    val keystorePath = System.getenv("SIGNING_KEYSTORE")
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += setOf("META-INF/*.version", "DebugProbesKt.bin", "kotlin-tooling-metadata.json")
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(project(":shared"))
}
```

- [ ] **Step 5: `android/SleepCycles/shared/build.gradle.kts`**

```kotlin
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Shared code: logic, state and the Compose UI live in commonMain.
// Android-only pieces (SharedPreferences, Activity) live in androidMain; desktop-only ones
// (java.util.prefs, window) in desktopMain; JVM code both use (clock) in jvmCommonMain.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

// Single source of truth for the desktop installer version, also baked into the running app
// below (generateDesktopVersion) so Settings can show it.
val appVersion = "1.0." + (System.getenv("GITHUB_RUN_NUMBER") ?: "1")

// Desktop has no BuildConfig/PackageInfo equivalent; generate a one-constant Kotlin file instead.
val generateDesktopVersion = tasks.register("generateDesktopVersion") {
    val outputDir = layout.buildDirectory.dir("generated/desktopVersion/kotlin")
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("com/david7ce/sleepcycles/platform/DesktopVersion.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            "package com.david7ce.sleepcycles.platform\n\ninternal const val DESKTOP_VERSION = \"$appVersion\"\n"
        )
    }
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    jvm("desktop") {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    sourceSets {
        val jvmCommonMain by creating { dependsOn(commonMain.get()) }
        androidMain.get().dependsOn(jvmCommonMain)
        val desktopMain by getting {
            dependsOn(jvmCommonMain)
            kotlin.srcDir(generateDesktopVersion)
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.jna.platform) // Windows title bar dark/light (DwmSetWindowAttribute)
            }
        }

        commonMain.dependencies {
            implementation(project(":pkgshared"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}

android {
    namespace = "com.david7ce.sleepcycles.shared"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Desktop app: packageMsi (Windows), packageDmg (macOS), createDistributable (Linux). See .github/workflows/desktop.yml.
compose.desktop {
    application {
        mainClass = "com.david7ce.sleepcycles.platform.DesktopMainKt"
        nativeDistributions {
            // Only what gets published: .msi (Windows) and .dmg (macOS). Linux uses createDistributable + tar.gz in CI.
            targetFormats(TargetFormat.Msi, TargetFormat.Dmg)
            packageName = "SleepCycles"
            // Installers need MAJOR.MINOR.PATCH with a number in MAJOR; CI run number keeps each build newer.
            packageVersion = appVersion
            vendor = "david7ce"
            description = "SleepCycles: sleep cycle calculator"
            windows {
                menuGroup = "SleepCycles"
                upgradeUuid = "b3f1d7a2-8c54-4e0b-a6d9-5e2c7f9a1b34"
            }
        }
    }
}
```

- [ ] **Step 6: Android resources**

`app/src/main/res/values/strings.xml`
```xml
<resources>
    <string name="app_name">SleepCycles</string>
    <string name="shortcut_sleep_now">Sleep now</string>
</resources>
```
`values-es/strings.xml` → `shortcut_sleep_now` = `Dormir ahora`; `values-fr` → `Dormir maintenant`; `values-de` → `Jetzt schlafen`; `values-pt` → `Dormir agora`; `values-it` → `Dormi ora`. Each file:
```xml
<resources>
    <string name="shortcut_sleep_now">Dormir ahora</string>
</resources>
```
(`app_name` stays only in the base file: product names are not translated.)

`values/themes.xml`
```xml
<resources>
    <!-- Window theme only; colours come from Compose (SleepCyclesTheme). -->
    <style name="Theme.SleepCycles" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```
`values-night/themes.xml`
```xml
<resources>
    <style name="Theme.SleepCycles" parent="android:Theme.Material.NoActionBar" />
</resources>
```
`xml/locales_config.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
    <locale android:name="en" />
    <locale android:name="es" />
    <locale android:name="fr" />
    <locale android:name="de" />
    <locale android:name="pt" />
    <locale android:name="it" />
</locale-config>
```
`xml/shortcuts.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<shortcuts xmlns:android="http://schemas.android.com/apk/res/android">
    <shortcut
        android:shortcutId="sleep_now"
        android:enabled="true"
        android:icon="@mipmap/ic_launcher"
        android:shortcutShortLabel="@string/shortcut_sleep_now"
        android:shortcutLongLabel="@string/shortcut_sleep_now">
        <intent
            android:action="com.david7ce.sleepcycles.SLEEP_NOW"
            android:targetPackage="com.david7ce.sleepcycles"
            android:targetClass="com.david7ce.sleepcycles.platform.MainActivity" />
    </shortcut>
</shortcuts>
```
`mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` (identical):
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```
`drawable/ic_launcher_background.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#1A114F" android:pathData="M0,0h108v108h-108z" />
</vector>
```
`drawable/ic_launcher_foreground.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Crescent moon (Material "dark_mode"), centred in the 66dp safe zone. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <group android:scaleX="2.2" android:scaleY="2.2" android:translateX="27.6" android:translateY="27.6">
        <path android:fillColor="#ECE8FB"
            android:pathData="M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9s9,-4.03 9,-9c0,-0.46 -0.04,-0.92 -0.1,-1.36c-0.98,1.37 -2.58,2.26 -4.4,2.26c-2.98,0 -5.4,-2.42 -5.4,-5.4c0,-1.81 0.89,-3.42 2.26,-4.4C12.92,3.04 12.46,3 12,3L12,3z" />
    </group>
</vector>
```

- [ ] **Step 7: `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<!--
  No permissions: settings stay in the app's private SharedPreferences, no network.
  The "Sleep now" launcher shortcut starts MainActivity with action SLEEP_NOW.
-->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:name="com.david7ce.sleepcycles.platform.SleepCyclesApplication"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:label="@string/app_name"
        android:localeConfig="@xml/locales_config"
        android:supportsRtl="true"
        android:enableOnBackInvokedCallback="true"
        android:theme="@style/Theme.SleepCycles">

        <activity
            android:name="com.david7ce.sleepcycles.platform.MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <meta-data android:name="android.app.shortcuts" android:resource="@xml/shortcuts" />
        </activity>
    </application>
</manifest>
```

- [ ] **Step 8: Stub sources so CI can already build (replaced in later tasks)**

`shared/src/commonMain/kotlin/com/david7ce/sleepcycles/SleepCyclesApp.kt`
```kotlin
package com.david7ce.sleepcycles

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** Replaced in Task 9. */
@Composable
fun SleepCyclesApp() {
    Text("SleepCycles")
}
```
`shared/src/jvmCommonMain/kotlin/com/david7ce/sleepcycles/platform/Stub.kt`: an empty file with only `package com.david7ce.sleepcycles.platform` so the source set exists (deleted in Task 10).

`shared/src/androidMain/kotlin/com/david7ce/sleepcycles/platform/SleepCyclesApplication.kt`
```kotlin
package com.david7ce.sleepcycles.platform

import android.app.Application

/** Replaced in Task 10. */
class SleepCyclesApplication : Application()
```
`shared/src/androidMain/kotlin/com/david7ce/sleepcycles/platform/MainActivity.kt`
```kotlin
package com.david7ce.sleepcycles.platform

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.david7ce.sleepcycles.SleepCyclesApp

/** Replaced in Task 10. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SleepCyclesApp() }
    }
}
```
`shared/src/desktopMain/kotlin/com/david7ce/sleepcycles/platform/DesktopMain.kt`
```kotlin
package com.david7ce.sleepcycles.platform

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.david7ce.sleepcycles.SleepCyclesApp

/** Replaced in Task 11. */
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "SleepCycles") { SleepCyclesApp() }
}
```
`shared/src/commonTest/kotlin/com/david7ce/sleepcycles/SmokeTest.kt`
```kotlin
package com.david7ce.sleepcycles

import kotlin.test.Test
import kotlin.test.assertEquals

class SmokeTest {
    @Test
    fun testsRun() = assertEquals(4, 2 + 2)
}
```

- [ ] **Step 9: Build locally only if an SDK happens to exist, otherwise defer to CI**

```powershell
.\scripts\build-and-test.ps1 -App SleepCycles
```
Expected with SDK: BUILD SUCCESSFUL. Without SDK (this machine): fails with "SDK location not found"; that is expected, move on, CI is Task 14.

- [ ] **Step 10: Commit**

```bash
cd /c/Users/tener/Workspaces/repos/pkg-lab
git add android/SleepCycles
git commit -m "Scaffold SleepCycles: KMP shared module, Android packaging, desktop target

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Calculation and rating (pure logic)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/david7ce/sleepcycles/logic/SleepCalc.kt`
- Test: `shared/src/commonTest/kotlin/com/david7ce/sleepcycles/logic/SleepCalcTest.kt`

**Interfaces:**
- Produces:
  - `const val CYCLE_MINUTES = 90`, `MINUTES_PER_DAY = 1440`, `MAX_LATENCY_MINUTES = 120`, `DEFAULT_LATENCY_MINUTES = 15`
  - `val CYCLE_COUNTS: List<Int>` = `[6,5,4,3,2,1]`
  - `enum class Mode { Wake, Bed }`
  - `enum class AgeRange(val minMinutes: Int, val maxMinutes: Int) { Child, Teen, Adult, Senior }`
  - `enum class Rating { Good, Ok, Low }`
  - `data class Suggestion(val cycles: Int, val timeOfDay: Int, val sleepMinutes: Int, val rating: Rating)`
  - `fun wrapDay(minutes: Int): Int`, `fun clampLatency(minutes: Int): Int`
  - `fun rate(sleepMinutes: Int, age: AgeRange): Rating`
  - `fun suggestions(mode: Mode, anchor: Int, latencyMinutes: Int, age: AgeRange): List<Suggestion>`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.david7ce.sleepcycles.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SleepCalcTest {
    private fun hm(h: Int, m: Int) = h * 60 + m

    @Test
    fun wakeModeCountsBackFromTheWakeTimeIncludingLatency() {
        val s = suggestions(Mode.Wake, hm(7, 0), 15, AgeRange.Adult)
        assertEquals(listOf(6, 5, 4, 3, 2, 1), s.map { it.cycles })
        assertEquals(
            listOf(hm(21, 45), hm(23, 15), hm(0, 45), hm(2, 15), hm(3, 45), hm(5, 15)),
            s.map { it.timeOfDay },
        )
        assertEquals(listOf(540, 450, 360, 270, 180, 90), s.map { it.sleepMinutes })
    }

    @Test
    fun bedModeCountsForwardFromTheBedTimeIncludingLatency() {
        val s = suggestions(Mode.Bed, hm(23, 0), 15, AgeRange.Adult)
        assertEquals(
            listOf(hm(8, 15), hm(6, 45), hm(5, 15), hm(3, 45), hm(2, 15), hm(0, 45)),
            s.map { it.timeOfDay },
        )
    }

    @Test
    fun resultsAlwaysStayInsideTheDayAroundMidnight() {
        for (mode in Mode.entries) for (anchor in listOf(0, 1, hm(23, 59), 1439)) for (latency in listOf(0, 15, 120)) {
            suggestions(mode, anchor, latency, AgeRange.Adult).forEach {
                assertTrue(it.timeOfDay in 0 until MINUTES_PER_DAY, "$mode $anchor $latency -> ${it.timeOfDay}")
            }
        }
        // 00:00 wake, no latency, 1 cycle -> 22:30 the previous evening.
        assertEquals(hm(22, 30), suggestions(Mode.Wake, 0, 0, AgeRange.Adult).last().timeOfDay)
        // 23:59 bed, no latency, 1 cycle -> 01:29.
        assertEquals(hm(1, 29), suggestions(Mode.Bed, hm(23, 59), 0, AgeRange.Adult).last().timeOfDay)
    }

    @Test
    fun latencyIsClampedToTheSupportedRange() {
        val max = suggestions(Mode.Bed, 0, MAX_LATENCY_MINUTES, AgeRange.Adult)
        assertEquals(max, suggestions(Mode.Bed, 0, 500, AgeRange.Adult))
        assertEquals(suggestions(Mode.Bed, 0, 0, AgeRange.Adult), suggestions(Mode.Bed, 0, -30, AgeRange.Adult))
        assertEquals(0, clampLatency(-1))
        assertEquals(120, clampLatency(121))
    }

    @Test
    fun ratingsPerAgeRange() {
        fun ratings(age: AgeRange) = suggestions(Mode.Wake, 0, 0, age).map { it.rating }
        val g = Rating.Good
        val o = Rating.Ok
        val l = Rating.Low
        assertEquals(listOf(g, g, o, o, l, l), ratings(AgeRange.Adult))
        assertEquals(listOf(g, o, l, o, l, l), ratings(AgeRange.Child))
        assertEquals(listOf(g, o, l, o, l, l), ratings(AgeRange.Teen))
        assertEquals(listOf(o, g, o, o, l, l), ratings(AgeRange.Senior))
    }

    @Test
    fun ratingBoundaries() {
        assertEquals(Rating.Low, rate(329, AgeRange.Adult))
        assertEquals(Rating.Ok, rate(330, AgeRange.Adult))
        assertEquals(Rating.Ok, rate(419, AgeRange.Adult))
        assertEquals(Rating.Good, rate(420, AgeRange.Adult))
        assertEquals(Rating.Good, rate(540, AgeRange.Adult))
        assertEquals(Rating.Ok, rate(541, AgeRange.Adult))
        assertEquals(Rating.Ok, rate(630, AgeRange.Adult))
        assertEquals(Rating.Low, rate(631, AgeRange.Adult))
        // 4.5 h is always at least "ok": the PWA's cycle-aligned fallback.
        assertEquals(Rating.Ok, rate(270, AgeRange.Adult))
        assertEquals(Rating.Low, rate(271, AgeRange.Adult))
    }
}
```

- [ ] **Step 2: Run to verify it fails** (CI, or locally with an SDK)

Run: `./gradlew :shared:desktopTest --tests "*SleepCalcTest*"` (from `android/SleepCycles`)
Expected: FAIL, unresolved reference `suggestions`.

- [ ] **Step 3: Implement**

```kotlin
package com.david7ce.sleepcycles.logic

const val CYCLE_MINUTES = 90
const val MINUTES_PER_DAY = 24 * 60
/** The PWA's "time to fall asleep" input is 0..120. */
const val MAX_LATENCY_MINUTES = 120
const val DEFAULT_LATENCY_MINUTES = 15

/** Cycles offered, from the most sleep to the least. */
val CYCLE_COUNTS = listOf(6, 5, 4, 3, 2, 1)

/** [Wake]: the user fixes the wake-up time and gets bedtimes. [Bed]: the reverse. */
enum class Mode { Wake, Bed }

/** Recommended sleep per age range, in minutes. The labels (with the years) live in Strings. */
enum class AgeRange(val minMinutes: Int, val maxMinutes: Int) {
    Child(9 * 60, 11 * 60),
    Teen(8 * 60, 10 * 60),
    Adult(7 * 60, 9 * 60),
    Senior(7 * 60, 8 * 60),
}

enum class Rating { Good, Ok, Low }

/** [timeOfDay] is minutes since midnight, the bedtime (Wake mode) or the wake-up time (Bed mode). */
data class Suggestion(val cycles: Int, val timeOfDay: Int, val sleepMinutes: Int, val rating: Rating)

fun wrapDay(minutes: Int): Int = ((minutes % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY

fun clampLatency(minutes: Int): Int = minutes.coerceIn(0, MAX_LATENCY_MINUTES)

/**
 * Good inside the recommended range, ok within one cycle of it, plus 4.5 h (three cycles), which the
 * PWA always rates ok because it is the shortest sleep that still ends between cycles of a nap-free night.
 */
fun rate(sleepMinutes: Int, age: AgeRange): Rating = when {
    sleepMinutes in age.minMinutes..age.maxMinutes -> Rating.Good
    sleepMinutes in (age.minMinutes - CYCLE_MINUTES)..(age.maxMinutes + CYCLE_MINUTES) -> Rating.Ok
    sleepMinutes == 3 * CYCLE_MINUTES -> Rating.Ok
    else -> Rating.Low
}

fun suggestions(mode: Mode, anchor: Int, latencyMinutes: Int, age: AgeRange): List<Suggestion> {
    val latency = clampLatency(latencyMinutes)
    return CYCLE_COUNTS.map { cycles ->
        val sleep = cycles * CYCLE_MINUTES
        val offset = latency + sleep
        val time = if (mode == Mode.Wake) wrapDay(anchor - offset) else wrapDay(anchor + offset)
        Suggestion(cycles, time, sleep, rate(sleep, age))
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :shared:desktopTest --tests "*SleepCalcTest*"`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add android/SleepCycles/shared/src
git commit -m "SleepCycles: sleep cycle calculation and age-based rating

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Time parsing, formatting and latency input

**Files:**
- Create: `shared/src/commonMain/kotlin/com/david7ce/sleepcycles/logic/TimeFormat.kt`
- Test: `shared/src/commonTest/kotlin/com/david7ce/sleepcycles/logic/TimeFormatTest.kt`

**Interfaces:**
- Consumes: `wrapDay`, `MAX_LATENCY_MINUTES` (Task 3).
- Produces:
  - `enum class ClockFormat { H24, H12 }`
  - `data class TimeParts(val hour: String, val minute: String, val pm: Boolean)`
  - `fun timeParts(minutesOfDay: Int, format: ClockFormat): TimeParts`
  - `fun parseTime(hourText: String, minuteText: String, pm: Boolean, format: ClockFormat): Int?`
  - `fun formatTime(minutesOfDay: Int, format: ClockFormat, am: String = "AM", pm: String = "PM"): String`
  - `fun formatHours(minutes: Int, separator: Char): String`
  - `fun parseLatency(text: String): Int`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.david7ce.sleepcycles.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TimeFormatTest {
    @Test
    fun partsIn24Hours() {
        assertEquals(TimeParts("07", "05", false), timeParts(7 * 60 + 5, ClockFormat.H24))
        assertEquals(TimeParts("23", "59", true), timeParts(23 * 60 + 59, ClockFormat.H24))
    }

    @Test
    fun partsIn12HoursHandleMidnightAndNoon() {
        assertEquals(TimeParts("12", "00", false), timeParts(0, ClockFormat.H12))
        assertEquals(TimeParts("12", "30", true), timeParts(12 * 60 + 30, ClockFormat.H12))
        assertEquals(TimeParts("01", "05", true), timeParts(13 * 60 + 5, ClockFormat.H12))
    }

    @Test
    fun parse12HourMidnightAndNoon() {
        assertEquals(0, parseTime("12", "00", false, ClockFormat.H12))
        assertEquals(12 * 60, parseTime("12", "00", true, ClockFormat.H12))
        assertEquals(19 * 60 + 5, parseTime("7", "5", true, ClockFormat.H12))
    }

    @Test
    fun invalidTextIsRejected() {
        assertNull(parseTime("0", "30", false, ClockFormat.H12))
        assertNull(parseTime("13", "00", false, ClockFormat.H12))
        assertNull(parseTime("24", "00", false, ClockFormat.H24))
        assertNull(parseTime("07", "60", false, ClockFormat.H24))
        assertNull(parseTime("", "30", false, ClockFormat.H24))
        assertNull(parseTime("ab", "30", false, ClockFormat.H24))
        assertNull(parseTime("07", "", false, ClockFormat.H24))
        assertNull(parseTime("-1", "30", false, ClockFormat.H24))
    }

    @Test
    fun everyMinuteOfTheDayRoundTripsInBothFormats() {
        for (format in ClockFormat.entries) for (m in 0 until MINUTES_PER_DAY) {
            val p = timeParts(m, format)
            assertEquals(m, parseTime(p.hour, p.minute, p.pm, format), "$format $m")
        }
    }

    @Test
    fun formattedTimes() {
        assertEquals("07:00", formatTime(7 * 60, ClockFormat.H24))
        assertEquals("00:45", formatTime(45, ClockFormat.H24))
        assertEquals("12:00 AM", formatTime(0, ClockFormat.H12))
        assertEquals("7:05 PM", formatTime(19 * 60 + 5, ClockFormat.H12))
        assertEquals("7:05 p. m.", formatTime(19 * 60 + 5, ClockFormat.H12, pm = "p. m."))
    }

    @Test
    fun hoursUseTheLanguagesDecimalSeparator() {
        assertEquals("9", formatHours(540, '.'))
        assertEquals("7.5", formatHours(450, '.'))
        assertEquals("1,5", formatHours(90, ','))
        assertEquals("4,5", formatHours(270, ','))
    }

    @Test
    fun latencyTextNeverBreaksTheCalculation() {
        assertEquals(15, parseLatency("15"))
        assertEquals(20, parseLatency(" 20 "))
        assertEquals(0, parseLatency(""))
        assertEquals(0, parseLatency("abc"))
        assertEquals(0, parseLatency("-5"))
        assertEquals(0, parseLatency("1.5"))
        assertEquals(120, parseLatency("500"))
        assertEquals(120, parseLatency("99999999999"))
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :shared:desktopTest --tests "*TimeFormatTest*"`
Expected: FAIL, unresolved reference `timeParts`.

- [ ] **Step 3: Implement**

```kotlin
package com.david7ce.sleepcycles.logic

enum class ClockFormat { H24, H12 }

/** What the hour and minute fields show. [pm] is only meaningful in 12-hour format. */
data class TimeParts(val hour: String, val minute: String, val pm: Boolean)

private fun Int.pad2() = toString().padStart(2, '0')

fun timeParts(minutesOfDay: Int, format: ClockFormat): TimeParts {
    val m = wrapDay(minutesOfDay)
    val hour24 = m / 60
    val shown = if (format == ClockFormat.H24) hour24 else (hour24 % 12).let { if (it == 0) 12 else it }
    return TimeParts(shown.pad2(), (m % 60).pad2(), pm = hour24 >= 12)
}

/** Minutes of the day, or null while the text is not a complete, valid time (the caller keeps the last valid one). */
fun parseTime(hourText: String, minuteText: String, pm: Boolean, format: ClockFormat): Int? {
    val h = hourText.toIntOrNull() ?: return null
    val m = minuteText.toIntOrNull() ?: return null
    if (m !in 0..59) return null
    val hour24 = when (format) {
        ClockFormat.H24 -> if (h in 0..23) h else return null
        ClockFormat.H12 -> if (h in 1..12) (h % 12) + (if (pm) 12 else 0) else return null
    }
    return hour24 * 60 + m
}

fun formatTime(minutesOfDay: Int, format: ClockFormat, am: String = "AM", pm: String = "PM"): String {
    val p = timeParts(minutesOfDay, format)
    return if (format == ClockFormat.H24) "${p.hour}:${p.minute}"
    else "${p.hour.trimStart('0')}:${p.minute} ${if (p.pm) pm else am}"
}

/** Whole or half hours ("9", "7,5"); every cycle count is a multiple of 30 minutes. */
fun formatHours(minutes: Int, separator: Char): String {
    val whole = minutes / 60
    return if (minutes % 60 == 0) "$whole" else "$whole${separator}5"
}

/** The PWA's field: empty, negative or non-numeric counts as 0; above its maximum is capped. */
fun parseLatency(text: String): Int =
    text.trim().toLongOrNull()?.coerceIn(0L, MAX_LATENCY_MINUTES.toLong())?.toInt() ?: 0
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :shared:desktopTest --tests "*TimeFormatTest*"`
Expected: PASS (8 tests).

- [ ] **Step 5: Commit**

```bash
git add android/SleepCycles/shared/src
git commit -m "SleepCycles: time parsing/formatting and latency input rules

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Settings model and persistence

**Files:**
- Create: `shared/src/commonMain/kotlin/com/david7ce/sleepcycles/platform/Services.kt`
- Create: `shared/src/commonMain/kotlin/com/david7ce/sleepcycles/logic/UserSettings.kt`
- Create: `shared/src/commonTest/kotlin/com/david7ce/sleepcycles/TestSupport.kt`
- Test: `shared/src/commonTest/kotlin/com/david7ce/sleepcycles/logic/UserSettingsTest.kt`

**Interfaces:**
- Consumes: `Mode`, `AgeRange`, `ClockFormat`, `MINUTES_PER_DAY`, `DEFAULT_LATENCY_MINUTES`, `clampLatency` (Tasks 3-4).
- Produces:
  - `interface SettingsStore { fun get(key: String): String?; fun put(key: String, value: String) }`
  - `interface Clock { fun minutesOfDay(): Int }`
  - `class Services(val settings: SettingsStore, val clock: Clock)`
  - `enum class ThemeMode { System, Light, Dark }`
  - `enum class AppLanguage(val code: String?, val nativeName: String) { System, Es, En, Fr, De, Pt, It }`
  - `data class UserSettings(mode, wakeTime, bedTime, age, latencyMinutes, format, theme, language)` with defaults
  - `fun SettingsStore.load(): UserSettings`, `fun SettingsStore.save(settings: UserSettings)`
  - Test helpers `MapStore`, `FakeClock`.

- [ ] **Step 1: Test support** (`TestSupport.kt`)

```kotlin
package com.david7ce.sleepcycles

import com.david7ce.sleepcycles.platform.Clock
import com.david7ce.sleepcycles.platform.SettingsStore

class MapStore(val values: MutableMap<String, String> = mutableMapOf()) : SettingsStore {
    override fun get(key: String): String? = values[key]
    override fun put(key: String, value: String) { values[key] = value }
}

class FakeClock(var minutes: Int = 0) : Clock {
    override fun minutesOfDay(): Int = minutes
}
```

- [ ] **Step 2: Write the failing test**

```kotlin
package com.david7ce.sleepcycles.logic

import com.david7ce.sleepcycles.MapStore
import kotlin.test.Test
import kotlin.test.assertEquals

class UserSettingsTest {
    @Test
    fun emptyStoreGivesTheDefaults() {
        val s = MapStore().load()
        assertEquals(UserSettings(), s)
        assertEquals(Mode.Wake, s.mode)
        assertEquals(7 * 60, s.wakeTime)
        assertEquals(23 * 60, s.bedTime)
        assertEquals(AgeRange.Adult, s.age)
        assertEquals(15, s.latencyMinutes)
        assertEquals(ClockFormat.H24, s.format)
    }

    @Test
    fun saveThenLoadRoundTrips() {
        val store = MapStore()
        val saved = UserSettings(
            mode = Mode.Bed, wakeTime = 6 * 60 + 30, bedTime = 22 * 60 + 10, age = AgeRange.Senior,
            latencyMinutes = 40, format = ClockFormat.H12, theme = ThemeMode.Dark, language = AppLanguage.De,
        )
        store.save(saved)
        assertEquals(saved, store.load())
    }

    @Test
    fun corruptValuesFallBackFieldByField() {
        val store = MapStore(
            mutableMapOf(
                "mode" to "Sideways", "wakeTime" to "soon", "bedTime" to "1440", "age" to "Martian",
                "latency" to "999", "format" to "H12", "theme" to "", "language" to "Klingon",
            )
        )
        val s = store.load()
        val d = UserSettings()
        assertEquals(d.mode, s.mode)
        assertEquals(d.wakeTime, s.wakeTime)
        assertEquals(d.bedTime, s.bedTime)
        assertEquals(d.age, s.age)
        assertEquals(120, s.latencyMinutes)       // numeric but out of range: clamped, not discarded
        assertEquals(ClockFormat.H12, s.format)   // the one valid value is kept
        assertEquals(d.theme, s.theme)
        assertEquals(d.language, s.language)
    }

    @Test
    fun negativeTimeIsRejected() {
        assertEquals(UserSettings().wakeTime, MapStore(mutableMapOf("wakeTime" to "-5")).load().wakeTime)
    }
}
```

- [ ] **Step 3: Run to verify it fails**

Run: `./gradlew :shared:desktopTest --tests "*UserSettingsTest*"`
Expected: FAIL, unresolved reference `UserSettings`.

- [ ] **Step 4: Implement `Services.kt`**

```kotlin
package com.david7ce.sleepcycles.platform

/** String key/value persistence: SharedPreferences on Android, java.util.prefs on desktop. */
interface SettingsStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
}

/** Current time of day; an interface so tests (and "Sleep now") are deterministic. */
interface Clock {
    fun minutesOfDay(): Int
}

class Services(val settings: SettingsStore, val clock: Clock)
```

- [ ] **Step 5: Implement `UserSettings.kt`**

```kotlin
package com.david7ce.sleepcycles.logic

import com.david7ce.sleepcycles.platform.SettingsStore

enum class ThemeMode { System, Light, Dark }

/** Languages the app offers. [code] is the ISO 639 code; System follows the device. Names are shown in their own language. */
enum class AppLanguage(val code: String?, val nativeName: String) {
    System(null, ""),
    Es("es", "Español"),
    En("en", "English"),
    Fr("fr", "Français"),
    De("de", "Deutsch"),
    Pt("pt", "Português"),
    It("it", "Italiano"),
}

/** Everything that survives a restart. Defaults are the PWA's. */
data class UserSettings(
    val mode: Mode = Mode.Wake,
    val wakeTime: Int = 7 * 60,
    val bedTime: Int = 23 * 60,
    val age: AgeRange = AgeRange.Adult,
    val latencyMinutes: Int = DEFAULT_LATENCY_MINUTES,
    val format: ClockFormat = ClockFormat.H24,
    val theme: ThemeMode = ThemeMode.System,
    val language: AppLanguage = AppLanguage.System,
)

private object Key {
    const val MODE = "mode"
    const val WAKE = "wakeTime"
    const val BED = "bedTime"
    const val AGE = "age"
    const val LATENCY = "latency"
    const val FORMAT = "format"
    const val THEME = "theme"
    const val LANGUAGE = "language"
}

private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default

private fun timeOr(text: String?, default: Int): Int =
    text?.toIntOrNull()?.takeIf { it in 0 until MINUTES_PER_DAY } ?: default

/** Each field falls back to its default on its own, so one corrupt value never loses the rest. */
fun SettingsStore.load(): UserSettings {
    val d = UserSettings()
    return UserSettings(
        mode = enumOr(get(Key.MODE), d.mode),
        wakeTime = timeOr(get(Key.WAKE), d.wakeTime),
        bedTime = timeOr(get(Key.BED), d.bedTime),
        age = enumOr(get(Key.AGE), d.age),
        latencyMinutes = get(Key.LATENCY)?.toIntOrNull()?.let(::clampLatency) ?: d.latencyMinutes,
        format = enumOr(get(Key.FORMAT), d.format),
        theme = enumOr(get(Key.THEME), d.theme),
        language = enumOr(get(Key.LANGUAGE), d.language),
    )
}

fun SettingsStore.save(settings: UserSettings) {
    put(Key.MODE, settings.mode.name)
    put(Key.WAKE, settings.wakeTime.toString())
    put(Key.BED, settings.bedTime.toString())
    put(Key.AGE, settings.age.name)
    put(Key.LATENCY, settings.latencyMinutes.toString())
    put(Key.FORMAT, settings.format.name)
    put(Key.THEME, settings.theme.name)
    put(Key.LANGUAGE, settings.language.name)
}
```

- [ ] **Step 6: Run to verify it passes**

Run: `./gradlew :shared:desktopTest --tests "*UserSettingsTest*"`
Expected: PASS (4 tests).

- [ ] **Step 7: Commit**

```bash
git add android/SleepCycles/shared/src
git commit -m "SleepCycles: settings model and tolerant persistence

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 6: AppState

**Files:**
- Create: `shared/src/commonMain/kotlin/com/david7ce/sleepcycles/ui/AppState.kt`
- Test: `shared/src/commonTest/kotlin/com/david7ce/sleepcycles/ui/AppStateTest.kt`

**Interfaces:**
- Consumes: Tasks 3-5 (`Mode`, `AgeRange`, `ClockFormat`, `ThemeMode`, `AppLanguage`, `UserSettings`, `Suggestion`, `suggestions`, `clampLatency`, `Services`, `load`, `save`).
- Produces:
  ```kotlin
  sealed interface Screen { data object Main : Screen; data object Settings : Screen }
  class AppState(services: Services, systemLanguage: String, val appVersion: String) {
      var mode: Mode; val wakeTime: Int; val bedTime: Int; val age: AgeRange; val latencyMinutes: Int
      val format: ClockFormat; val theme: ThemeMode; val language: AppLanguage
      var systemLanguage: String
      val screen: Screen; val showHelp: Boolean
      val anchor: Int; val suggestions: List<Suggestion>; val canGoBack: Boolean
      fun setMode(m: Mode); fun setAnchor(minutes: Int); fun sleepNow()
      fun setAge(a: AgeRange); fun setLatency(minutes: Int); fun setFormat(f: ClockFormat)
      fun setTheme(t: ThemeMode); fun setLanguage(l: AppLanguage)
      fun openSettings(); fun openHelp(); fun closeHelp(); fun back()
  }
  ```
  (`strings` is added in Task 7.)

- [ ] **Step 1: Write the failing test**

```kotlin
package com.david7ce.sleepcycles.ui

import com.david7ce.sleepcycles.FakeClock
import com.david7ce.sleepcycles.MapStore
import com.david7ce.sleepcycles.logic.AgeRange
import com.david7ce.sleepcycles.logic.ClockFormat
import com.david7ce.sleepcycles.logic.Mode
import com.david7ce.sleepcycles.logic.load
import com.david7ce.sleepcycles.platform.Services
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppStateTest {
    private fun state(store: MapStore = MapStore(), clock: FakeClock = FakeClock(), language: String = "en") =
        AppState(Services(store, clock), language, "1.0")

    @Test
    fun startsWithTheDefaults() {
        val s = state()
        assertEquals(Mode.Wake, s.mode)
        assertEquals(7 * 60, s.anchor)
        assertEquals(6, s.suggestions.size)
        assertEquals(Screen.Main, s.screen)
        assertFalse(s.showHelp)
    }

    @Test
    fun theAnchorBelongsToTheCurrentMode() {
        val s = state()
        s.setAnchor(6 * 60)
        assertEquals(6 * 60, s.wakeTime)
        assertEquals(23 * 60, s.bedTime)
        s.setMode(Mode.Bed)
        assertEquals(23 * 60, s.anchor)
        s.setAnchor(22 * 60)
        assertEquals(22 * 60, s.bedTime)
        assertEquals(6 * 60, s.wakeTime)
    }

    @Test
    fun sleepNowSwitchesToBedModeAtTheCurrentTimeAndPersists() {
        val store = MapStore()
        val s = state(store, FakeClock(23 * 60 + 59))
        s.sleepNow()
        s.sleepNow() // repeated taps are harmless
        assertEquals(Mode.Bed, s.mode)
        assertEquals(23 * 60 + 59, s.bedTime)
        assertEquals(23 * 60 + 59, store.load().bedTime)
        assertEquals(Mode.Bed, store.load().mode)
        assertTrue(s.suggestions.all { it.timeOfDay in 0..1439 })
    }

    @Test
    fun changesSurviveARestart() {
        val store = MapStore()
        val first = state(store)
        first.setAge(AgeRange.Teen)
        first.setLatency(30)
        first.setFormat(ClockFormat.H12)
        val second = state(store)
        assertEquals(AgeRange.Teen, second.age)
        assertEquals(30, second.latencyMinutes)
        assertEquals(ClockFormat.H12, second.format)
    }

    @Test
    fun latencyIsClamped() {
        val s = state()
        s.setLatency(5000)
        assertEquals(120, s.latencyMinutes)
        s.setLatency(-3)
        assertEquals(0, s.latencyMinutes)
    }

    @Test
    fun backClosesHelpFirstThenSettings() {
        val s = state()
        s.openSettings()
        s.openHelp()
        assertTrue(s.canGoBack)
        s.back()
        assertFalse(s.showHelp)
        assertEquals(Screen.Settings, s.screen)
        s.back()
        assertEquals(Screen.Main, s.screen)
        assertFalse(s.canGoBack)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :shared:desktopTest --tests "*AppStateTest*"`
Expected: FAIL, unresolved reference `AppState`.

- [ ] **Step 3: Implement**

```kotlin
package com.david7ce.sleepcycles.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.david7ce.sleepcycles.logic.AgeRange
import com.david7ce.sleepcycles.logic.AppLanguage
import com.david7ce.sleepcycles.logic.ClockFormat
import com.david7ce.sleepcycles.logic.Mode
import com.david7ce.sleepcycles.logic.Suggestion
import com.david7ce.sleepcycles.logic.ThemeMode
import com.david7ce.sleepcycles.logic.UserSettings
import com.david7ce.sleepcycles.logic.clampLatency
import com.david7ce.sleepcycles.logic.load
import com.david7ce.sleepcycles.logic.save
import com.david7ce.sleepcycles.logic.suggestions as calculateSuggestions // the property below has the same name
import com.david7ce.sleepcycles.platform.Services

sealed interface Screen {
    data object Main : Screen
    data object Settings : Screen
}

/**
 * All UI state. Every setter persists immediately: the data is tiny and the app can be killed at any time.
 * The Android Application owns one instance so it outlives activity recreation.
 */
class AppState(
    private val services: Services,
    systemLanguage: String,
    val appVersion: String,
) {
    private val saved = services.settings.load()

    var mode by mutableStateOf(saved.mode); private set
    var wakeTime by mutableStateOf(saved.wakeTime); private set
    var bedTime by mutableStateOf(saved.bedTime); private set
    var age by mutableStateOf(saved.age); private set
    var latencyMinutes by mutableStateOf(saved.latencyMinutes); private set
    var format by mutableStateOf(saved.format); private set
    var theme by mutableStateOf(saved.theme); private set
    var language by mutableStateOf(saved.language); private set

    /** The device language (ISO 639 code), updated by the platform; used when [language] is System. */
    var systemLanguage by mutableStateOf(systemLanguage)

    var screen by mutableStateOf<Screen>(Screen.Main); private set
    var showHelp by mutableStateOf(false); private set

    /** The time the user fixes: the wake-up time in Wake mode, the bedtime in Bed mode. */
    val anchor: Int get() = if (mode == Mode.Wake) wakeTime else bedTime
    val suggestions: List<Suggestion> get() = calculateSuggestions(mode, anchor, latencyMinutes, age)
    val canGoBack: Boolean get() = showHelp || screen != Screen.Main

    fun setMode(value: Mode) { mode = value; persist() }

    fun setAnchor(minutes: Int) {
        if (mode == Mode.Wake) wakeTime = minutes else bedTime = minutes
        persist()
    }

    /** Same as the PWA's "Sleep now": bed mode at the current time. */
    fun sleepNow() {
        mode = Mode.Bed
        bedTime = services.clock.minutesOfDay()
        persist()
    }

    fun setAge(value: AgeRange) { age = value; persist() }
    fun setLatency(minutes: Int) { latencyMinutes = clampLatency(minutes); persist() }
    fun setFormat(value: ClockFormat) { format = value; persist() }
    fun setTheme(value: ThemeMode) { theme = value; persist() }
    fun setLanguage(value: AppLanguage) { language = value; persist() }

    fun openSettings() { screen = Screen.Settings }
    fun openHelp() { showHelp = true }
    fun closeHelp() { showHelp = false }

    /** Closes the help dialog first, then leaves Settings. */
    fun back() {
        when {
            showHelp -> showHelp = false
            screen != Screen.Main -> screen = Screen.Main
        }
    }

    private fun persist() {
        services.settings.save(
            UserSettings(mode, wakeTime, bedTime, age, latencyMinutes, format, theme, language)
        )
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :shared:desktopTest --tests "*AppStateTest*"`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add android/SleepCycles/shared/src
git commit -m "SleepCycles: AppState with persistence, Sleep now and back handling

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Strings and translations

**Files:**
- Create: `shared/src/commonMain/kotlin/com/david7ce/sleepcycles/ui/Strings.kt`
- Create: `shared/src/commonMain/kotlin/com/david7ce/sleepcycles/ui/Translations.kt`
- Modify: `shared/src/commonMain/kotlin/com/david7ce/sleepcycles/ui/AppState.kt` (add `strings`)
- Test: `shared/src/commonTest/kotlin/com/david7ce/sleepcycles/StringsTest.kt`

**Interfaces:**
- Consumes: `AgeRange`, `AppLanguage` (Tasks 3, 5), `AppState.language`, `systemLanguage` (Task 6).
- Produces: `open class Strings` (English); objects `EnStrings`, `EsStrings`, `FrStrings`, `DeStrings`, `PtStrings`, `ItStrings`; `fun stringsFor(languageCode: String): Strings`; `val LocalStrings`; `AppState.strings: Strings`. Members used by the UI: `decimalSeparator: Char`, `wakeUpAt`, `goToBedAt`, `sleepNow`, `resultsWake`, `resultsBed`, `hint(latency)`, `cycles(n, hours)`, `hourLabel`, `minuteLabel`, `am`, `pm`, `ratingGood/Ok/Low`, `ageLabel(AgeRange)`, `settings`, `ageRange`, `fallAsleepTime`, `timeFormat`, `format24`, `format12`, `theme`, `themeSystem/Light/Dark`, `language`, `languageSystem`, `versionLabel(v)`, `howItWorks`, `howItWorksBody`, `gotIt`, `back`, `close`, `cancel`, `delete`, `selectAll`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.david7ce.sleepcycles

import com.david7ce.sleepcycles.logic.AgeRange
import com.david7ce.sleepcycles.logic.AppLanguage
import com.david7ce.sleepcycles.ui.EnStrings
import com.david7ce.sleepcycles.ui.stringsFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class StringsTest {
    @Test
    fun everyLanguageHasItsOwnTexts() {
        val en = EnStrings
        AppLanguage.entries.filter { it.code != null && it.code != "en" }.forEach { language ->
            val s = stringsFor(language.code!!)
            listOf(
                s.wakeUpAt to en.wakeUpAt, s.goToBedAt to en.goToBedAt, s.sleepNow to en.sleepNow,
                s.resultsWake to en.resultsWake, s.resultsBed to en.resultsBed,
                s.hint(15) to en.hint(15), s.cycles(3, "4.5") to en.cycles(3, "4.5"),
                s.ratingGood to en.ratingGood, s.ratingOk to en.ratingOk, s.ratingLow to en.ratingLow,
                s.ageLabel(AgeRange.Adult) to en.ageLabel(AgeRange.Adult),
                s.settings to en.settings, s.ageRange to en.ageRange, s.fallAsleepTime to en.fallAsleepTime,
                s.timeFormat to en.timeFormat, s.format12 to en.format12, s.theme to en.theme,
                s.themeDark to en.themeDark, s.language to en.language, s.languageSystem to en.languageSystem,
                s.howItWorks to en.howItWorks, s.howItWorksBody to en.howItWorksBody, s.gotIt to en.gotIt,
                s.back to en.back, s.close to en.close, s.cancel to en.cancel,
            ).forEach { (translated, original) -> assertNotEquals(original, translated, "${language.name}: $translated") }
            assertEquals(',', s.decimalSeparator, language.name)
        }
        assertEquals('.', en.decimalSeparator)
    }

    @Test
    fun placeholdersAreFilled() {
        AppLanguage.entries.mapNotNull { it.code }.forEach { code ->
            val s = stringsFor(code)
            assertTrue(s.hint(25).contains("25"), "$code hint")
            assertTrue(s.cycles(6, "9").contains("6") && s.cycles(6, "9").contains("9"), "$code cycles")
            assertTrue(s.versionLabel("1.0.7").contains("1.0.7"), "$code version")
        }
    }

    @Test
    fun oneCycleIsSingular() {
        assertEquals("1 cycle · 1.5 h", EnStrings.cycles(1, "1.5"))
        assertEquals("6 cycles · 9 h", EnStrings.cycles(6, "9"))
    }

    @Test
    fun unknownOrRegionalLanguagesResolveSafely() {
        assertSame(EnStrings, stringsFor("ja"))
        assertSame(EnStrings, stringsFor("ar"))
        assertSame(EnStrings, stringsFor(""))
        assertSame(stringsFor("pt"), stringsFor("pt-BR"))
        assertSame(stringsFor("es"), stringsFor("ES"))
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :shared:desktopTest --tests "*StringsTest*"`
Expected: FAIL, unresolved reference `EnStrings`.

- [ ] **Step 3: `Strings.kt`**

```kotlin
package com.david7ce.sleepcycles.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.david7ce.sleepcycles.logic.AgeRange

/**
 * UI text. English is the base; each language overrides it (Translations.kt). Plain Kotlin instead
 * of a resources library: identical on every platform, and a missing translation falls back to
 * English instead of crashing. (Same approach as Recetario.)
 */
open class Strings {
    /** Separator in "7.5 h". */
    open val decimalSeparator = '.'

    // Main
    open val wakeUpAt = "Wake up at"
    open val goToBedAt = "Go to bed at"
    open val sleepNow = "Sleep now"
    open val resultsWake = "Go to bed at one of these times"
    open val resultsBed = "Wake up at one of these times"
    open fun hint(latency: Int) = "1 cycle = 90 min · includes $latency min to fall asleep"
    open fun cycles(n: Int, hours: String) = (if (n == 1) "1 cycle" else "$n cycles") + " · $hours h"
    open val hourLabel = "Hour"
    open val minuteLabel = "Min"
    open val am = "AM"
    open val pm = "PM"
    open val ratingGood = "Within the recommended range for your age"
    open val ratingOk = "Slightly outside the recommended range"
    open val ratingLow = "Far from the recommended range"

    // Settings
    open val settings = "Settings"
    open val ageRange = "Age range"
    protected open val ageChild = "Child (6–13) · 9–11 h"
    protected open val ageTeen = "Teen (14–17) · 8–10 h"
    protected open val ageAdult = "Adult (18–64) · 7–9 h"
    protected open val ageSenior = "Senior (65+) · 7–8 h"
    fun ageLabel(age: AgeRange): String = when (age) {
        AgeRange.Child -> ageChild
        AgeRange.Teen -> ageTeen
        AgeRange.Adult -> ageAdult
        AgeRange.Senior -> ageSenior
    }
    open val fallAsleepTime = "Time to fall asleep (minutes)"
    open val timeFormat = "Time format"
    open val format24 = "24-hour"
    open val format12 = "12-hour (AM/PM)"
    open val theme = "Theme"
    open val themeSystem = "System"
    open val themeLight = "Light"
    open val themeDark = "Dark"
    open val language = "Language"
    open val languageSystem = "System default"
    open fun versionLabel(version: String) = "Version $version"

    // Help
    open val howItWorks = "How does it work?"
    open val howItWorksBody =
        "Sleep goes through cycles of about 90 minutes. Waking up in the middle of one leaves you groggy; " +
            "waking up between two is much easier. The app counts back (or forward) in 90-minute steps and adds " +
            "the time you usually need to fall asleep. Cycle length varies from person to person, so treat the " +
            "times as a guide. The faces show how each option compares with the recommended sleep for your age range."
    open val gotIt = "Got it"

    // Shared components (android/shared)
    open val back = "Back"
    open val close = "Close"
    open val cancel = "Cancel"
    open val delete = "Delete"
    open val selectAll = "Select all"
}

val LocalStrings = staticCompositionLocalOf<Strings> { EnStrings }
```

- [ ] **Step 4: `Translations.kt`**

```kotlin
package com.david7ce.sleepcycles.ui

/** Strings for an ISO 639 language code ("pt-BR" and "ES" work); English for anything not translated. */
fun stringsFor(languageCode: String): Strings = when (languageCode.lowercase().take(2)) {
    "es" -> EsStrings
    "fr" -> FrStrings
    "de" -> DeStrings
    "pt" -> PtStrings
    "it" -> ItStrings
    else -> EnStrings
}

object EnStrings : Strings()

object EsStrings : Strings() {
    override val decimalSeparator = ','
    override val wakeUpAt = "Despertar a las"
    override val goToBedAt = "Dormir a las"
    override val sleepNow = "Dormir ahora"
    override val resultsWake = "Acuéstate a una de estas horas"
    override val resultsBed = "Despierta a una de estas horas"
    override fun hint(latency: Int) = "1 ciclo = 90 min · incluye $latency min para dormirse"
    override fun cycles(n: Int, hours: String) = (if (n == 1) "1 ciclo" else "$n ciclos") + " · $hours h"
    override val hourLabel = "Hora"
    override val minuteLabel = "Min"
    override val ratingGood = "Dentro del rango recomendado para tu edad"
    override val ratingOk = "Ligeramente fuera del rango recomendado"
    override val ratingLow = "Lejos del rango recomendado"
    override val settings = "Ajustes"
    override val ageRange = "Rango de edad"
    override val ageChild = "Niño (6–13) · 9–11 h"
    override val ageTeen = "Adolescente (14–17) · 8–10 h"
    override val ageAdult = "Adulto (18–64) · 7–9 h"
    override val ageSenior = "Mayor (65+) · 7–8 h"
    override val fallAsleepTime = "Tiempo para dormirse (minutos)"
    override val timeFormat = "Formato de hora"
    override val format24 = "24 horas"
    override val format12 = "12 horas (AM/PM)"
    override val theme = "Tema"
    override val themeSystem = "Sistema"
    override val themeLight = "Claro"
    override val themeDark = "Oscuro"
    override val language = "Idioma"
    override val languageSystem = "Predeterminado del sistema"
    override fun versionLabel(version: String) = "Versión $version"
    override val howItWorks = "¿Cómo funciona?"
    override val howItWorksBody =
        "El sueño pasa por ciclos de unos 90 minutos. Despertar en mitad de uno deja una sensación de " +
            "aturdimiento; despertar entre dos es mucho más fácil. La app cuenta hacia atrás (o hacia delante) " +
            "en pasos de 90 minutos y suma el tiempo que sueles tardar en dormirte. La duración del ciclo varía " +
            "de una persona a otra, así que toma las horas como una guía. Las caras indican cómo se compara cada " +
            "opción con el sueño recomendado para tu edad."
    override val gotIt = "Entendido"
    override val back = "Atrás"
    override val close = "Cerrar"
    override val cancel = "Cancelar"
    override val delete = "Eliminar"
    override val selectAll = "Seleccionar todo"
}

object FrStrings : Strings() {
    override val decimalSeparator = ','
    override val wakeUpAt = "Me réveiller à"
    override val goToBedAt = "Me coucher à"
    override val sleepNow = "Dormir maintenant"
    override val resultsWake = "Couchez-vous à l’une de ces heures"
    override val resultsBed = "Réveillez-vous à l’une de ces heures"
    override fun hint(latency: Int) = "1 cycle = 90 min · inclut $latency min pour s’endormir"
    override fun cycles(n: Int, hours: String) = (if (n == 1) "1 cycle" else "$n cycles") + " · $hours h"
    override val hourLabel = "Heure"
    override val minuteLabel = "Min"
    override val ratingGood = "Dans la plage recommandée pour votre âge"
    override val ratingOk = "Légèrement hors de la plage recommandée"
    override val ratingLow = "Loin de la plage recommandée"
    override val settings = "Réglages"
    override val ageRange = "Tranche d’âge"
    override val ageChild = "Enfant (6–13) · 9–11 h"
    override val ageTeen = "Ado (14–17) · 8–10 h"
    override val ageAdult = "Adulte (18–64) · 7–9 h"
    override val ageSenior = "Senior (65+) · 7–8 h"
    override val fallAsleepTime = "Temps pour s’endormir (minutes)"
    override val timeFormat = "Format de l’heure"
    override val format24 = "24 heures"
    override val format12 = "12 heures (AM/PM)"
    override val theme = "Thème"
    override val themeSystem = "Système"
    override val themeLight = "Clair"
    override val themeDark = "Sombre"
    override val language = "Langue"
    override val languageSystem = "Par défaut du système"
    override fun versionLabel(version: String) = "Version $version"
    override val howItWorks = "Comment ça marche ?"
    override val howItWorksBody =
        "Le sommeil se déroule en cycles d’environ 90 minutes. Se réveiller au milieu d’un cycle laisse une " +
            "sensation de lourdeur ; se réveiller entre deux est bien plus facile. L’application compte à rebours " +
            "(ou en avant) par tranches de 90 minutes et ajoute le temps dont vous avez besoin pour vous endormir. " +
            "La durée d’un cycle varie d’une personne à l’autre : considérez ces heures comme un repère. Les visages " +
            "indiquent comment chaque option se situe par rapport au sommeil recommandé pour votre âge."
    override val gotIt = "Compris"
    override val back = "Retour"
    override val close = "Fermer"
    override val cancel = "Annuler"
    override val delete = "Supprimer"
    override val selectAll = "Tout sélectionner"
}

object DeStrings : Strings() {
    override val decimalSeparator = ','
    override val wakeUpAt = "Aufwachen um"
    override val goToBedAt = "Schlafen um"
    override val sleepNow = "Jetzt schlafen"
    override val resultsWake = "Geh zu einer dieser Zeiten schlafen"
    override val resultsBed = "Wach zu einer dieser Zeiten auf"
    override fun hint(latency: Int) = "1 Zyklus = 90 Min. · enthält $latency Min. zum Einschlafen"
    override fun cycles(n: Int, hours: String) = (if (n == 1) "1 Zyklus" else "$n Zyklen") + " · $hours Std."
    override val hourLabel = "Stunde"
    override val minuteLabel = "Min."
    override val ratingGood = "Im empfohlenen Bereich für dein Alter"
    override val ratingOk = "Leicht außerhalb des empfohlenen Bereichs"
    override val ratingLow = "Weit vom empfohlenen Bereich entfernt"
    override val settings = "Einstellungen"
    override val ageRange = "Altersgruppe"
    override val ageChild = "Kind (6–13) · 9–11 Std."
    override val ageTeen = "Jugendlicher (14–17) · 8–10 Std."
    override val ageAdult = "Erwachsener (18–64) · 7–9 Std."
    override val ageSenior = "Senior (65+) · 7–8 Std."
    override val fallAsleepTime = "Zeit zum Einschlafen (Minuten)"
    override val timeFormat = "Zeitformat"
    override val format24 = "24-Stunden"
    override val format12 = "12-Stunden (AM/PM)"
    override val theme = "Design"
    override val themeSystem = "System"
    override val themeLight = "Hell"
    override val themeDark = "Dunkel"
    override val language = "Sprache"
    override val languageSystem = "Systemstandard"
    override fun versionLabel(version: String) = "Version $version"
    override val howItWorks = "Wie funktioniert das?"
    override val howItWorksBody =
        "Schlaf verläuft in Zyklen von etwa 90 Minuten. Mitten in einem Zyklus aufzuwachen macht benommen; " +
            "zwischen zwei Zyklen aufzuwachen fällt viel leichter. Die App rechnet in 90-Minuten-Schritten zurück " +
            "(oder vor) und addiert die Zeit, die du zum Einschlafen brauchst. Die Zykluslänge ist von Mensch zu " +
            "Mensch verschieden, die Zeiten sind daher ein Richtwert. Die Gesichter zeigen, wie jede Option im " +
            "Vergleich zum empfohlenen Schlaf für dein Alter abschneidet."
    override val gotIt = "Verstanden"
    override val back = "Zurück"
    override val close = "Schließen"
    override val cancel = "Abbrechen"
    override val delete = "Löschen"
    override val selectAll = "Alle auswählen"
}

object PtStrings : Strings() {
    override val decimalSeparator = ','
    override val wakeUpAt = "Acordar às"
    override val goToBedAt = "Dormir às"
    override val sleepNow = "Dormir agora"
    override val resultsWake = "Durma em um destes horários"
    override val resultsBed = "Acorde em um destes horários"
    override fun hint(latency: Int) = "1 ciclo = 90 min · inclui $latency min para adormecer"
    override fun cycles(n: Int, hours: String) = (if (n == 1) "1 ciclo" else "$n ciclos") + " · $hours h"
    override val hourLabel = "Hora"
    override val minuteLabel = "Min"
    override val ratingGood = "Dentro da faixa recomendada para sua idade"
    override val ratingOk = "Um pouco fora da faixa recomendada"
    override val ratingLow = "Longe da faixa recomendada"
    override val settings = "Configurações"
    override val ageRange = "Faixa etária"
    override val ageChild = "Criança (6–13) · 9–11 h"
    override val ageTeen = "Adolescente (14–17) · 8–10 h"
    override val ageAdult = "Adulto (18–64) · 7–9 h"
    override val ageSenior = "Idoso (65+) · 7–8 h"
    override val fallAsleepTime = "Tempo para adormecer (minutos)"
    override val timeFormat = "Formato da hora"
    override val format24 = "24 horas"
    override val format12 = "12 horas (AM/PM)"
    override val theme = "Tema"
    override val themeSystem = "Sistema"
    override val themeLight = "Claro"
    override val themeDark = "Escuro"
    override val language = "Idioma"
    override val languageSystem = "Padrão do sistema"
    override fun versionLabel(version: String) = "Versão $version"
    override val howItWorks = "Como funciona?"
    override val howItWorksBody =
        "O sono passa por ciclos de cerca de 90 minutos. Acordar no meio de um deixa a sensação de " +
            "atordoamento; acordar entre dois é bem mais fácil. O app conta para trás (ou para frente) em passos " +
            "de 90 minutos e soma o tempo que você costuma levar para adormecer. A duração do ciclo varia de " +
            "pessoa para pessoa, então use os horários como referência. Os rostos mostram como cada opção se " +
            "compara ao sono recomendado para a sua idade."
    override val gotIt = "Entendi"
    override val back = "Voltar"
    override val close = "Fechar"
    override val cancel = "Cancelar"
    override val delete = "Excluir"
    override val selectAll = "Selecionar tudo"
}

object ItStrings : Strings() {
    override val decimalSeparator = ','
    override val wakeUpAt = "Svegliarmi alle"
    override val goToBedAt = "Dormire alle"
    override val sleepNow = "Dormi ora"
    override val resultsWake = "Vai a letto a uno di questi orari"
    override val resultsBed = "Svegliati a uno di questi orari"
    override fun hint(latency: Int) = "1 ciclo = 90 min · include $latency min per addormentarsi"
    override fun cycles(n: Int, hours: String) = (if (n == 1) "1 ciclo" else "$n cicli") + " · $hours h"
    override val hourLabel = "Ora"
    override val minuteLabel = "Min"
    override val ratingGood = "Entro l’intervallo consigliato per la tua età"
    override val ratingOk = "Leggermente fuori dall’intervallo consigliato"
    override val ratingLow = "Lontano dall’intervallo consigliato"
    override val settings = "Impostazioni"
    override val ageRange = "Fascia d’età"
    override val ageChild = "Bambino (6–13) · 9–11 h"
    override val ageTeen = "Adolescente (14–17) · 8–10 h"
    override val ageAdult = "Adulto (18–64) · 7–9 h"
    override val ageSenior = "Anziano (65+) · 7–8 h"
    override val fallAsleepTime = "Tempo per addormentarsi (minuti)"
    override val timeFormat = "Formato dell’ora"
    override val format24 = "24 ore"
    override val format12 = "12 ore (AM/PM)"
    override val theme = "Tema"
    override val themeSystem = "Sistema"
    override val themeLight = "Chiaro"
    override val themeDark = "Scuro"
    override val language = "Lingua"
    override val languageSystem = "Predefinita di sistema"
    override fun versionLabel(version: String) = "Versione $version"
    override val howItWorks = "Come funziona?"
    override val howItWorksBody =
        "Il sonno attraversa cicli di circa 90 minuti. Svegliarsi nel mezzo di uno lascia storditi; " +
            "svegliarsi tra due è molto più facile. L’app conta all’indietro (o in avanti) a passi di 90 minuti " +
            "e aggiunge il tempo che di solito serve per addormentarsi. La durata del ciclo varia da persona a " +
            "persona, quindi considera gli orari come un’indicazione. I volti mostrano come ogni opzione si " +
            "confronta con il sonno consigliato per la tua età."
    override val gotIt = "Capito"
    override val back = "Indietro"
    override val close = "Chiudi"
    override val cancel = "Annulla"
    override val delete = "Elimina"
    override val selectAll = "Seleziona tutto"
}
```

- [ ] **Step 5: Add `strings` to `AppState`**: insert after the `systemLanguage` declaration

```kotlin
    /** Texts in the chosen language, or the device's when the choice is System. */
    val strings: Strings get() = stringsFor(language.code ?: systemLanguage)
```

- [ ] **Step 6: Run to verify it passes**

Run: `./gradlew :shared:desktopTest --tests "*StringsTest*" --tests "*AppStateTest*"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add android/SleepCycles/shared/src
git commit -m "SleepCycles: strings in English, Spanish, French, German, Portuguese, Italian

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 8: Theme, icons, night sky and rating faces

**Files:**
- Create: `ui/Theme.kt`, `ui/AppIcons.kt`, `ui/NightSky.kt`, `ui/RatingFace.kt` (all under `shared/src/commonMain/kotlin/com/david7ce/sleepcycles/ui/`)

**Interfaces:**
- Consumes: `ThemeMode` (Task 5), `Rating` (Task 3).
- Produces:
  - `@Composable fun isDark(mode: ThemeMode): Boolean`
  - `@Composable fun SleepCyclesTheme(dark: Boolean, content: @Composable () -> Unit)`
  - `object AppIcons { val Settings: ImageVector; val Info: ImageVector }`
  - `@Composable fun NightSky(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit)`
  - `@Composable fun RatingFace(rating: Rating, description: String, dark: Boolean, modifier: Modifier = Modifier)`
  - `val WakeAccent: Color`, `val BedAccent: Color`

There is no logic to unit-test here; correctness is verified by compiling in CI (Task 14) and by looking at the app.

- [ ] **Step 1: `Theme.kt`**

```kotlin
package com.david7ce.sleepcycles.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.david7ce.sleepcycles.logic.ThemeMode

/** Mode accents from the PWA: warm sand for "wake up at", pale lavender for "go to bed at". */
val WakeAccent = Color(0xFFDFB987)
val BedAccent = Color(0xFFE2DDF8)

// Night palette taken from the PWA's stylesheet.
private val Dark = darkColorScheme(
    primary = Color(0xFFB3A5F2),
    onPrimary = Color(0xFF1A114F),
    primaryContainer = Color(0xFF2C2170),
    onPrimaryContainer = Color(0xFFECE8FB),
    secondary = Color(0xFFB3A5F2),
    onSecondary = Color(0xFF1A114F),
    secondaryContainer = Color(0xFF3A2E8C),
    onSecondaryContainer = Color(0xFFECE8FB),
    tertiary = WakeAccent,
    background = Color(0xFF0A0920),
    onBackground = Color(0xFFECE8FB),
    surface = Color(0xFF140E3D),
    onSurface = Color(0xFFECE8FB),
    surfaceVariant = Color(0xFF251A63),
    onSurfaceVariant = Color(0xFFBEB5E3),
    surfaceContainerLowest = Color(0xFF0A0920),
    surfaceContainerLow = Color(0xFF140E3D),
    surfaceContainer = Color(0xFF1A114F),
    surfaceContainerHigh = Color(0xFF251A63),
    surfaceContainerHighest = Color(0xFF2C2170),
    outline = Color(0xFF8A80C0),
    outlineVariant = Color(0xFF453A86),
    error = Color(0xFFF08A8A),
)

private val Light = lightColorScheme(
    primary = Color(0xFF5B4BB8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5DEFF),
    onPrimaryContainer = Color(0xFF1A0B5E),
    secondary = Color(0xFF5B4BB8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5DEFF),
    onSecondaryContainer = Color(0xFF1A0B5E),
    tertiary = Color(0xFF8A5A12),
    background = Color(0xFFFCF8FF),
    onBackground = Color(0xFF1B1730),
    surface = Color(0xFFFCF8FF),
    onSurface = Color(0xFF1B1730),
    surfaceVariant = Color(0xFFE6E0F5),
    onSurfaceVariant = Color(0xFF48435C),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F1FF),
    surfaceContainer = Color(0xFFF0EAFB),
    surfaceContainerHigh = Color(0xFFEAE4F6),
    surfaceContainerHighest = Color(0xFFE4DEF0),
    outline = Color(0xFF79728F),
    outlineVariant = Color(0xFFCAC3DC),
    error = Color(0xFFBA1A1A),
)

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

@Composable
fun SleepCyclesTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
```

- [ ] **Step 2: `AppIcons.kt`** (paths copied from Recetario's `Icons.kt`)

```kotlin
package com.david7ce.sleepcycles.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** The two Material icons the app uses, as path data (Material 3 no longer bundles icons). */
object AppIcons {
    private fun icon(name: String, path: String) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(addPathNodes(path), fill = SolidColor(Color.Black))
            .build()

    val Settings by lazy {
        icon(
            "Settings",
            "M19.14,12.94c0.04,-0.3 0.06,-0.61 0.06,-0.94c0,-0.32 -0.02,-0.64 -0.07,-0.94l2.03,-1.58c0.18,-0.14 0.23,-0.41 0.12,-0.61" +
                "l-1.92,-3.32c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 -1.03,-0.7 -1.62,-0.94L14.4,2.81c-0.04,-0.24 " +
                "-0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 -0.47,0.41L9.25,5.35C8.66,5.59 8.12,5.92 7.63,6.29L5.24,5.33c-0.22," +
                "-0.08 -0.47,0 -0.59,0.22L2.74,8.87C2.62,9.08 2.66,9.34 2.86,9.48l2.03,1.58C4.84,11.36 4.8,11.69 4.8,12s0.02,0.64 " +
                "0.07,0.94l-2.03,1.58c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32c0.12,0.22 0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 " +
                "1.03,0.7 1.62,0.94l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24,0 0.44,-0.17 0.47,-0.41l0.36,-2.54c0.59,-0.24 " +
                "1.13,-0.56 1.62,-0.94l2.39,0.96c0.22,0.08 0.47,0 0.59,-0.22l1.92,-3.32c0.12,-0.22 0.07,-0.47 -0.12,-0.61L19.14,12.94z" +
                "M12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6s1.62,-3.6 3.6,-3.6s3.6,1.62 3.6,3.6S13.98,15.6 12,15.6z",
        )
    }

    val Info by lazy {
        icon(
            "Info",
            "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM13,17h-2v-6h2v6zM13,9h-2V7h2v2z",
        )
    }
}
```

- [ ] **Step 3: `NightSky.kt`**

```kotlin
package com.david7ce.sleepcycles.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random

private class Star(val x: Float, val y: Float, val radius: Float, val alpha: Float)

/** Mountain ridge as (x, y) fractions of the canvas; the area below the line is filled. */
private val FarRidge = listOf(0f to 0.78f, 0.18f to 0.70f, 0.34f to 0.76f, 0.55f to 0.66f, 0.78f to 0.75f, 1f to 0.69f)
private val MidRidge = listOf(0f to 0.84f, 0.22f to 0.78f, 0.45f to 0.86f, 0.68f to 0.77f, 0.88f to 0.83f, 1f to 0.80f)
private val NearRidge = listOf(0f to 0.92f, 0.25f to 0.88f, 0.5f to 0.94f, 0.75f to 0.89f, 1f to 0.93f)

private fun DrawScope.ridge(points: List<Pair<Float, Float>>, color: Color) {
    val path = Path().apply {
        moveTo(0f, size.height)
        points.forEach { (x, y) -> lineTo(x * size.width, y * size.height) }
        lineTo(size.width, size.height)
        close()
    }
    drawPath(path, color)
}

/**
 * The PWA's night sky: gradient, stars and three mountain ridges, drawn (no images) so it costs nothing
 * to ship and scales to any window. [content] is laid out on top.
 */
@Composable
fun NightSky(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val stars = remember {
        val random = Random(7) // fixed seed: the same sky on every launch and recomposition
        List(70) { Star(random.nextFloat(), random.nextFloat() * 0.7f, 0.6f + random.nextFloat() * 1.4f, 0.4f + random.nextFloat() * 0.5f) }
    }
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.verticalGradient(
                    0f to Color(0xFF0A0920), 0.4f to Color(0xFF140E3D), 0.7f to Color(0xFF1A114F), 1f to Color(0xFF2C2170),
                )
            )
            stars.forEach {
                drawCircle(Color.White.copy(alpha = it.alpha), it.radius * density, Offset(it.x * size.width, it.y * size.height))
            }
            ridge(FarRidge, Color(0xFF453A86))
            ridge(MidRidge, Color(0xFF2B2166))
            ridge(NearRidge, Color(0xFF120C33))
        }
        content()
    }
}
```

- [ ] **Step 4: `RatingFace.kt`**

```kotlin
package com.david7ce.sleepcycles.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.david7ce.sleepcycles.logic.Rating

private fun ratingColor(rating: Rating, dark: Boolean): Color = when (rating) {
    Rating.Good -> if (dark) Color(0xFF7EE0A3) else Color(0xFF1E7B45)
    Rating.Ok -> if (dark) Color(0xFFF0CD7A) else Color(0xFF8A6200)
    Rating.Low -> if (dark) Color(0xFFF08A8A) else Color(0xFFB3261E)
}

/**
 * Smile, flat mouth or frown, drawn rather than emoji: colour emoji render badly on desktop JVM/Linux.
 * The shape (not just the colour) carries the meaning, and [description] is read by screen readers.
 */
@Composable
fun RatingFace(rating: Rating, description: String, dark: Boolean, modifier: Modifier = Modifier) {
    val color = ratingColor(rating, dark)
    Canvas(modifier.size(32.dp).semantics { contentDescription = description }) {
        val w = size.width
        val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        drawCircle(color, radius = w / 2 - stroke.width, style = stroke)
        drawCircle(color, radius = w * 0.06f, center = Offset(w * 0.36f, w * 0.40f))
        drawCircle(color, radius = w * 0.06f, center = Offset(w * 0.64f, w * 0.40f))
        when (rating) {
            Rating.Good -> drawArc(
                color, startAngle = 20f, sweepAngle = 140f, useCenter = false,
                topLeft = Offset(w * 0.30f, w * 0.40f), size = Size(w * 0.40f, w * 0.30f), style = stroke,
            )
            Rating.Ok -> drawLine(
                color, Offset(w * 0.35f, w * 0.68f), Offset(w * 0.65f, w * 0.68f),
                strokeWidth = stroke.width, cap = StrokeCap.Round,
            )
            Rating.Low -> drawArc(
                color, startAngle = 200f, sweepAngle = 140f, useCenter = false,
                topLeft = Offset(w * 0.30f, w * 0.58f), size = Size(w * 0.40f, w * 0.30f), style = stroke,
            )
        }
    }
}
```

- [ ] **Step 5: Commit** (compilation is checked by CI in Task 14)

```bash
git add android/SleepCycles/shared/src
git commit -m "SleepCycles: theme, night sky and rating faces

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 9: Screens and root composable

**Files:**
- Create: `ui/TimeField.kt`, `ui/MainScreen.kt`, `ui/SettingsScreen.kt`, `ui/HelpDialog.kt`
- Modify (replace stub): `shared/src/commonMain/kotlin/com/david7ce/sleepcycles/SleepCyclesApp.kt`

**Interfaces:**
- Consumes: everything from Tasks 3-8; `com.david7ce.shared.ui.{SegmentedChoice, ChoiceDropdown, SubScreen, SectionTitle, LocalSharedStrings, SharedStrings}`.
- Produces: `@Composable fun SleepCyclesApp(app: AppState)` (the single entry point both platforms call).

- [ ] **Step 1: `TimeField.kt`**

```kotlin
package com.david7ce.sleepcycles.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.david7ce.sleepcycles.logic.ClockFormat
import com.david7ce.sleepcycles.logic.parseTime
import com.david7ce.sleepcycles.logic.timeParts
import com.david7ce.shared.ui.SegmentedChoice

/**
 * Typed hh:mm input (like the PWA's), plus AM/PM in 12-hour format. A value is emitted only when the
 * text is a complete valid time; otherwise the fields are flagged and the last valid time stays.
 * Always left to right, also under an RTL layout direction.
 */
@Composable
fun TimeField(minutes: Int, format: ClockFormat, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val initial = timeParts(minutes, format)
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf(initial.minute) }
    var pm by remember { mutableStateOf(initial.pm) }
    var emitted by remember { mutableStateOf(minutes) }
    var shownFormat by remember { mutableStateOf(format) }

    // A change that did not come from typing here (Sleep now, mode or format switch) replaces the fields.
    // Own typing must not: it would turn a typed "1" into "01" under the cursor.
    LaunchedEffect(minutes, format) {
        if (minutes != emitted || format != shownFormat) {
            val p = timeParts(minutes, format)
            hour = p.hour
            minute = p.minute
            pm = p.pm
            emitted = minutes
            shownFormat = format
        }
    }

    fun commit() {
        parseTime(hour, minute, pm, format)?.let {
            emitted = it
            onChange(it)
        }
    }
    val valid = parseTime(hour, minute, pm, format) != null

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DigitField(hour, s.hourLabel, !valid, Modifier.weight(1f)) { hour = it; commit() }
            Text(":", style = MaterialTheme.typography.headlineMedium)
            DigitField(minute, s.minuteLabel, !valid, Modifier.weight(1f)) { minute = it; commit() }
            if (format == ClockFormat.H12) {
                SegmentedChoice(
                    options = listOf(false to s.am, true to s.pm),
                    selected = pm,
                    modifier = Modifier.width(132.dp),
                ) { pm = it; commit() }
            }
        }
    }
}

@Composable
private fun DigitField(value: String, label: String, isError: Boolean, modifier: Modifier, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValue(it.filter(Char::isDigit).take(2)) },
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}
```

- [ ] **Step 2: `MainScreen.kt`**

```kotlin
package com.david7ce.sleepcycles.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.david7ce.shared.ui.SegmentedChoice
import com.david7ce.sleepcycles.logic.Mode
import com.david7ce.sleepcycles.logic.Rating
import com.david7ce.sleepcycles.logic.Suggestion
import com.david7ce.sleepcycles.logic.formatHours
import com.david7ce.sleepcycles.logic.formatTime

@Composable
fun MainScreen(app: AppState) {
    val s = LocalStrings.current
    val dark = isDark(app.theme)
    val content: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 520.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TopRow(app)
                SegmentedChoice(
                    options = listOf(Mode.Bed to s.goToBedAt, Mode.Wake to s.wakeUpAt),
                    selected = app.mode,
                    onSelect = app::setMode,
                )
                TimeField(app.anchor, app.format, app::setAnchor)
                Button(onClick = app::sleepNow, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(s.sleepNow) }
                Text(
                    s.hint(app.latencyMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (app.mode == Mode.Wake) s.resultsWake else s.resultsBed,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (app.mode == Mode.Wake) WakeAccentFor(dark) else BedAccentFor(dark),
                )
                app.suggestions.forEach { SuggestionCard(it, app, dark) }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (dark) NightSky { content() }
    else Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
}

// The PWA's pale lavender is unreadable on a light background; light mode uses the theme's own colors.
@Composable
private fun WakeAccentFor(dark: Boolean): Color = if (dark) WakeAccent else MaterialTheme.colorScheme.tertiary

@Composable
private fun BedAccentFor(dark: Boolean): Color = if (dark) BedAccent else MaterialTheme.colorScheme.primary

@Composable
private fun TopRow(app: AppState) {
    val s = LocalStrings.current
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("SleepCycles", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = app::openHelp) { Icon(AppIcons.Info, contentDescription = s.howItWorks) }
        IconButton(onClick = app::openSettings) { Icon(AppIcons.Settings, contentDescription = s.settings) }
    }
}

@Composable
private fun SuggestionCard(item: Suggestion, app: AppState, dark: Boolean) {
    val s = LocalStrings.current
    val description = when (item.rating) {
        Rating.Good -> s.ratingGood
        Rating.Ok -> s.ratingOk
        Rating.Low -> s.ratingLow
    }
    val cardColor = if (dark) Color(0x1AFFFFFF) else MaterialTheme.colorScheme.surfaceContainer
    Row(
        Modifier
            .fillMaxWidth()
            .background(cardColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                formatTime(item.timeOfDay, app.format, s.am, s.pm),
                style = MaterialTheme.typography.headlineMedium,
                color = if (app.mode == Mode.Wake) WakeAccentFor(dark) else BedAccentFor(dark),
            )
            Text(
                s.cycles(item.cycles, formatHours(item.sleepMinutes, s.decimalSeparator)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RatingFace(item.rating, description, dark)
    }
}
```

- [ ] **Step 3: `SettingsScreen.kt`**

```kotlin
package com.david7ce.sleepcycles.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.david7ce.shared.ui.ChoiceDropdown
import com.david7ce.shared.ui.SectionTitle
import com.david7ce.shared.ui.SegmentedChoice
import com.david7ce.shared.ui.SubScreen
import com.david7ce.sleepcycles.logic.AgeRange
import com.david7ce.sleepcycles.logic.AppLanguage
import com.david7ce.sleepcycles.logic.ClockFormat
import com.david7ce.sleepcycles.logic.MAX_LATENCY_MINUTES
import com.david7ce.sleepcycles.logic.ThemeMode
import com.david7ce.sleepcycles.logic.parseLatency

/** Most-used first: age, time to fall asleep, time format, theme, language; version at the very bottom. */
@Composable
fun SettingsScreen(app: AppState) {
    val s = LocalStrings.current
    SubScreen(title = s.settings, onBack = app::back) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            SectionTitle(s.ageRange)
            ChoiceDropdown(
                options = AgeRange.entries.map { it to s.ageLabel(it) },
                selected = app.age,
                modifier = Modifier.fillMaxWidth(),
                onSelect = app::setAge,
            )

            SectionTitle(s.fallAsleepTime)
            LatencyField(app.latencyMinutes, app::setLatency)

            SectionTitle(s.timeFormat)
            SegmentedChoice(
                options = listOf(ClockFormat.H24 to s.format24, ClockFormat.H12 to s.format12),
                selected = app.format,
                onSelect = app::setFormat,
            )

            SectionTitle(s.theme)
            SegmentedChoice(
                options = listOf(
                    ThemeMode.System to s.themeSystem, ThemeMode.Light to s.themeLight, ThemeMode.Dark to s.themeDark,
                ),
                selected = app.theme,
                onSelect = app::setTheme,
            )

            SectionTitle(s.language)
            ChoiceDropdown(
                options = AppLanguage.entries.map { it to (if (it == AppLanguage.System) s.languageSystem else it.nativeName) },
                selected = app.language,
                modifier = Modifier.fillMaxWidth(),
                onSelect = app::setLanguage,
            )

            Spacer(Modifier.height(32.dp))
            Text(
                s.versionLabel(app.appVersion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Free text, applied as typed: empty or invalid counts as 0 and anything above the maximum is capped. */
@Composable
private fun LatencyField(minutes: Int, onChange: (Int) -> Unit) {
    var text by remember { mutableStateOf(minutes.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it.filter(Char::isDigit).take(3)
            onChange(parseLatency(text))
        },
        supportingText = { Text("0–$MAX_LATENCY_MINUTES") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
```

- [ ] **Step 4: `HelpDialog.kt`**

```kotlin
package com.david7ce.sleepcycles.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.howItWorks) },
        text = { Text(s.howItWorksBody, Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(s.gotIt) } },
    )
}
```

- [ ] **Step 5: Replace `SleepCyclesApp.kt`**

```kotlin
package com.david7ce.sleepcycles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.david7ce.shared.ui.LocalSharedStrings
import com.david7ce.shared.ui.SharedStrings
import com.david7ce.sleepcycles.ui.AppState
import com.david7ce.sleepcycles.ui.HelpDialog
import com.david7ce.sleepcycles.ui.LocalStrings
import com.david7ce.sleepcycles.ui.MainScreen
import com.david7ce.sleepcycles.ui.Screen
import com.david7ce.sleepcycles.ui.SettingsScreen
import com.david7ce.sleepcycles.ui.SleepCyclesTheme
import com.david7ce.sleepcycles.ui.isDark

/** Root of the shared UI. Each platform builds an [AppState], handles back, and calls this. */
@Composable
fun SleepCyclesApp(app: AppState) {
    val strings = app.strings
    CompositionLocalProvider(
        LocalStrings provides strings,
        // Text for the shared components (android/shared), in the app's chosen language.
        LocalSharedStrings provides SharedStrings(
            cancel = strings.cancel, back = strings.back, close = strings.close,
            delete = strings.delete, selectAll = strings.selectAll,
        ),
    ) {
        SleepCyclesTheme(isDark(app.theme)) {
            when (app.screen) {
                Screen.Main -> MainScreen(app)
                Screen.Settings -> SettingsScreen(app)
            }
            if (app.showHelp) HelpDialog(onDismiss = app::closeHelp)
        }
    }
}
```

- [ ] **Step 6: Keep the platform stubs compiling with the new signature**

The Task 2 stubs call `SleepCyclesApp()` with no argument, which no longer exists. Make each commit build:
in `androidMain/.../MainActivity.kt` change `setContent { SleepCyclesApp() }` to `setContent { }` and drop the
now-unused `SleepCyclesApp` import; in `desktopMain/.../DesktopMain.kt` change
`{ SleepCyclesApp() }` to `{ }` and drop that import too. Tasks 10 and 11 replace both files for good.

- [ ] **Step 7: Commit**

```bash
git add android/SleepCycles/shared/src
git commit -m "SleepCycles: main screen, settings, help dialog and root composable

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 10: Android platform

**Files:**
- Create: `shared/src/jvmCommonMain/kotlin/com/david7ce/sleepcycles/platform/LocalClock.kt` (delete `Stub.kt`)
- Create: `shared/src/androidMain/kotlin/com/david7ce/sleepcycles/platform/PreferencesSettingsStore.kt`
- Replace: `SleepCyclesApplication.kt`, `MainActivity.kt` in the same folder

**Interfaces:**
- Consumes: `Clock`, `SettingsStore`, `Services`, `AppState` (Tasks 5-6), `SleepCyclesApp`, `isDark` (Tasks 8-9).
- Produces: `object LocalClock : Clock`; `class PreferencesSettingsStore(prefs: SharedPreferences)`; `class SleepCyclesApplication { val state: AppState }`; `MainActivity.ACTION_SLEEP_NOW = "com.david7ce.sleepcycles.SLEEP_NOW"`.

- [ ] **Step 1: `LocalClock.kt`; remove the stub**

```kotlin
package com.david7ce.sleepcycles.platform

import java.time.LocalTime

/** The device's wall clock; shared by Android and desktop. */
object LocalClock : Clock {
    override fun minutesOfDay(): Int = LocalTime.now().let { it.hour * 60 + it.minute }
}
```
```bash
git rm android/SleepCycles/shared/src/jvmCommonMain/kotlin/com/david7ce/sleepcycles/platform/Stub.kt
```

- [ ] **Step 2: `PreferencesSettingsStore.kt`**

```kotlin
package com.david7ce.sleepcycles.platform

import android.content.SharedPreferences

class PreferencesSettingsStore(private val prefs: SharedPreferences) : SettingsStore {
    override fun get(key: String): String? = prefs.getString(key, null)
    override fun put(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
}
```

- [ ] **Step 3: `SleepCyclesApplication.kt`**

```kotlin
package com.david7ce.sleepcycles.platform

import android.app.Application
import com.david7ce.sleepcycles.ui.AppState
import java.util.Locale

/** Builds the state once per process; it outlives activity recreation (rotation, language change). */
class SleepCyclesApplication : Application() {
    val state: AppState by lazy {
        val services = Services(
            settings = PreferencesSettingsStore(getSharedPreferences("settings", MODE_PRIVATE)),
            clock = LocalClock,
        )
        val version = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        AppState(services, Locale.getDefault().language, version)
    }
}
```

- [ ] **Step 4: `MainActivity.kt`**

```kotlin
package com.david7ce.sleepcycles.platform

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import com.david7ce.sleepcycles.SleepCyclesApp
import com.david7ce.sleepcycles.ui.isDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = (application as SleepCyclesApplication).state
        app.systemLanguage = resources.configuration.locales[0].language
        // The launcher shortcut. Only on a fresh start: a recreation (rotation) must not repeat it.
        if (savedInstanceState == null && intent?.action == ACTION_SLEEP_NOW) app.sleepNow()
        enableEdgeToEdge()
        setContent {
            // System bar icons follow the app theme, which can differ from the system one.
            val dark = isDark(app.theme)
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
                )
                onDispose { }
            }
            BackHandler(enabled = app.canGoBack) { app.back() }
            SleepCyclesApp(app)
        }
    }

    companion object {
        const val ACTION_SLEEP_NOW = "com.david7ce.sleepcycles.SLEEP_NOW"
        private val LIGHT_SCRIM = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        private val DARK_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add android/SleepCycles/shared/src
git commit -m "SleepCycles: Android platform (preferences, application, activity, Sleep now shortcut)

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 11: Desktop platform

**Files:**
- Create: `shared/src/desktopMain/kotlin/com/david7ce/sleepcycles/platform/JavaPreferencesStore.kt`
- Create: `shared/src/desktopMain/kotlin/com/david7ce/sleepcycles/platform/WindowsTitleBar.kt`
- Replace: `shared/src/desktopMain/kotlin/com/david7ce/sleepcycles/platform/DesktopMain.kt`

**Interfaces:**
- Consumes: `SettingsStore`, `Services`, `LocalClock`, `AppState`, `SleepCyclesApp`, `isDark`; generated `DESKTOP_VERSION` (Task 2).
- Produces: `fun main()` (the `mainClass` Gradle packages), `applyWindowsTitleBarTheme(windowTitle, dark)`.

- [ ] **Step 1: `JavaPreferencesStore.kt`**

```kotlin
package com.david7ce.sleepcycles.platform

import java.util.prefs.Preferences

/** Registry on Windows, ~/.java on Linux, plist on macOS: whatever the OS considers "user settings". */
class JavaPreferencesStore(
    private val node: Preferences = Preferences.userRoot().node("com/david7ce/sleepcycles"),
) : SettingsStore {
    override fun get(key: String): String? = node.get(key, null)
    override fun put(key: String, value: String) {
        node.put(key, value)
        runCatching { node.flush() } // an unwritable backing store must never crash the app
    }
}
```

- [ ] **Step 2: `WindowsTitleBar.kt`**: copy `android/Recetario/shared/src/desktopMain/kotlin/com/david7ce/recetario/platform/WindowsTitleBar.kt` verbatim, changing only the `package` line to `com.david7ce.sleepcycles.platform`

```bash
cd /c/Users/tener/Workspaces/repos/pkg-lab/android
sed 's/^package com.david7ce.recetario.platform/package com.david7ce.sleepcycles.platform/' \
  Recetario/shared/src/desktopMain/kotlin/com/david7ce/recetario/platform/WindowsTitleBar.kt \
  > SleepCycles/shared/src/desktopMain/kotlin/com/david7ce/sleepcycles/platform/WindowsTitleBar.kt
head -3 SleepCycles/shared/src/desktopMain/kotlin/com/david7ce/sleepcycles/platform/WindowsTitleBar.kt
```
Expected first line: `package com.david7ce.sleepcycles.platform`.

- [ ] **Step 3: Replace `DesktopMain.kt`**

```kotlin
package com.david7ce.sleepcycles.platform

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.david7ce.sleepcycles.SleepCyclesApp
import com.david7ce.sleepcycles.ui.AppState
import com.david7ce.sleepcycles.ui.isDark
import java.util.Locale

private const val WINDOW_TITLE = "SleepCycles"

fun main() {
    val state = AppState(
        services = Services(settings = JavaPreferencesStore(), clock = LocalClock),
        systemLanguage = Locale.getDefault().language,
        appVersion = DESKTOP_VERSION,
    )
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = WINDOW_TITLE,
            state = rememberWindowState(size = DpSize(480.dp, 820.dp)),
        ) {
            val dark = isDark(state.theme)
            LaunchedEffect(dark) { applyWindowsTitleBarTheme(WINDOW_TITLE, dark) }
            SleepCyclesApp(state)
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add android/SleepCycles/shared/src
git commit -m "SleepCycles: desktop platform (window, java.util.prefs, Windows title bar)

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 12: CI for SleepCycles (Android release naming + desktop matrix)

**Files:**
- Modify: `.github/workflows/android.yml`
- Rewrite: `.github/workflows/desktop.yml`
- Modify: `scripts/build-and-test.ps1`

**Interfaces:**
- Produces: release tag `sleepcycles-latest` (Android APKs + desktop installers), per-app desktop matrix.

- [ ] **Step 1: `android.yml`: header comment**

Replace the sentence `Recetario shares that release with desktop.yml's installers (see "Publish to Releases" below)` with `Recetario and SleepCycles share their release ("<app>-latest", lower case) with desktop.yml's installers (see "Publish to Releases" below)`.

- [ ] **Step 2: `android.yml`: tag selection**

Replace
```bash
          # Recetario ships Android + desktop (Windows/Linux/macOS) on one release, published by
          # both this workflow and desktop.yml - never destructively, since either can run first
          # and neither should delete what the other already uploaded.
          if [ "$APP" = "Recetario" ]; then TAG="recetario-latest"; else TAG="android-${APP}-latest"; fi
```
with
```bash
          # Apps that also ship desktop installers (Windows/Linux/macOS) keep Android and desktop on one
          # release, published by both this workflow and desktop.yml - never destructively, since either
          # can run first and neither should delete what the other already uploaded.
          # Keep this list in sync with desktop.yml's detect job.
          MULTI=" Recetario SleepCycles "
          IS_MULTI=false
          if [[ "$MULTI" == *" $APP "* ]]; then IS_MULTI=true; TAG="${APP,,}-latest"; else TAG="android-${APP}-latest"; fi
```

- [ ] **Step 3: `android.yml`: release notes**

Replace
```bash
            if [ "$APP" = "Recetario" ]; then
              echo "- **.msi**: Windows (64-bit)."
              echo "- **.tar.gz**: Linux (64-bit). Unpack and run \`Recetario/bin/Recetario\`. Includes Java, nothing to install."
              echo "- **.dmg**: macOS (Apple Silicon)."
            fi
```
with
```bash
            if [ "$IS_MULTI" = true ]; then
              echo "- **.msi**: Windows (64-bit)."
              echo "- **.tar.gz**: Linux (64-bit). Unpack and run \`${APP}/bin/${APP}\`. Includes Java, nothing to install."
              echo "- **.dmg**: macOS (Apple Silicon)."
            fi
```
and in the comment above `publish()` replace `(Recetario's desktop installers, published independently by desktop.yml)` with `(the desktop installers of Recetario/SleepCycles, published independently by desktop.yml)`, and `# lost the race: desktop.yml (Recetario) created it first` with `# lost the race: desktop.yml created it first`.

- [ ] **Step 4: Rewrite `desktop.yml`**

```yaml
# Desktop installers for the apps that have a desktop target (Recetario, SleepCycles). Only the
# minimum, each format on the system that produces it:
#   Windows x64  → .msi
#   Linux x64    → tar.gz with the app and its bundled Java (nothing to install, any x64 distro)
#   macOS arm64  → .dmg (arm runner)
# Deliberately left out: Linux arm64, iOS (needs an Apple Developer account to install on an iPhone).
# Each app publishes to its own "<app, lower case>-latest" release, shared with android.yml's APKs.
name: Desktop

on:
  push:
    branches: [main]
    paths:
      - "android/Recetario/**"
      - "android/SleepCycles/**"
      - "android/shared/**"
      - ".github/workflows/desktop.yml"
  workflow_dispatch:
    inputs:
      app:
        description: "App folder inside android/ (empty = every app with a desktop target)"
        required: false
        default: ""

permissions:
  contents: write

jobs:
  detect:
    runs-on: ubuntu-latest
    outputs:
      apps: ${{ steps.list.outputs.apps }}
    steps:
      - uses: actions/checkout@v5
        with:
          fetch-depth: 0

      - id: list
        env:
          EVENT: ${{ github.event_name }}
          INPUT_APP: ${{ github.event.inputs.app }}
          BEFORE: ${{ github.event.before }}
        run: |
          # Keep this list in sync with android.yml's MULTI.
          ALL="Recetario SleepCycles"

          if [ "$EVENT" = "workflow_dispatch" ]; then
            if [ -n "$INPUT_APP" ]; then apps="$INPUT_APP"; else apps="$ALL"; fi
          elif [ -z "$BEFORE" ] || [ "$BEFORE" = "0000000000000000000000000000000000000000" ] \
               || ! git cat-file -e "$BEFORE^{commit}" 2>/dev/null; then
            apps="$ALL"   # first push, or rewritten history
          else
            changed=$(git diff --name-only "$BEFORE" HEAD)
            if echo "$changed" | grep -qE '^(\.github/workflows/desktop\.yml|android/shared/)'; then
              apps="$ALL"
            else
              apps=$(echo "$changed" | grep '^android/' | cut -d/ -f2 | sort -u)
            fi
          fi

          # Only apps from the list above that exist
          json=$(for a in $apps; do
                   case " $ALL " in *" $a "*) [ -f "android/$a/gradlew" ] && echo "$a";; esac
                 done | jq -R . | jq -sc .)
          echo "Desktop apps: $json"
          echo "apps=$json" >> "$GITHUB_OUTPUT"

  build:
    needs: detect
    if: needs.detect.outputs.apps != '[]'
    strategy:
      fail-fast: false
      matrix:
        app: ${{ fromJson(needs.detect.outputs.apps) }}
        os: [windows-latest, ubuntu-latest, macos-14]
        include:
          - os: windows-latest
            task: packageMsi
            glob: "*.msi"
          - os: ubuntu-latest
            task: createDistributable
            glob: "*-linux-x64.tar.gz"
          - os: macos-14
            task: packageDmg
            glob: "*.dmg"
    runs-on: ${{ matrix.os }}
    name: build (${{ matrix.app }}, ${{ matrix.os }})
    defaults:
      run:
        shell: bash
        working-directory: android/${{ matrix.app }}
    steps:
      - uses: actions/checkout@v5

      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: 17

      - uses: gradle/actions/setup-gradle@v5

      - name: Desktop tests
        run: |
          chmod +x gradlew
          ./gradlew :shared:desktopTest --no-daemon

      - name: Installer (${{ matrix.task }})
        run: ./gradlew :shared:${{ matrix.task }} --no-daemon

      # The plugin leaves each format in its own subfolder; find it and copy it to a fixed folder.
      - name: Locate installer
        env:
          APP: ${{ matrix.app }}
        run: |
          mkdir -p "$RUNNER_TEMP/installers"
          if [ "$RUNNER_OS" = "Linux" ]; then
            # createDistributable leaves app/<Name>/bin/<Name>; the whole folder gets packaged.
            bin=$(find shared/build/compose -type f -path "*/bin/$APP" | head -1)
            [ -n "$bin" ] || { echo "::error::The Linux app wasn't generated"; find shared/build/compose -maxdepth 4 | head -50; exit 1; }
            appdir=$(dirname "$(dirname "$bin")")
            tar -czf "$RUNNER_TEMP/installers/$APP-linux-x64.tar.gz" -C "$(dirname "$appdir")" "$(basename "$appdir")"
            ls -lh "$RUNNER_TEMP/installers/"
          else
            found=$(find shared/build/compose -type f -name '${{ matrix.glob }}' | head -1)
            echo "::notice title=Installer::${found:-not found}"
            [ -n "$found" ] || { echo "::error::${{ matrix.glob }} wasn't generated"; find shared/build/compose -maxdepth 4 | head -50; exit 1; }
            cp "$found" "$RUNNER_TEMP/installers/"
          fi

      - name: Save installer
        uses: actions/upload-artifact@v5
        with:
          name: desktop-${{ matrix.app }}-${{ matrix.os }}
          path: ${{ runner.temp }}/installers/${{ matrix.glob }}
          if-no-files-found: error

  release:
    needs: [detect, build]
    if: ${{ !cancelled() && needs.detect.outputs.apps != '[]' }}
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        app: ${{ fromJson(needs.detect.outputs.apps) }}
    steps:
      - uses: actions/checkout@v5
        with:
          fetch-depth: 0 # full history, so the commit log below can reach an old release's commit

      - uses: actions/download-artifact@v5
        with:
          pattern: desktop-${{ matrix.app }}-*
          merge-multiple: true
          path: dist

      - name: Publish to Releases
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          GH_REPO: ${{ github.repository }}
          APP: ${{ matrix.app }}
        run: |
          set -o pipefail
          files=$(find dist -type f \( -name '*.msi' -o -name '*.tar.gz' -o -name '*.dmg' \))
          echo "::notice title=Installers::$(echo $files | tr '\n' ' ')"
          [ -n "$files" ] || { echo "::error::No installers to publish"; exit 1; }

          # Same release as android.yml's build for this app (Android + desktop together) - never a
          # destructive delete, since android.yml's own APKs may already be on it, or may land
          # after this runs. This workflow doesn't know Android's versionName, so it never sets
          # the release title itself except as a fallback on a brand new release.
          TAG="${APP,,}-latest"

          # Commits since the last desktop release (none on a first-ever release).
          prev=$(gh release view "$TAG" --json targetCommitish -q .targetCommitish 2>/dev/null || true)
          log=""
          if [ -n "$prev" ] && git cat-file -e "$prev^{commit}" 2>/dev/null; then
            log=$(git log "$prev..$GITHUB_SHA" --oneline --no-decorate -- "android/$APP" android/shared 2>/dev/null)
          fi

          {
            echo "Built automatically from ${GITHUB_SHA::7}."
            echo
            echo "- **…-arm64-v8a.apk / …-armeabi-v7a.apk / …-universal.apk**: Android."
            echo "- **.msi**: Windows (64-bit)."
            echo "- **.tar.gz**: Linux (64-bit). Unpack and run \`${APP}/bin/${APP}\`. Includes Java, nothing to install."
            echo "- **.dmg**: macOS (Apple Silicon)."
            if [ -n "$log" ]; then
              echo
              echo "Changes:"
              echo "$log" | sed 's/^/- /'
            fi
          } > "$RUNNER_TEMP/notes.md"

          # Adds/replaces the installers without touching the Android APKs already on the same
          # release - deletes only this app's own old-versioned installers.
          publish() {
            new_names=$(basename -a $files)
            gh release view "$TAG" --json assets -q '.assets[].name' 2>/dev/null \
              | grep -E '\.(msi|dmg)$|\.tar\.gz$' | while IFS= read -r old; do
                  echo "$new_names" | grep -qx "$old" || gh release delete-asset "$TAG" "$old" --yes
                done
            gh release upload "$TAG" $files --clobber
            gh release edit "$TAG" --target "$GITHUB_SHA" --notes-file "$RUNNER_TEMP/notes.md"
          }

          if gh release view "$TAG" >/dev/null 2>&1; then
            publish
          elif gh release create "$TAG" $files \
                --target "$GITHUB_SHA" --title "$APP" --notes-file "$RUNNER_TEMP/notes.md" 2>"$RUNNER_TEMP/create.err"; then
            : # created fresh (android.yml will set the real "<App> <version>" title if it runs too)
          elif grep -qi "already exists" "$RUNNER_TEMP/create.err"; then
            publish # lost the race: android.yml created it first
          else
            cat "$RUNNER_TEMP/create.err" >&2
            exit 1
          fi
```

- [ ] **Step 5: Syntax-check both workflows**

```bash
cd /c/Users/tener/Workspaces/repos/pkg-lab
python -c "import yaml,sys; [yaml.safe_load(open(f)) for f in ['.github/workflows/android.yml','.github/workflows/desktop.yml']]; print('yaml ok')"
```
Expected: `yaml ok`. (If Python/PyYAML is missing: `pwsh -c "Get-Content .github/workflows/desktop.yml | Out-Null"` is not a parse; install PyYAML or check with a YAML-aware editor.) Also run `bash -n` on each `run:` block extracted by hand for the two changed shell snippets (the `MULTI` test and the `detect` case statement).

- [ ] **Step 6: `scripts/build-and-test.ps1`**

Replace the header usage lines for `-Desktop`/`-Package`:
```
  .\scripts\build-and-test.ps1 -Desktop            # also run :shared:desktopTest for every desktop app (Recetario, SleepCycles)
  .\scripts\build-and-test.ps1 -Package            # also build the Windows .msi for those apps
  .\scripts\build-and-test.ps1 -Desktop -App SleepCycles   # just one
```
and replace the final `if ($Desktop -or $Package) { ... }` block with
```powershell
# Apps under android/ that have a desktop target (jvm("desktop") in their shared module).
$desktopApps = @("Recetario", "SleepCycles")

if ($Desktop -or $Package) {
    $targets = if ($App) { $desktopApps | Where-Object { $_ -eq $App } } else { $desktopApps }
    if (-not $targets) { throw "-Desktop/-Package: '$App' has no desktop target (have: $($desktopApps -join ', '))" }
    foreach ($t in $targets) {
        Push-Location (Join-Path $PSScriptRoot "..\android\$t")
        try {
            if ($Desktop) {
                Write-Host "`n=== $t : desktopTest ===" -ForegroundColor Cyan
                if ($daemonFlag) { ./gradlew.bat :shared:desktopTest $daemonFlag } else { ./gradlew.bat :shared:desktopTest }
            }
            if ($Package) {
                Write-Host "`n=== $t : packageMsi (Windows installer) ===" -ForegroundColor Cyan
                if ($daemonFlag) { ./gradlew.bat :shared:packageMsi $daemonFlag } else { ./gradlew.bat :shared:packageMsi }
            }
        } finally { Pop-Location }
    }
}
```
Also add `SleepCycles` to the local-run note in the README (Task 13).

- [ ] **Step 7: Commit**

```bash
git add .github/workflows scripts
git commit -m "CI: SleepCycles releases (Android + desktop) and per-app desktop matrix

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 13: Documentation

**Files:**
- Create: `android/SleepCycles/README.md`
- Modify: `README.md`, `ROADMAP.md`, `android/shared/README.md`, `docs/superpowers/specs/2026-10-13-sleepcycles-design.md`

- [ ] **Step 1: `android/SleepCycles/README.md`**

```markdown
# SleepCycles

Sleep cycle calculator: pick when you want to wake up or go to bed and get six times aligned to 90-minute
cycles, each rated against the recommended sleep for your age range. Native port of the PWA
[sleep-cycles-calc](https://github.com/David7ce/sleep-cycles-calc). Kotlin Multiplatform + Compose
Multiplatform: Android, Windows, Linux and macOS. Spanish, English, French, German, Portuguese and Italian
(device language by default, or chosen in Settings).

- Two modes: *go to bed at* (gives wake-up times) and *wake up at* (gives bedtimes).
- *Sleep now* (also a launcher shortcut on Android): bed mode at the current time.
- Six suggestions (6 to 1 cycles). Each cycle is 90 min; your time to fall asleep (0–120 min, default 15) is added.
- Rating by age range, shown as a face and its description: child 9–11 h, teen 8–10 h, adult 7–9 h,
  senior 7–8 h; "ok" within one cycle of the range and at 4.5 h.
- 24-hour or 12-hour (AM/PM), light/dark/system theme (dark draws the PWA's night sky). No accounts,
  no network, no permissions; settings stay on the device.

## Structure

```
SleepCycles/
├── app/        Android packaging only: id, version, signing, ABI splits, icon, manifest, shortcut
└── shared/     Kotlin Multiplatform library (androidTarget + jvm("desktop"))
    └── src/
        ├── commonMain/   logic/ (calculation, time, settings: pure) · platform/ (interfaces) · ui/
        ├── jvmCommonMain/ clock shared by Android and desktop
        ├── androidMain/  SharedPreferences store, Application, MainActivity
        ├── desktopMain/  java.util.prefs store, window, Windows title bar
        └── commonTest/   logic, state and strings tests (run on the Android unit-test task and on desktopTest)
```

Dependency direction: `ui → logic`; `platform` interfaces are implemented per target.

## Run and test

- Android + unit tests: `.\scripts\build-and-test.ps1 -App SleepCycles` (needs the Android SDK).
- Desktop: `./gradlew.bat :shared:run`, tests with `./gradlew.bat :shared:desktopTest`, installer with `:shared:packageMsi`.
- Downloads: *Releases → `sleepcycles-latest`* (APKs, `.msi`, `.tar.gz`, `.dmg`).

## Not tested on real hardware

Verified by CI (unit tests and builds). Not exercised on a physical Android device or on macOS and Linux desktops.
```

- [ ] **Step 2: `README.md` (root)**

- In the tree add `│   ├── SleepCycles/       ← sleep cycle calculator (Kotlin Multiplatform: Android + Windows/Linux/macOS)` under `android/` and extend the `shared/` comment is unchanged.
- In *Downloading* change `(Recetario: \`recetario-latest\`, see below)` to `(Recetario: \`recetario-latest\`, SleepCycles: \`sleepcycles-latest\`, see below)`.
- In *Release titles* change `(only Recetario today)` to `(Recetario and SleepCycles)` and `Recetario's` mentions of `recetario-latest` to `\`recetario-latest\` / \`sleepcycles-latest\``.
- In *Local development loop*, `-Desktop`/`-Package` now cover every desktop app and accept `-App`.
- *Multiplatform apps* bullet: `(see Recetario)` → `(see Recetario, SleepCycles)`.

- [ ] **Step 3: `ROADMAP.md`**

- Under *Open items* add:
  ```markdown
  - **Watch the first CI runs of SleepCycles.** Written without an Android SDK or any local build (this
    machine has none), so the first `android.yml` and `desktop.yml` runs are its first compile. Also
    watch the new per-app `desktop.yml` matrix and `sleepcycles-latest` end to end, and check the app
    once on a real phone (Sleep now shortcut, 12-hour input, language switch).
  ```
- In *Reference → Releases* change `Recetario's is \`recetario-latest\`, shared by Android and desktop` to `Recetario's is \`recetario-latest\` and SleepCycles' is \`sleepcycles-latest\`, each shared by Android and desktop`.
- In *Not worth doing* change `three Gradle projects` to `four Gradle projects` and `Revisit only if a 4th app needs it` to `Revisit only if a 5th app needs it`.

- [ ] **Step 4: `android/shared/README.md`**: change `Used by VozReves, FuelLog and Recetario.` to `Used by VozReves, FuelLog, Recetario and SleepCycles.`

- [ ] **Step 5: Align the spec with what was built**

In `docs/superpowers/specs/2026-10-13-sleepcycles-design.md`:
- Replace `(change recreates the activity on Android)` with `(applied live: texts are Compose state, no activity recreation, as in Recetario)`.
- In *Logic*, replace `- Latency: minutes to fall asleep; invalid or negative input counts as 0.` with `- Latency: minutes to fall asleep, 0..120 (the PWA input's range); empty, invalid or negative counts as 0, above 120 is capped.`
- In *UI → Main*, after the sentence ending `rating face.`, add: `Results are live: there is no Calculate button, the cards update as the time changes.`

- [ ] **Step 6: Commit**

```bash
git add README.md ROADMAP.md android docs
git commit -m "Docs: SleepCycles README, root README/ROADMAP, spec aligned with the build

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 14: Verify in CI and hand over

**Files:** none (no secrets are touched).

This machine cannot build the project, so this task is the only real verification. It needs the owner: pushing and opening Actions are outward-facing steps.

- [ ] **Step 1: Ask the owner to push the branch** (never push without their ok)

```bash
git push -u origin feat/sleepcycles
```

- [ ] **Step 2: Run the workflows on the branch**

In GitHub: *Actions → Android → Run workflow*, branch `feat/sleepcycles`, app `SleepCycles`. Then *Actions → Desktop → Run workflow*, same branch, app `SleepCycles`. Do the same once with `Recetario` in Desktop, to prove the generalized matrix did not break it.

Expected:
- Android: three APK builds green, "Unit tests" notice shows the SleepCycles tests (29: SleepCalc 6, TimeFormat 8, UserSettings 4, AppState 6, Strings 4, Smoke 1) with 0 failures.
- Desktop: `desktopTest` green on three OSes; `.msi`, `SleepCycles-linux-x64.tar.gz`, `.dmg` uploaded.
- Release `sleepcycles-latest` contains the APKs and the three installers (workflow runs from a non-main branch also publish to Releases: say so to the owner before running, or run from `main` after merge instead).

- [ ] **Step 3: Fix what CI reports**

Compile errors appear as annotations on the run. Likeliest places, from the APIs used: `Modifier.statusBarsPadding()`/`navigationBarsPadding()` (used by Recetario, fine), `OutlinedTextField`'s `supportingText`, `SegmentedChoice` with `Boolean` as `T`, `drawArc` parameter names, `@Composable` lambda stored in a `val` in `MainScreen` (`content`). Fix, commit, re-run.

- [ ] **Step 4: Report faithfully**

Final status to the owner states what CI verified (tests, builds) and what nobody has verified: behavior on a real Android device, on macOS and on Linux, and the visual quality of the night sky, faces and 12-hour field.
