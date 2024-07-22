// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.android.test) apply false
}

spotless {
    kotlin {
        target("**/*.kt")
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
        if (!System.getenv("CI").isNullOrEmpty()) {
            publishing.onlyIf { true }
            tag("CI")
        } else {
            publishing.onlyIf { false }
        }
        obfuscation {
            username { name -> name.reversed() }
            ipAddresses { addresses -> addresses.map { _ -> "0.0.0.0" } }
        }
    }
}
