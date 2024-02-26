package com.gradle.ccud.adapters.enterprise;

import com.gradle.ccud.adapters.BuildCacheAdapter;
import com.gradle.ccud.adapters.enterprise.proxies.GradleEnterpriseBuildCacheProxy;
import org.jetbrains.annotations.Nullable;

public class GradleEnterpriseBuildCacheAdapter implements BuildCacheAdapter {

    private final GradleEnterpriseBuildCacheProxy buildCache;

    public GradleEnterpriseBuildCacheAdapter(GradleEnterpriseBuildCacheProxy buildCache) {
        this.buildCache = buildCache;
    }

    @Nullable
    @Override
    public String getServer() {
        return buildCache.getServer();
    }

    @Override
    public void setServer(@Nullable String server) {
        buildCache.setServer(server);
    }

    @Nullable
    @Override
    public String getPath() {
        return buildCache.getPath();
    }

    @Override
    public void setPath(@Nullable String path) {
        buildCache.setPath(path);
    }

    @Nullable
    @Override
    public Boolean getAllowUntrustedServer() {
        return buildCache.getAllowUntrustedServer();
    }

    @Override
    public void setAllowUntrustedServer(boolean allowUntrustedServer) {
        buildCache.setAllowUntrustedServer(allowUntrustedServer);
    }

    @Override
    public boolean getAllowInsecureProtocol() {
        return buildCache.getAllowInsecureProtocol();
    }

    @Override
    public void setAllowInsecureProtocol(boolean allowInsecureProtocol) {
        buildCache.setAllowInsecureProtocol(allowInsecureProtocol);
    }

    @Override
    public void usernameAndPassword(String username, String password) {
        buildCache.usernameAndPassword(username, password);
    }

    @Nullable
    @Override
    public Object getUsernameAndPassword() {
        return buildCache.getUsernameAndPassword();
    }

    @Override
    public boolean getUseExpectContinue() {
        return buildCache.getUseExpectContinue();
    }

    @Override
    public void setUseExpectContinue(boolean useExpectContinue) {
        buildCache.setUseExpectContinue(useExpectContinue);
    }
}