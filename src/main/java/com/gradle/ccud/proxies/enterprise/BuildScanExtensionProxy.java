package com.gradle.ccud.proxies.enterprise;

import com.gradle.ccud.proxies.ProxyAction;
import com.gradle.scan.plugin.BuildResult;
import com.gradle.scan.plugin.PublishedBuildScan;
import org.gradle.api.Action;

import javax.annotation.Nullable;

public interface BuildScanExtensionProxy {

    @ProxyAction
    void background(Action<? super BuildScanExtensionProxy> action);

    void tag(String tag);

    void value(String name, String value);

    void link(String name, String url);

    void buildFinished(Action<? super BuildResult> action);

    void buildScanPublished(Action<? super PublishedBuildScan> action);

    void setTermsOfServiceUrl(String termsOfServiceUrl);

    String getTermsOfServiceUrl();

    void setTermsOfServiceAgree(String agree);

    String getTermsOfServiceAgree();

    void setServer(String server);

    @Nullable
    String getServer();

    void setAllowUntrustedServer(boolean allow);

    boolean getAllowUntrustedServer();

    void publishAlways();

    void publishAlwaysIf(boolean condition);

    void publishOnFailure();

    void publishOnFailureIf(boolean condition);

    void setUploadInBackground(boolean uploadInBackground);

    boolean isUploadInBackground();

    BuildScanDataObfuscationProxy getObfuscation();

    default void obfuscation(Action<? super BuildScanDataObfuscationProxy> action) {
        action.execute(getObfuscation());
    }

    BuildScanCaptureSettingsProxy getCapture();

    default void capture(Action<? super BuildScanCaptureSettingsProxy> action) {
        action.execute(getCapture());
    }

}
