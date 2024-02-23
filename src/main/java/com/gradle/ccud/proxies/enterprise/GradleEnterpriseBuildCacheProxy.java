package com.gradle.ccud.proxies.enterprise;

import com.gradle.CommonCustomUserDataGradlePlugin;
import com.gradle.ccud.proxies.ProxyType;
import org.gradle.caching.configuration.BuildCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * Proxy interface for {@code com.gradle.enterprise.gradleplugin.GradleEnterpriseBuildCache}
 */
public interface GradleEnterpriseBuildCacheProxy extends ProxyType, BuildCache {

    Logger LOG = LoggerFactory.getLogger(CommonCustomUserDataGradlePlugin.class);

    static boolean isGradleEnterpriseBuildCache(BuildCache cache) {
        Class<?> buildCacheClass = gradleEnterpriseBuildCacheClass();
        if (buildCacheClass != null) {
            return buildCacheClass.isInstance(cache);
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    static Class<? extends BuildCache> gradleEnterpriseBuildCacheClass() {
        try {
            return (Class<? extends BuildCache>) Class.forName("com.gradle.enterprise.gradleplugin.GradleEnterpriseBuildCache");
        } catch (ClassNotFoundException e) {
            LOG.debug("Could not load GradleEnterpriseBuildCache", e);
            return null;
        }
    }

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
