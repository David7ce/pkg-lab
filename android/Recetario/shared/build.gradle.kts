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
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.sqldelight.sqlite.driver)
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
            }
        }
    }
}

android {
    namespace = "com.deivon.recetario.shared"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("RecipeDatabase") {
            packageName.set("com.deivon.recetario.db")
        }
    }
}

// Desktop app: packageMsi (Windows), packageAppImage (Linux), packageDmg (macOS). See .github/workflows/desktop.yml.
compose.desktop {
    application {
        mainClass = "com.deivon.recetario.platform.DesktopMainKt"
        nativeDistributions {
            // Solo lo que se publica: .msi (Windows), AppImage (Linux), .dmg (macOS). Cada uno se genera en su sistema.
            targetFormats(TargetFormat.Msi, TargetFormat.AppImage, TargetFormat.Dmg)
            packageName = "Recetario"
            // Installers need MAJOR.MINOR.PATCH with a number in MAJOR; CI run number keeps each build newer.
            packageVersion = "1.0." + (System.getenv("GITHUB_RUN_NUMBER") ?: "1")
            vendor = "deivon"
            description = "Recetario: recetario personal"
            windows {
                menuGroup = "Recetario"
                upgradeUuid = "6f1c2a9e-4b7d-4e8a-9c3f-2d5b8a1e7f40"
            }
        }
    }
}
