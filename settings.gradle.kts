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

rootProject.name = "Radio"
include(":app")
include(":lib-snapcast-android")
include(":librespot-android-decoder")
include(":librespot-android-sink")
include(":librespot-android-zeroconf-server")
