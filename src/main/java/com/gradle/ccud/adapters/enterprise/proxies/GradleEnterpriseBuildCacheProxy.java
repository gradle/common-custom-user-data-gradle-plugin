package com.gradle.ccud.adapters.enterprise.proxies;

import com.gradle.ccud.adapters.reflection.ProxyType;
import org.gradle.caching.configuration.BuildCache;

import javax.annotation.Nullable;

/**
 * Proxy interface for {@code com.gradle.enterprise.gradleplugin.GradleEnterpriseBuildCache}
 */
public interface GradleEnterpriseBuildCacheProxy extends ProxyType, BuildCache {

    @Nullable
    String getServer();

    void setServer(@Nullable String server);

    @Nullable
    String getPath();

    void setPath(@Nullable String path);

    @Nullable
    Boolean getAllowUntrustedServer();

    void setAllowUntrustedServer(boolean allowUntrustedServer);

    boolean getAllowInsecureProtocol();

    void setAllowInsecureProtocol(boolean allowInsecureProtocol);

    void usernameAndPassword(String username, String password);

    @Nullable
    Object getUsernameAndPassword();

    boolean getUseExpectContinue();

    void setUseExpectContinue(boolean useExpectContinue);
}
