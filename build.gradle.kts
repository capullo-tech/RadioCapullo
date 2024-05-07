// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.gradle.develocity)
}

spotless {
    kotlin {
        target("**/*.kt")
        ktlint()
        ktlint(libs.versions.ktlint.get()).userData(mapOf("max_line_length" to "100"))
    }
    kotlinGradle {
        target("*.gradle.kts") // default target for kotlinGradle
        ktlint()
    }
}

develocity {
    buildScan {
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
        obfuscation {
            username { name -> name.reversed() }
            hostname { host -> host.toCharArray().map { character -> Character.getNumericValue(character) }.joinToString("-") }
            ipAddresses { addresses -> addresses.map { _ -> "0.0.0.0" } }
        }
    }
}