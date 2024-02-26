package com.gradle.ccud.adapters.enterprise.proxies;

import com.gradle.ccud.adapters.reflection.ProxyType;
import org.gradle.api.Action;
import org.gradle.caching.configuration.AbstractBuildCache;

import javax.annotation.Nullable;

/**
 * Proxy interface for {@code com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension}
 */
public interface GradleEnterpriseExtensionProxy extends ProxyType {

    BuildScanExtensionProxy getBuildScan();

    default void buildScan(Action<? super BuildScanExtensionProxy> action) {
        action.execute(getBuildScan());
    }

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

    Class<? extends AbstractBuildCache> getBuildCache();

}
