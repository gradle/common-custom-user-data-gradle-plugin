package com.gradle.ccud.adapters;

import java.util.List;

/**
 * Adapter for {@link com.gradle.develocity.agent.gradle.scan.BuildResult} and {@link com.gradle.ccud.adapters.enterprise.proxies.BuildResultProxy}
 */
public interface BuildResultAdapter {

    List<Throwable> getFailures();

}
