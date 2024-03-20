package com.gradle;

import com.gradle.develocity.agent.gradle.adapters.BuildCacheAdapter;
import com.gradle.develocity.agent.gradle.adapters.DevelocityAdapter;
import com.gradle.develocity.agent.gradle.adapters.develocity.DevelocityBuildCacheAdapter;
import com.gradle.develocity.agent.gradle.adapters.enterprise.GradleEnterpriseBuildCacheAdapter;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.caching.configuration.AbstractBuildCache;
import org.gradle.caching.configuration.BuildCacheConfiguration;
import org.gradle.caching.http.HttpBuildCache;

import java.time.Duration;
import java.util.Optional;

/**
 * Provide standardized Develocity configuration. By applying the plugin, these settings will automatically be applied.
 */
final class Overrides {

    // system properties to override Develocity configuration
    static final String DEVELOCITY_URL = "develocity.url";
    // deprecated, use 'develocity.url' instead
    static final String GRADLE_ENTERPRISE_URL = "gradle.enterprise.url";
    static final String DEVELOCITY_ALLOW_UNTRUSTED_SERVER = "develocity.allowUntrustedServer";
    // deprecated, use 'develocity.allowUntrustedServer' instead
    static final String GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER = "gradle.enterprise.allowUntrustedServer";

    // system properties to override local build cache configuration
    static final String LOCAL_CACHE_DIRECTORY = "gradle.cache.local.directory";
    static final String LOCAL_CACHE_REMOVE_UNUSED_ENTRIES_AFTER_DAYS = "gradle.cache.local.removeUnusedEntriesAfterDays";
    static final String LOCAL_CACHE_ENABLED = "gradle.cache.local.enabled";
    static final String LOCAL_CACHE_PUSH = "gradle.cache.local.push";

    // system properties to override remote build cache configuration
    static final String REMOTE_CACHE_URL = "gradle.cache.remote.url";
    static final String REMOTE_CACHE_SERVER = "gradle.cache.remote.server";
    static final String REMOTE_CACHE_PATH = "gradle.cache.remote.path";
    static final String REMOTE_CACHE_ALLOW_UNTRUSTED_SERVER = "gradle.cache.remote.allowUntrustedServer";
    static final String REMOTE_CACHE_ALLOW_INSECURE_PROTOCOL = "gradle.cache.remote.allowInsecureProtocol";
    static final String REMOTE_CACHE_ENABLED = "gradle.cache.remote.enabled";
    static final String REMOTE_CACHE_PUSH = "gradle.cache.remote.push";

    private final ProviderFactory providers;

    Overrides(ProviderFactory providers) {
        this.providers = providers;
    }

    void configureDevelocity(DevelocityAdapter develocity) {
        firstAvailableSysPropertyOrEnvVariable(providers, DEVELOCITY_URL, GRADLE_ENTERPRISE_URL).ifPresent(develocity::setServer);
        firstAvailableBooleanSysPropertyOrEnvVariable(providers, DEVELOCITY_ALLOW_UNTRUSTED_SERVER, GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER).ifPresent(develocity::setAllowUntrustedServer);
    }

    void configureBuildCache(BuildCacheConfiguration buildCache, Class<? extends AbstractBuildCache> develocityCacheClass) {
        buildCache.local(local -> {
            sysPropertyOrEnvVariable(LOCAL_CACHE_DIRECTORY, providers).ifPresent(local::setDirectory);
            durationSysPropertyOrEnvVariable(LOCAL_CACHE_REMOVE_UNUSED_ENTRIES_AFTER_DAYS, providers).ifPresent(v -> local.setRemoveUnusedEntriesAfterDays((int) v.toDays()));
            booleanSysPropertyOrEnvVariable(LOCAL_CACHE_ENABLED, providers).ifPresent(local::setEnabled);
            booleanSysPropertyOrEnvVariable(LOCAL_CACHE_PUSH, providers).ifPresent(local::setPush);
        });

        // Only touch remote build cache configuration if it is already present and of type HttpBuildCache or DevelocityBuildCache
        // Do nothing in case of another build cache type like AWS S3 being used
        if (buildCache.getRemote() instanceof HttpBuildCache) {
            buildCache.remote(HttpBuildCache.class, remote -> {
                sysPropertyOrEnvVariable(REMOTE_CACHE_URL, providers).ifPresent(remote::setUrl);
                booleanSysPropertyOrEnvVariable(REMOTE_CACHE_ALLOW_UNTRUSTED_SERVER, providers).ifPresent(remote::setAllowUntrustedServer);
                booleanSysPropertyOrEnvVariable(REMOTE_CACHE_ALLOW_INSECURE_PROTOCOL, providers).ifPresent(remote::setAllowInsecureProtocol);
                booleanSysPropertyOrEnvVariable(REMOTE_CACHE_ENABLED, providers).ifPresent(remote::setEnabled);
                booleanSysPropertyOrEnvVariable(REMOTE_CACHE_PUSH, providers).ifPresent(remote::setPush);
            });
        } else if (develocityCacheClass.isInstance(buildCache.getRemote())) {
            buildCache.remote(develocityCacheClass, remote -> {
                BuildCacheAdapter adapter = createBuildCacheAdapter(remote, develocityCacheClass);
                sysPropertyOrEnvVariable(REMOTE_CACHE_SERVER, providers).ifPresent(adapter::setServer);
                sysPropertyOrEnvVariable(REMOTE_CACHE_PATH, providers).ifPresent(adapter::setPath);
                booleanSysPropertyOrEnvVariable(REMOTE_CACHE_ALLOW_UNTRUSTED_SERVER, providers).ifPresent(adapter::setAllowUntrustedServer);
                booleanSysPropertyOrEnvVariable(REMOTE_CACHE_ALLOW_INSECURE_PROTOCOL, providers).ifPresent(adapter::setAllowInsecureProtocol);
                booleanSysPropertyOrEnvVariable(REMOTE_CACHE_ENABLED, providers).ifPresent(adapter::setEnabled);
                booleanSysPropertyOrEnvVariable(REMOTE_CACHE_PUSH, providers).ifPresent(adapter::setPush);
            });
        }
    }

    private static BuildCacheAdapter createBuildCacheAdapter(AbstractBuildCache cache, Class<? extends AbstractBuildCache> reportedCacheClass) {
        if (reportedCacheClass.getName().toLowerCase().contains("develocity")) {
            return new DevelocityBuildCacheAdapter(cache);
        }

        return new GradleEnterpriseBuildCacheAdapter(cache);
    }

    static Optional<String> firstAvailableSysPropertyOrEnvVariable(ProviderFactory providers, String... sysPropertyNames) {
        for (String sysPropertyName : sysPropertyNames) {
            Optional<String> optValue = sysPropertyOrEnvVariable(sysPropertyName, providers);
            if (optValue.isPresent()) {
                return optValue;
            }
        }

        return Optional.empty();
    }

    static Optional<String> sysPropertyOrEnvVariable(String sysPropertyName, ProviderFactory providers) {
        return Utils.sysPropertyOrEnvVariable(sysPropertyName, toEnvVarName(sysPropertyName), providers);
    }

    static Optional<Boolean> firstAvailableBooleanSysPropertyOrEnvVariable(ProviderFactory providers, String... sysPropertyNames) {
        for (String sysPropertyName : sysPropertyNames) {
            Optional<Boolean> optValue = booleanSysPropertyOrEnvVariable(sysPropertyName, providers);
            if (optValue.isPresent()) {
                return optValue;
            }
        }

        return Optional.empty();
    }

    static Optional<Boolean> booleanSysPropertyOrEnvVariable(String sysPropertyName, ProviderFactory providers) {
        return Utils.booleanSysPropertyOrEnvVariable(sysPropertyName, toEnvVarName(sysPropertyName), providers);
    }

    static Optional<Duration> durationSysPropertyOrEnvVariable(String sysPropertyName, ProviderFactory providers) {
        return Utils.durationSysPropertyOrEnvVariable(sysPropertyName, toEnvVarName(sysPropertyName), providers);
    }

    private static String toEnvVarName(String sysPropertyName) {
        return sysPropertyName.toUpperCase().replace('.', '_');
    }

}
