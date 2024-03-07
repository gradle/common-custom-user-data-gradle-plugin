package com.gradle.ccud.adapters.enterprise;

import com.gradle.ccud.adapters.BuildResultAdapter;
import com.gradle.ccud.adapters.BuildScanAdapter;
import com.gradle.ccud.adapters.BuildScanCaptureAdapter;
import com.gradle.ccud.adapters.BuildScanObfuscationAdapter;
import com.gradle.ccud.adapters.PublishedBuildScanAdapter;
import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.Action;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Collections;
import java.util.List;

class BuildScanExtensionAdapter implements BuildScanAdapter {

    private final BuildScanExtension buildScan;
    private final BuildScanCaptureSettingsAdapter capture;
    private final BuildScanDataObfuscationAdapter obfuscation;

    BuildScanExtensionAdapter(BuildScanExtension buildScan) {
        this.buildScan = buildScan;
        this.capture = new BuildScanCaptureSettingsAdapter(buildScan.getCapture());
        this.obfuscation = new BuildScanDataObfuscationAdapter(buildScan.getObfuscation());
    }

    @Override
    public void background(Action<? super BuildScanAdapter> action) {
        buildScan.background(__ -> action.execute(this));
    }

    @Override
    public void tag(String tag) {
        buildScan.tag(tag);
    }

    @Override
    public void value(String name, String value) {
        buildScan.value(name, value);
    }

    @Override
    public void link(String name, String url) {
        buildScan.link(name, url);
    }

    @Override
    public void buildFinished(Action<? super BuildResultAdapter> action) {
        //noinspection Convert2Lambda
        buildScan.buildFinished(buildResult -> action.execute(new BuildResultAdapter() {
            @Override
            public List<Throwable> getFailures() {
                return Collections.singletonList(buildResult.getFailure());
            }
        }));
    }

    @Override
    public void buildScanPublished(Action<? super PublishedBuildScanAdapter> action) {
        buildScan.buildScanPublished(scan -> action.execute(new PublishedBuildScanAdapter() {
            @Override
            public String getBuildScanId() {
                return scan.getBuildScanId();
            }

            @Override
            public URI getBuildScanUri() {
                return scan.getBuildScanUri();
            }
        }));
    }

    @Override
    public void setTermsOfUseUrl(String termsOfServiceUrl) {
        buildScan.setTermsOfServiceUrl(termsOfServiceUrl);
    }

    @Nullable
    @Override
    public String getTermsOfUseUrl() {
        return buildScan.getTermsOfServiceUrl();
    }

    @Override
    public void setTermsOfUseAgree(@Nullable String agree) {
        buildScan.setTermsOfServiceAgree(agree);
    }

    @Nullable
    @Override
    public String getTermsOfUseAgree() {
        return buildScan.getTermsOfServiceAgree();
    }

    @Override
    public void setUploadInBackground(boolean uploadInBackground) {
        buildScan.setUploadInBackground(uploadInBackground);
    }

    @Override
    public boolean isUploadInBackground() {
        return buildScan.isUploadInBackground();
    }

    @Override
    public void publishAlways() {
        buildScan.publishAlways();
    }

    @Override
    public void publishAlwaysIf(boolean condition) {
        buildScan.publishAlwaysIf(condition);
    }

    @Override
    public void publishOnFailure() {
        buildScan.publishOnFailure();
    }

    @Override
    public void publishOnFailureIf(boolean condition) {
        buildScan.publishOnFailureIf(condition);
    }

    @Nullable
    @Override
    public BuildScanObfuscationAdapter getObfuscation() {
        return obfuscation;
    }

    @Override
    public void obfuscation(Action<? super BuildScanObfuscationAdapter> action) {
        action.execute(this.obfuscation);
    }

    @Nullable
    @Override
    public BuildScanCaptureAdapter getCapture() {
        return capture;
    }

    @Override
    public void capture(Action<? super BuildScanCaptureAdapter> action) {
        action.execute(this.capture);
    }
}
