plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// -Pabi=arm64-v8a | armeabi-v7a | universal (the CI workflow builds each one in parallel).
val targetAbi: String? = (findProperty("abi") as String?)?.takeIf { it != "universal" }
val abiCodes = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2)

android {
    namespace = "com.deivon.fuellog"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.deivon.fuellog"
        minSdk = 26
        targetSdk = 35
        // CI run number ×10 + ABI code: every build installs over the previous one.
        val build = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = build * 10 + (abiCodes[targetAbi] ?: 0)
        versionName = "1.0.$build"
        resourceConfigurations += listOf("en", "es", "fr", "de", "pt", "zh", "hi", "ar")
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
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }

    packaging {
        resources.excludes += setOf("META-INF/*.version", "DebugProbesKt.bin", "kotlin-tooling-metadata.json")
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    // AndroidX / Compose only. No third-party runtime libraries.
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")

    implementation(project(":shared"))

    // Tests only (not shipped): JUnit, and the real org.json so backup parsing runs on the JVM.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
