package com.gradle.ccud.adapters;

import com.gradle.ccud.adapters.develocity.DevelocityBuildCacheAdapter;
import com.gradle.ccud.adapters.enterprise.GradleEnterpriseBuildCacheAdapter;
import com.gradle.ccud.adapters.enterprise.proxies.GradleEnterpriseBuildCacheProxy;
import com.gradle.ccud.adapters.reflection.ProxyFactory;
import com.gradle.develocity.agent.gradle.buildcache.DevelocityBuildCache;
import org.gradle.caching.configuration.AbstractBuildCache;
import org.gradle.caching.configuration.BuildCache;

import javax.annotation.Nullable;

/**
 * Adapter for {@link com.gradle.develocity.agent.gradle.buildcache.DevelocityBuildCache} and {@link com.gradle.ccud.adapters.enterprise.proxies.GradleEnterpriseBuildCacheProxy}
 */
public interface BuildCacheAdapter extends BuildCache {

    // if the method is called, we know that it is either a GE or DV cache
    static BuildCacheAdapter create(AbstractBuildCache cache, Class<? extends AbstractBuildCache> reportedCacheClass) {
        if (reportedCacheClass.getName().toLowerCase().contains("develocity")) {
            return new DevelocityBuildCacheAdapter((DevelocityBuildCache) cache);
        }

        return new GradleEnterpriseBuildCacheAdapter(ProxyFactory.createProxy(cache, GradleEnterpriseBuildCacheProxy.class));
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
