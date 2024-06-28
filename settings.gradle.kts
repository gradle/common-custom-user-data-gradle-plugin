plugins {
    id("com.gradle.develocity") version "3.17.5"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "2.0.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

val isCI = providers.environmentVariable("CI").isPresent

develocity {
    server = "https://ge.solutions-team.gradle.com"
    buildScan {
        uploadInBackground = !isCI
        publishing.onlyIf { it.isAuthenticated }
        obfuscation {
            ipAddresses { addresses -> addresses.map { _ -> "0.0.0.0" } }
        }
    }
}

buildCache {
    local {
        isEnabled = true
    }

    remote(develocity.buildCache) {
        isEnabled = true
        // Check access key presence to avoid build cache errors on PR builds when access key is not present
        val accessKey = providers.environmentVariable("GRADLE_ENTERPRISE_ACCESS_KEY").isPresent
        isPush = isCI && accessKey
    }
}

rootProject.name = "common-custom-user-data-gradle-plugin"
