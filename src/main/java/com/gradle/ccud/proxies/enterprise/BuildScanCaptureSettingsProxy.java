package com.gradle.ccud.proxies.enterprise;

public interface BuildScanCaptureSettingsProxy {

    void setTaskInputFiles(boolean capture);

    boolean isTaskInputFiles();

    void setBuildLogging(boolean capture);

    boolean isBuildLogging();

    void setTestLogging(boolean capture);

    boolean isTestLogging();

}
