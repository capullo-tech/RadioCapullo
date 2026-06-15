pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://www.jitpack.io") }
        // lib-shairport-android is consumed from the local maven repo until it
        // is published; build it with:
        //   lib-shairport-android$ ./gradlew publishToMavenLocal
        mavenLocal()
    }
}

plugins {
    id("com.gradle.develocity") version("3.17.2")
}

rootProject.name = "Radio"
include(":app")
