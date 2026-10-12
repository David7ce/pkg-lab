plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val targetAbi: String? = (findProperty("abi") as String?)?.takeIf { it != "universal" }
val abiCodes = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2)

android {
    namespace = "com.deivon.vozreves"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.deivon.vozreves"
        minSdk = 26
        targetSdk = 35
        // En GitHub Actions el número de compilación sube solo, así cada APK actualiza a la anterior.
        // ×10 + código de ABI para que las APKs por arquitectura no choquen entre sí.
        val build = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = build * 10 + (abiCodes[targetAbi] ?: 0)
        versionName = "1.0.$build"
        // Solo los 5 idiomas de la app: quita las traducciones sobrantes de las librerías.
        resourceConfigurations += listOf("en", "es", "zh", "hi", "ar")
    }

    // -Pabi=arm64-v8a | armeabi-v7a | universal (el workflow compila cada una en paralelo).
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
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }

    // APK más ligera: fuera metadatos que la app no usa en ejecución.
    packaging {
        resources.excludes += setOf("META-INF/*.version", "DebugProbesKt.bin", "kotlin-tooling-metadata.json")
    }
    // Sin el bloque de dependencias cifrado de Google (no aporta nada fuera de Play; F-Droid lo pide).
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")

    testImplementation("junit:junit:4.13.2")
}
