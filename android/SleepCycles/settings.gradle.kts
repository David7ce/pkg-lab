pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    // Shared by every app under android/ and android/shared (see android/gradle/libs.versions.toml).
    versionCatalogs {
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}

rootProject.name = "SleepCycles"
include(":app", ":shared", ":pkgshared")
// Components shared by every app (android/shared).
project(":pkgshared").projectDir = file("../shared")
