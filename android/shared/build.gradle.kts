// Módulo compartido de las apps Kotlin de pkg-lab. Multiplataforma: commonMain es Compose puro,
// sin recursos ni iconos de Android; el texto se pasa desde cada app (LocalSharedStrings).
// Lo usan VozReves, FuelLog y Recetario. Las versiones de Compose son las de Recetario (CMP 1.11.1),
// así que las apps que lo incluyen deben estar en Kotlin 2.4.21 y AGP 8.13.x (ver README).
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }
    // Escritorio (Windows y Linux): misma UI Compose, sin recursos de Android.
    jvm("desktop")
    sourceSets {
        commonMain.dependencies {
            // "api": las apps que incluyen este módulo usan Compose directamente.
            api("org.jetbrains.compose.runtime:runtime:1.11.1")
            api("org.jetbrains.compose.foundation:foundation:1.11.1")
            api("org.jetbrains.compose.ui:ui:1.11.1")
            api("org.jetbrains.compose.material3:material3:1.9.0")
        }
    }
}

android {
    namespace = "com.deivon.shared"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
}
