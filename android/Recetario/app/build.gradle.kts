// Packaging only: application id, version, signing, ABI splits, launcher icon.
// All code lives in :shared (commonMain + androidMain).
plugins {
    alias(libs.plugins.android.application)
}

// -Pabi=arm64-v8a | armeabi-v7a | universal (the CI workflow builds each one in parallel).
val targetAbi: String? = (findProperty("abi") as String?)?.takeIf { it != "universal" }
val abiCodes = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2)

android {
    namespace = "com.deivon.recetario"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.deivon.recetario"
        minSdk = 26
        targetSdk = 36
        // CI run number ×10 + ABI code: every build installs over the previous one.
        val build = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = build * 10 + (abiCodes[targetAbi] ?: 0)
        versionName = "1.0.$build"
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
