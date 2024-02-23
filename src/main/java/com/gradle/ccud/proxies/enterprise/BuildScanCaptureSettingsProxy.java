package com.gradle.ccud.proxies.enterprise;

import com.gradle.ccud.proxies.ProxyType;

/**
 * Proxy interface for com.gradle.scan.plugin.BuildScanCaptureSettings
 */
public interface BuildScanCaptureSettingsProxy extends ProxyType {

    void setTaskInputFiles(boolean capture);

    boolean isTaskInputFiles();

    void setBuildLogging(boolean capture);

    boolean isBuildLogging();

    void setTestLogging(boolean capture);

    boolean isTestLogging();

}
