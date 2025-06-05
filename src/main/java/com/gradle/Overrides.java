package com.gradle;

import com.gradle.develocity.agent.gradle.adapters.BuildCacheConfigurationAdapter;
import com.gradle.develocity.agent.gradle.adapters.BuildCacheConfigurationAdapter.LocalBuildCacheAdapter;
import com.gradle.develocity.agent.gradle.adapters.BuildCacheConfigurationAdapter.RemoteBuildCacheAdapter;
import com.gradle.develocity.agent.gradle.adapters.DevelocityAdapter;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.ProviderFactory;

import java.time.Duration;
import java.util.Optional;

/**
 * Provide standardized Develocity configuration. By applying the plugin, these settings will automatically be applied.
 */
final class Overrides {

    private static final Logger logger = Logging.getLogger(Overrides.class);

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

    void configureBuildCache(BuildCacheConfigurationAdapter buildCache) {
        configureLocalBuildCache(buildCache.getLocal());

        RemoteBuildCacheAdapter remote = buildCache.getRemote();
        if (remote != null) {
            configureRemoteBuildCache(remote);
        }
    }

    private void configureLocalBuildCache(LocalBuildCacheAdapter local) {
        sysPropertyOrEnvVariable(LOCAL_CACHE_DIRECTORY, providers).ifPresent(local::setDirectory);
        durationSysPropertyOrEnvVariable(LOCAL_CACHE_REMOVE_UNUSED_ENTRIES_AFTER_DAYS, providers).ifPresent(v -> {
            if (!Utils.isGradle9OrNewer()) {
                local.setRemoveUnusedEntriesAfterDays((int) v.toDays());
            } else {
                logger.warn("{} override unsupported. As of Gradle 9.0, entry retention can only be changed in an init script", LOCAL_CACHE_REMOVE_UNUSED_ENTRIES_AFTER_DAYS);
            }
        });
        booleanSysPropertyOrEnvVariable(LOCAL_CACHE_ENABLED, providers).ifPresent(local::setEnabled);
        booleanSysPropertyOrEnvVariable(LOCAL_CACHE_PUSH, providers).ifPresent(local::setPush);
    }

    private void configureRemoteBuildCache(RemoteBuildCacheAdapter remote) {
        sysPropertyOrEnvVariable(REMOTE_CACHE_URL, providers).ifPresent(remote::setUrl);
        sysPropertyOrEnvVariable(REMOTE_CACHE_SERVER, providers).ifPresent(remote::setServer);
        sysPropertyOrEnvVariable(REMOTE_CACHE_PATH, providers).ifPresent(remote::setPath);
        booleanSysPropertyOrEnvVariable(REMOTE_CACHE_ALLOW_UNTRUSTED_SERVER, providers).ifPresent(remote::setAllowUntrustedServer);
        booleanSysPropertyOrEnvVariable(REMOTE_CACHE_ALLOW_INSECURE_PROTOCOL, providers).ifPresent(remote::setAllowInsecureProtocol);
        booleanSysPropertyOrEnvVariable(REMOTE_CACHE_ENABLED, providers).ifPresent(remote::setEnabled);
        booleanSysPropertyOrEnvVariable(REMOTE_CACHE_PUSH, providers).ifPresent(remote::setPush);
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
