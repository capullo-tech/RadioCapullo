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

rootProject.name = "Radio"
include(":app")
include(":lib-snapcast-android")
include(":librespot-android")
include(":librespot-android:librespot-android-decoder")
include(":librespot-android:librespot-android-sink")
include(":librespot-android:librespot-android-zeroconf-server")
