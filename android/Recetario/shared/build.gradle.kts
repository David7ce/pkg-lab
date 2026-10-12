import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Shared code: model, logic, persistence and the Compose UI live in commonMain.
// Android-only pieces (SQLite driver, files, images, network, PDF, Activity) live in androidMain.
// Desktop (Windows/Linux) will add a jvm("desktop") target here; see README.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    sourceSets {
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
