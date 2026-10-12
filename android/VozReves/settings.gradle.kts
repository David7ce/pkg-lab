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

rootProject.name = "VozReves"
include(":app")
// Módulo compartido por todas las apps (android/shared).
include(":pkgshared")
project(":pkgshared").projectDir = file("../shared")
