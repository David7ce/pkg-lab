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
// Componentes compartidos de todas las apps (android/shared).
project(":pkgshared").projectDir = file("../shared")
