package com.gradle.ccud.adapters;

import org.gradle.api.Action;

import javax.annotation.Nullable;

/**
 * Adapter for {@link com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration} and {@link com.gradle.ccud.adapters.enterprise.proxies.BuildScanExtensionProxy}
 */
public interface BuildScanAdapter {

    void background(Action<? super BuildScanAdapter> action);

    void tag(String tag);

    void value(String name, String value);

    void link(String name, String url);

    void buildFinished(Action<? super BuildResultAdapter> action);

    void buildScanPublished(Action<? super PublishedBuildScanAdapter> action);

    void setTermsOfUseUrl(String termsOfServiceUrl);

    @Nullable
    String getTermsOfUseUrl();

    void setTermsOfUseAgree(@Nullable String agree);

    @Nullable
    String getTermsOfUseAgree();

    void setUploadInBackground(boolean uploadInBackground);

    boolean isUploadInBackground();

    void publishAlways();

    void publishAlwaysIf(boolean condition);

    void publishOnFailure();

    void publishOnFailureIf(boolean condition);

    @Nullable
    BuildScanObfuscationAdapter getObfuscation();

    void obfuscation(Action<? super BuildScanObfuscationAdapter> action);

    @Nullable
    BuildScanCaptureAdapter getCapture();

    void capture(Action<? super BuildScanCaptureAdapter> action);
}
