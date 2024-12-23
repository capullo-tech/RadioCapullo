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
        maven {
            name = "GithubPackages"
            url = uri("https://maven.pkg.github.com/capullo-tech/lib-snapcast-android")
            credentials {
                username = providers.gradleProperty("GITHUB_USERNAME").getOrElse(System.getenv("GITHUB_USERNAME") ?: "")
                password = providers.gradleProperty("GITHUB_TOKEN").getOrElse(System.getenv("GITHUB_TOKEN") ?: "")
            }
        }
    }
}

plugins {
    id("com.gradle.develocity") version("3.17.2")
}

rootProject.name = "Radio"
include(":app")
include(":snapcast-deps")
include(":lib-snapcast-android")
