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

        */
    }

    void configureBuildScanPublishing(BuildScanAdapter buildScan) {
        /* Example of build scan publishing configuration

        boolean isCiServer = System.getenv().containsKey("CI");

        buildScan.publishAlways();
        buildScan.capture(capture -> capture.setTaskInputFiles(true));
        buildScan.setUploadInBackground(!isCiServer);

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
        buildCache.getRemote().setStoreEnabled(isCiServer);

        */
    }

}
