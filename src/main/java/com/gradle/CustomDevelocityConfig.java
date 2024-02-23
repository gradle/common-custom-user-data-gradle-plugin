package com.gradle;

import com.gradle.ccud.proxies.enterprise.GradleEnterpriseExtensionProxy;
import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.caching.configuration.BuildCacheConfiguration;

/**
 * Provide standardized Develocity configuration.
 * By applying the plugin, these settings will automatically be applied.
 */
final class CustomDevelocityConfig {

    void configureDevelocity(GradleEnterpriseExtensionProxy develocity) {
        /* Example of Develocity configuration

        develocity.setServer("https://enterprise-samples.gradle.com");
        develocity.setAllowUntrustedServer(false);

        */
    }

    void configureDevelocityOnGradle4(BuildScanExtension buildScan) {
        /* Example of Develocity configuration

        buildScan.setServer("https://enterprise-samples.gradle.com");
        buildScan.setAllowUntrustedServer(false);

        */
    }

    void configureBuildScanPublishing(BuildScanExtension buildScan) {
        /* Example of build scan publishing configuration

        boolean isCiServer = System.getenv().containsKey("CI");

        buildScan.publishAlways();
        buildScan.capture(capture -> capture.setTaskInputFiles(true));
        buildScan.setUploadInBackground(!isCiServer);

        */
    }

    void configureBuildScanPublishingOnGradle4(BuildScanExtension buildScan) {
        /* Example of build scan publishing configuration

        boolean isCiServer = System.getenv().containsKey("CI");

        buildScan.publishAlways();

        */
    }

    void configureBuildCache(BuildCacheConfiguration buildCache) {
        /* Example of build cache configuration

        boolean isCiServer = System.getenv().containsKey("CI");

        // Enable the local build cache for all local and CI builds
        // For short-lived CI agents, it makes sense to disable the local build cache
        buildCache.local(local -> {
            local.setEnabled(true);
        });

        // Only permit store operations to the remote build cache for CI builds
        // Local builds will only read from the remote build cache
        buildCache.remote(GradleEnterpriseBuildCache.class, remote -> {
            remote.setEnabled(true);
            remote.setPush(isCiServer);
        });

        */
    }

}
