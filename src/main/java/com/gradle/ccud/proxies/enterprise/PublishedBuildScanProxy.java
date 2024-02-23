package com.gradle.ccud.proxies.enterprise;

import com.gradle.ccud.proxies.ProxyType;

import java.net.URI;

/**
 * Proxy interface for {@code com.gradle.scan.plugin.PublishedBuildScan}
 */
public interface PublishedBuildScanProxy extends ProxyType {

    String getBuildScanId();

    URI getBuildScanUri();

}
