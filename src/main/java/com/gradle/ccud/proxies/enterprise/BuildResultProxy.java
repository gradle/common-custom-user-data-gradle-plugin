package com.gradle.ccud.proxies.enterprise;

import com.gradle.ccud.proxies.ProxyType;

/**
 * Proxy interface for {@code com.gradle.scan.plugin.BuildResult}
 */
public interface BuildResultProxy extends ProxyType {

    Throwable getFailure();

}
