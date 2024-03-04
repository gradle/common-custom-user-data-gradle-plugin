package com.gradle.ccud.adapters.develocity;

import com.gradle.ccud.adapters.BuildScanAdapter;
import com.gradle.ccud.adapters.DevelocityAdapter;
import com.gradle.ccud.adapters.reflection.ProxyFactory;
import com.gradle.develocity.agent.gradle.DevelocityConfiguration;
import org.gradle.api.Action;
import org.gradle.caching.configuration.AbstractBuildCache;
import org.jetbrains.annotations.Nullable;

public class DevelocityConfigurationAdapter implements DevelocityAdapter {

    private final DevelocityConfiguration configuration;
    private final BuildScanConfigurationAdapter buildScan;

    public DevelocityConfigurationAdapter(Object configuration) {
        this.configuration = ProxyFactory.createProxy(configuration, DevelocityConfiguration.class);
        this.buildScan = new BuildScanConfigurationAdapter(this.configuration.getBuildScan());
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
        configuration.getServer().set(server);
    }

    @Nullable
    @Override
    public String getServer() {
        return configuration.getServer().getOrNull();
    }

    @Override
    public void setProjectId(@Nullable String projectId) {
        configuration.getProjectId().set(projectId);
    }

    @Nullable
    @Override
    public String getProjectId() {
        return configuration.getProjectId().getOrNull();
    }

    @Override
    public void setAllowUntrustedServer(boolean allow) {
        configuration.getAllowUntrustedServer().set(allow);
    }

    @Override
    public boolean getAllowUntrustedServer() {
        return configuration.getAllowUntrustedServer().get();
    }

    @Override
    public void setAccessKey(@Nullable String accessKey) {
        configuration.getAccessKey().set(accessKey);
    }

    @Nullable
    @Override
    public String getAccessKey() {
        return configuration.getAccessKey().getOrNull();
    }

    @Nullable
    @Override
    public Class<? extends AbstractBuildCache> getBuildCache() {
        return configuration.getBuildCache();
    }
}
