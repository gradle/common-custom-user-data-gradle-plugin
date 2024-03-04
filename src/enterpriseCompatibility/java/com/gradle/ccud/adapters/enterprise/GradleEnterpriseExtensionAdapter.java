package com.gradle.ccud.adapters.enterprise;

import com.gradle.ccud.adapters.BuildScanAdapter;
import com.gradle.ccud.adapters.DevelocityAdapter;
import com.gradle.ccud.adapters.reflection.ProxyFactory;
import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension;
import org.gradle.api.Action;
import org.gradle.caching.configuration.AbstractBuildCache;
import org.jetbrains.annotations.Nullable;

public class GradleEnterpriseExtensionAdapter implements DevelocityAdapter {

    private final GradleEnterpriseExtension extension;
    private final BuildScanExtensionAdapter buildScan;

    public GradleEnterpriseExtensionAdapter(Object extension) {
        this.extension = ProxyFactory.createProxy(extension, GradleEnterpriseExtension.class);
        this.buildScan = new BuildScanExtensionAdapter(this.extension.getBuildScan());
    }

    @Override
    public BuildScanAdapter getBuildScan() {
        return buildScan;
    }

    @Override
    public void buildScan(Action<? super BuildScanAdapter> action) {
        action.execute(buildScan);
    }

    @Override
    public void setServer(@Nullable String server) {
        extension.setServer(server);
    }

    @Nullable
    @Override
    public String getServer() {
        return extension.getServer();
    }

    @Override
    public void setProjectId(@Nullable String projectId) {
        extension.setProjectId(projectId);
    }

    @Nullable
    @Override
    public String getProjectId() {
        return extension.getProjectId();
    }

    @Override
    public void setAllowUntrustedServer(boolean allow) {
        extension.setAllowUntrustedServer(allow);
    }

    @Override
    public boolean getAllowUntrustedServer() {
        return extension.getAllowUntrustedServer();
    }

    @Override
    public void setAccessKey(@Nullable String accessKey) {
        extension.setAccessKey(accessKey);
    }

    @Nullable
    @Override
    public String getAccessKey() {
        return extension.getAccessKey();
    }

    @Nullable
    @Override
    public Class<? extends AbstractBuildCache> getBuildCache() {
        return extension.getBuildCache();
    }
}
