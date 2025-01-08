// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.android.test) apply false
}

spotless {
    kotlin {
        target("**/*.kt")
        ktlint(libs.versions.ktlint.get())
            .editorConfigOverride(
                mapOf(
                    "ktlint_code_style" to "android_studio",
                    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                )
            )
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
