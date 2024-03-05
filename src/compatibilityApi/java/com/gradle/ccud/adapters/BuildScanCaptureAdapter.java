package com.gradle.ccud.adapters;

/**
 * Adapter for {@link com.gradle.develocity.agent.gradle.scan.BuildScanCaptureConfiguration} and {@link com.gradle.ccud.adapters.enterprise.proxies.BuildScanCaptureSettingsProxy}
 */
public interface BuildScanCaptureAdapter {

    void setFileFingerprints(boolean capture);

    boolean isFileFingerprints();

    void setBuildLogging(boolean capture);

    boolean isBuildLogging();

    void setTestLogging(boolean capture);

    boolean isTestLogging();

}
