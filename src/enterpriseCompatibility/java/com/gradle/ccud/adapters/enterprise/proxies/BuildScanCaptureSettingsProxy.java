package com.gradle.ccud.adapters.enterprise.proxies;

import com.gradle.ccud.adapters.reflection.ProxyType;

/**
 * Proxy interface for {@code com.gradle.scan.plugin.BuildScanCaptureSettings}
 */
public interface BuildScanCaptureSettingsProxy extends ProxyType {

    void setTaskInputFiles(boolean capture);

    boolean isTaskInputFiles();

    void setBuildLogging(boolean capture);

    boolean isBuildLogging();

    void setTestLogging(boolean capture);

    boolean isTestLogging();

}
