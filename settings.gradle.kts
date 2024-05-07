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
        maven(url = "https://www.jitpack.io")
    }
}

plugins {
    id("com.gradle.develocity") version("3.17.2")
}

rootProject.name = "Radio"
include(":app")
include(":snapcast-deps")
include(":lib-snapcast-android")
