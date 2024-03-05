package com.gradle.ccud.adapters;

import java.net.URI;

/**
 * Adapter for {@link com.gradle.develocity.agent.gradle.scan.PublishedBuildScan} and {@link com.gradle.ccud.adapters.enterprise.proxies.PublishedBuildScanProxy}
 */
public interface PublishedBuildScanAdapter {

    String getBuildScanId();

    URI getBuildScanUri();

}
