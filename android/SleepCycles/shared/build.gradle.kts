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
