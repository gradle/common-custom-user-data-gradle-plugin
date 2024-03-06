package com.gradle.ccud.adapters.develocity;

import com.gradle.ccud.adapters.BuildResultAdapter;
import com.gradle.ccud.adapters.BuildScanAdapter;
import com.gradle.ccud.adapters.BuildScanCaptureAdapter;
import com.gradle.ccud.adapters.BuildScanObfuscationAdapter;
import com.gradle.ccud.adapters.PublishedBuildScanAdapter;
import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import org.gradle.api.Action;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.List;

class BuildScanConfigurationAdapter implements BuildScanAdapter {

    private final BuildScanConfiguration buildScan;
    private final BuildScanCaptureConfigurationAdapter capture;
    private final BuildScanDataObfuscationConfigurationAdapter obfuscation;

    BuildScanConfigurationAdapter(BuildScanConfiguration buildScan) {
        this.buildScan = buildScan;
        this.capture = new BuildScanCaptureConfigurationAdapter(buildScan.getCapture());
        this.obfuscation = new BuildScanDataObfuscationConfigurationAdapter(buildScan.getObfuscation());
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
        //noinspection Anonymous2MethodRef,Convert2Lambda
        buildScan.buildFinished(buildResult -> action.execute(new BuildResultAdapter() {
            @Override
            public List<Throwable> getFailures() {
                return buildResult.getFailures();
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
        buildScan.getTermsOfUseUrl().set(termsOfServiceUrl);
    }

    @Nullable
    @Override
    public String getTermsOfUseUrl() {
        return buildScan.getTermsOfUseUrl().getOrNull();
    }

    @Override
    public void setTermsOfUseAgree(@Nullable String agree) {
        buildScan.getTermsOfUseAgree().set(agree);
    }

    @Nullable
    @Override
    public String getTermsOfUseAgree() {
        return buildScan.getTermsOfUseAgree().getOrNull();
    }

    @Override
    public void setUploadInBackground(boolean uploadInBackground) {
        buildScan.getUploadInBackground().set(uploadInBackground);
    }

    @Override
    public boolean isUploadInBackground() {
        return buildScan.getUploadInBackground().get();
    }

    @Override
    public void publishAlways() {
        buildScan.publishing(publishing -> publishing.onlyIf(ctx -> true));
    }

    @Override
    public void publishAlwaysIf(boolean condition) {
        buildScan.publishing(publishing -> publishing.onlyIf(ctx -> condition));
    }

    @Override
    public void publishOnFailure() {
        buildScan.publishing(publishing -> publishing.onlyIf(ctx -> !ctx.getBuildResult().getFailures().isEmpty()));
    }

    @Override
    public void publishOnFailureIf(boolean condition) {
        buildScan.publishing(publishing -> publishing.onlyIf(ctx -> !ctx.getBuildResult().getFailures().isEmpty() && condition));
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
