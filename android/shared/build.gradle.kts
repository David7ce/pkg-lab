// Shared module for pkg-lab's Kotlin apps. Multiplatform: commonMain is plain Compose,
// with no Android resources or icons; text is passed in from each app (LocalSharedStrings).
// Used by VozReves, FuelLog and Recetario. The Compose versions are Recetario's (CMP 1.11.1),
// so apps that include this must be on Kotlin 2.4.21 and AGP 8.13.x (see README).
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
            api("org.jetbrains.compose.runtime:runtime:1.11.1")
            api("org.jetbrains.compose.foundation:foundation:1.11.1")
            api("org.jetbrains.compose.ui:ui:1.11.1")
            api("org.jetbrains.compose.material3:material3:1.9.0")
        }
    }
}

android {
    namespace = "com.david7ce.shared"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
}
