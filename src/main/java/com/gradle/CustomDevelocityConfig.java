package com.gradle;

import com.gradle.develocity.agent.gradle.adapters.BuildCacheConfigurationAdapter;
import com.gradle.develocity.agent.gradle.adapters.BuildScanAdapter;
import com.gradle.develocity.agent.gradle.adapters.DevelocityAdapter;

/**
 * Provide standardized Develocity configuration.
 * By applying the plugin, these settings will automatically be applied.
 */
final class CustomDevelocityConfig {

    void configureDevelocity(DevelocityAdapter develocity) {
        /* Example of Develocity configuration

        develocity.setServer("https://enterprise-samples.gradle.com");
        develocity.setAllowUntrustedServer(false);

        develocity.setProjectId("ccud-injection");
        */
    }

    void configureBuildScanPublishing(BuildScanAdapter buildScan) {
        /* Example of build scan publishing configuration

        boolean isCiServer = System.getenv().containsKey("CI");

        buildScan.publishAlways();
        buildScan.setUploadInBackground(!isCiServer);

        buildScan.tag("CUSTOM_TAG");
        buildScan.link("custom-link", "https://enterprise-samples.gradle.com/faq");
        buildScan.value("Custom Value Key", "Custom value");

        buildScan.capture(capture -> {
            capture.setFileFingerprints(true);
            capture.setBuildLogging(false);
            capture.setTestLogging(false);
        });

        buildScan.obfuscation(obfuscation -> {
            obfuscation.hostname(s -> "FIXED-HOSTNAME");
            obfuscation.username(s -> "FIXED-USERNAME");
        });

        buildScan.background(adapter -> {
            adapter.tag("BACKGROUND_TAG");
        });

        buildScan.buildFinished(adapter -> {
            buildScan.tag("BUILD_FINISHED_TAG");
        });

         */
    }

    void configureBuildCache(BuildCacheConfigurationAdapter buildCache) {
        /* Example of build cache configuration

        boolean isCiServer = System.getenv().containsKey("CI");

        // Enable the local build cache for all local and CI builds
        // For short-lived CI agents, it makes sense to disable the local build cache
        buildCache.getLocal().setEnabled(true);

        // Only permit store operations to the remote build cache for CI builds
        // Local builds will only read from the remote build cache
        buildCache.getRemote().setEnabled(true);
        buildCache.getRemote().setPush(isCiServer);

         */
    }

}
