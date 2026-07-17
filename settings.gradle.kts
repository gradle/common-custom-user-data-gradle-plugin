plugins {
    id("com.gradle.develocity") version "4.5.0"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "2.7.0"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

val isCI = System.getenv("CI") != null

develocity {
    server = "https://ge.solutions-team.gradle.com"
    buildScan {
        uploadInBackground = !isCI
        publishing.onlyIf { it.isAuthenticated }
        obfuscation {
            ipAddresses { addresses -> addresses.map { _ -> "0.0.0.0" } }
        }

        // Runner metadata, populated on CI by the develocity-runner-info action.
        // Captured as custom values (not tags) so each dimension stays named and
        // independently queryable when correlating Artifact Cache overhead with the
        // runner hardware/region. Guarded so local builds emit nothing, and a runner
        // without Azure IMDS (self-hosted) simply omits what it can't detect.
        mapOf(
            "Runner Type" to "DV_RUNNER_TYPE",               // optional human label, if the CI step set one
            "Runner Environment" to "DV_RUNNER_ENVIRONMENT", // github-hosted vs self-hosted (runner context)
            "Runner VM Size" to "DV_RUNNER_VM_SIZE",         // authoritative Azure SKU from IMDS
            "Runner CPU Count" to "DV_RUNNER_CPU_COUNT",     // from nproc; works everywhere
            "Runner Region" to "DV_RUNNER_REGION",           // Azure region from IMDS
            "Runner Context" to "DV_RUNNER_CONTEXT",         // entire GitHub Actions runner context (toJSON)
        ).forEach { (name, envVar) ->
            System.getenv(envVar)?.takeIf { it.isNotBlank() }?.let { value(name, it) }
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
        val accessKey = System.getenv("DEVELOCITY_ACCESS_KEY")
        isPush = isCI && accessKey != null
    }
}

rootProject.name = "common-custom-user-data-gradle-plugin"
