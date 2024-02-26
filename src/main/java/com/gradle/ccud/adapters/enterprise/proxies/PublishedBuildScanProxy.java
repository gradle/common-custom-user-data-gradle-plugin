package com.gradle.ccud.adapters.enterprise.proxies;

import com.gradle.ccud.adapters.reflection.ProxyType;

import java.net.URI;

/**
 * Proxy interface for {@code com.gradle.scan.plugin.PublishedBuildScan}
 */
public interface PublishedBuildScanProxy extends ProxyType {

    String getBuildScanId();

    URI getBuildScanUri();

}
