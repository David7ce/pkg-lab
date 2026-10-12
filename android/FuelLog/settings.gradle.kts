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
}

rootProject.name = "FuelLog"
include(":app")
// Module shared by every app (android/shared).
include(":pkgshared")
project(":pkgshared").projectDir = file("../shared")
