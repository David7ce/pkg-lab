// Shared module for pkg-lab's Kotlin apps. Multiplatform: commonMain is plain Compose,
// with no Android resources or icons; text is passed in from each app (LocalSharedStrings).
// Used by VozReves, FuelLog and Recetario, which all resolve this module's Compose/Kotlin/AGP
// versions from the single catalog at android/gradle/libs.versions.toml.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }
    // Desktop (Windows and Linux): same Compose UI, no Android resources.
    jvm("desktop")
    sourceSets {
        commonMain.dependencies {
            // "api": apps that include this module use Compose directly.
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.ui)
            api(libs.compose.material3)
        }
    }
}

android {
    namespace = "com.david7ce.shared"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
}
