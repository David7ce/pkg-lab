import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

val targetAbi: String? = (findProperty("abi") as String?)?.takeIf { it != "universal" }
val abiCodes = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2)

android {
    namespace = "com.david7ce.vozreves"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.david7ce.vozreves"
        minSdk = 26
        targetSdk = 35
        // On GitHub Actions the build number keeps rising, so every APK updates over the last one.
        // ×10 + ABI code so the per-architecture APKs don't collide with each other.
        val build = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = build * 10 + (abiCodes[targetAbi] ?: 0)
        // Hand-bumped on meaningful changes; versionCode (above) is what makes every build installable.
        versionName = "1.0"
        // Only the app's 5 languages: drops the extra translations that come from libraries.
        resourceConfigurations += listOf("en", "es", "zh", "hi", "ar")
    }

    // -Pabi=arm64-v8a | armeabi-v7a | universal (the workflow builds each one in parallel).
    splits {
        abi {
            isEnable = targetAbi != null
            if (targetAbi != null) { reset(); include(targetAbi) }
            isUniversalApk = false
        }
    }

    // Firma: si el workflow encuentra el keystore en los secrets del repo, lo usa;
    // si no, firma con la clave debug (vale para probar, pero cambia en cada compilación).
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
    kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    buildFeatures { compose = true }

    // Lighter APK: drop metadata the app doesn't use at runtime.
    packaging {
        resources.excludes += setOf("META-INF/*.version", "DebugProbesKt.bin", "kotlin-tooling-metadata.json")
    }
    // No Google's encrypted dependency block (adds nothing outside Play; F-Droid asks for its absence).
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true // Robolectric needs real resources, not stubs
    }
}

dependencies {
    // Compose (runtime, ui, material3) comes in through :pkgshared (api, Compose Multiplatform 1.11.1).
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")

    implementation(project(":pkgshared"))

    testImplementation("junit:junit:4.13.2")
    // Compose smoke tests on the JVM (no emulator): Robolectric hosts the Activity, ui-test-junit4
    // drives it.
    testImplementation("androidx.compose.ui:ui-test-junit4:1.7.8")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("org.robolectric:robolectric:4.16")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.8")
}
