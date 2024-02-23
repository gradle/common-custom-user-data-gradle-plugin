package com.gradle.ccud.proxies.enterprise;

import com.gradle.enterprise.gradleplugin.GradleEnterpriseBuildCache;
import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.Action;

import javax.annotation.Nullable;

public interface GradleEnterpriseExtensionProxy {

    BuildScanExtension getBuildScan();

    void buildScan(Action<? super BuildScanExtension> action);

    void setServer(String server);

    @Nullable
    String getServer();

    void setProjectId(String projectId);

    @Nullable
    String getProjectId();

    void setAllowUntrustedServer(boolean allow);

    boolean getAllowUntrustedServer();

    void setAccessKey(String accessKey);

    @Nullable
    String getAccessKey();

    Class<? extends GradleEnterpriseBuildCache> getBuildCache();

}
