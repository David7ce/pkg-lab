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

rootProject.name = "FuelLog"
include(":app")
// Module shared by every app (android/shared).
include(":pkgshared")
project(":pkgshared").projectDir = file("../shared")
