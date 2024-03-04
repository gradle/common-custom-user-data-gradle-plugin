package com.gradle.ccud.adapters.enterprise.proxies;

import com.gradle.ccud.adapters.reflection.ProxyType;

/**
 * Proxy interface for {@code com.gradle.scan.plugin.BuildResult}
 */
public interface BuildResultProxy extends ProxyType {

    Throwable getFailure();

}
