import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Shared code: model, logic, persistence and the Compose UI live in commonMain.
// Android-only pieces (SQLite driver, files, images, network, PDF, Activity) live in androidMain.
// Desktop (Windows/Linux): jvm("desktop") below. JVM code shared by Android and desktop
// (zip, HTTP, file storage) lives in jvmCommonMain.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

// Single source of truth for the desktop installer version, also baked into the running app
// below (generateDesktopVersion) so it can compare itself against GitHub Releases.
val appVersion = "1.0." + (System.getenv("GITHUB_RUN_NUMBER") ?: "1")

// Desktop has no BuildConfig/PackageInfo equivalent; generate a one-constant Kotlin file instead.
val generateDesktopVersion = tasks.register("generateDesktopVersion") {
    val outputDir = layout.buildDirectory.dir("generated/desktopVersion/kotlin")
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("com/david7ce/recetario/platform/DesktopVersion.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            "package com.david7ce.recetario.platform\n\ninternal const val DESKTOP_VERSION = \"$appVersion\"\n"
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
                implementation(libs.sqldelight.sqlite.driver)
                implementation(libs.jna.platform) // Windows title bar dark/light (DwmSetWindowAttribute)
            }
        }

        commonMain.dependencies {
            implementation(project(":pkgshared"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.sqldelight.runtime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.androidx.activity.compose)
        }
        val androidUnitTest by getting {
            dependencies {
                // Real SQLite on the JVM, so repository and backup tests run against the actual schema.
                implementation(libs.sqldelight.sqlite.driver)
                // Compose smoke test on the JVM (no emulator): Robolectric hosts it, ui-test-junit4
                // drives it, ui-test-manifest supplies the host activity createComposeRule() needs.
                implementation("androidx.compose.ui:ui-test-junit4:1.7.8")
                implementation("androidx.compose.ui:ui-test-manifest:1.7.8")
                implementation("androidx.test.ext:junit:1.2.1")
                implementation("org.robolectric:robolectric:4.16")
            }
        }
    }
}

android {
    namespace = "com.david7ce.recetario.shared"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    testOptions {
        unitTests.isIncludeAndroidResources = true // Robolectric needs real resources, not stubs
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("RecipeDatabase") {
            packageName.set("com.david7ce.recetario.db")
        }
    }
}

// Desktop app: packageMsi (Windows), packageDmg (macOS), createDistributable (Linux). See .github/workflows/desktop.yml.
compose.desktop {
    application {
        mainClass = "com.david7ce.recetario.platform.DesktopMainKt"
        nativeDistributions {
            // Only what gets published: .msi (Windows) and .dmg (macOS). Linux uses createDistributable + tar.gz in CI.
            targetFormats(TargetFormat.Msi, TargetFormat.Dmg)
            packageName = "Recetario"
            // Installers need MAJOR.MINOR.PATCH with a number in MAJOR; CI run number keeps each build newer.
            packageVersion = appVersion
            vendor = "deivon"
            description = "Recetario: recetario personal"
            windows {
                menuGroup = "Recetario"
                upgradeUuid = "6f1c2a9e-4b7d-4e8a-9c3f-2d5b8a1e7f40"
            }
        }
    }
}
