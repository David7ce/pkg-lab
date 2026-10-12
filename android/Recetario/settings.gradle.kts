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

rootProject.name = "Recetario"
include(":app", ":shared", ":pkgshared")
// Components shared by every app (android/shared).
project(":pkgshared").projectDir = file("../shared")
