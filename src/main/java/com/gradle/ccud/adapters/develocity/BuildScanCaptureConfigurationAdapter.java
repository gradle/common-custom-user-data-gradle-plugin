package com.gradle.ccud.adapters.develocity;

import com.gradle.ccud.adapters.BuildScanCaptureAdapter;
import com.gradle.develocity.agent.gradle.scan.BuildScanCaptureConfiguration;

class BuildScanCaptureConfigurationAdapter implements BuildScanCaptureAdapter {

    private final BuildScanCaptureConfiguration capture;

    BuildScanCaptureConfigurationAdapter(BuildScanCaptureConfiguration capture) {
        this.capture = capture;
    }

    @Override
    public void setFileFingerprints(boolean capture) {
        this.capture.getFileFingerprints().set(capture);
    }

    @Override
    public boolean isFileFingerprints() {
        return capture.getFileFingerprints().get();
    }

    @Override
    public void setBuildLogging(boolean capture) {
        this.capture.getBuildLogging().set(capture);
    }

    @Override
    public boolean isBuildLogging() {
        return capture.getBuildLogging().get();
    }

    @Override
    public void setTestLogging(boolean capture) {
        this.capture.getTestLogging().set(capture);
    }

    @Override
    public boolean isTestLogging() {
        return capture.getTestLogging().get();
    }
}
