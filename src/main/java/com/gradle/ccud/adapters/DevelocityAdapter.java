package com.gradle.ccud.adapters;

import com.gradle.ccud.adapters.develocity.DevelocityConfigurationAdapter;
import com.gradle.ccud.adapters.enterprise.BuildScanExtension_1_X_Adapter;
import com.gradle.ccud.adapters.enterprise.GradleEnterpriseExtensionAdapter;
import com.gradle.ccud.adapters.enterprise.proxies.BuildScanExtensionProxy;
import com.gradle.ccud.adapters.enterprise.proxies.GradleEnterpriseExtensionProxy;
import com.gradle.ccud.adapters.reflection.ProxyFactory;
import com.gradle.develocity.agent.gradle.DevelocityConfiguration;
import org.gradle.api.Action;
import org.gradle.caching.configuration.AbstractBuildCache;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.stream.Stream;

import static com.gradle.Utils.isGradle5OrNewer;

/**
 * Adapter for {@link com.gradle.develocity.agent.gradle.DevelocityConfiguration} and {@link com.gradle.ccud.adapters.enterprise.proxies.GradleEnterpriseExtensionProxy}
 */
public interface DevelocityAdapter {

    String DEVELOCITY_CONFIGURATION = "com.gradle.develocity.agent.gradle.DevelocityConfiguration";
    String GRADLE_ENTERPRISE_EXTENSION = "com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension";
    String BUILD_SCAN_EXTENSION = "com.gradle.scan.plugin.BuildScanExtension";

    static DevelocityAdapter create(Object gradleEnterpriseOrDevelocity) {
        if (isDevelocityConfiguration(gradleEnterpriseOrDevelocity)) {
            return new DevelocityConfigurationAdapter(ProxyFactory.createProxy(gradleEnterpriseOrDevelocity, DevelocityConfiguration.class));
        } else if (isGradleEnterpriseExtension(gradleEnterpriseOrDevelocity)) {
            return new GradleEnterpriseExtensionAdapter(ProxyFactory.createProxy(gradleEnterpriseOrDevelocity, GradleEnterpriseExtensionProxy.class));
        } else if (!isGradle5OrNewer() && isBuildScanExtension(gradleEnterpriseOrDevelocity)) {
            // Build Scan plugin only exposes the buildScan extension with a limited functionality
            return new BuildScanExtension_1_X_Adapter(ProxyFactory.createProxy(gradleEnterpriseOrDevelocity, BuildScanExtensionProxy.class));
        }

        throw new IllegalArgumentException("Provided extension of type '" + gradleEnterpriseOrDevelocity.getClass().getName() + "' is neither the Develocity configuration nor the Gradle Enterprise extension");
    }

    static boolean isDevelocityConfiguration(Object gradleEnterpriseOrDevelocity) {
        return implementsInterface(gradleEnterpriseOrDevelocity, DEVELOCITY_CONFIGURATION);
    }

    static boolean isGradleEnterpriseExtension(Object gradleEnterpriseOrDevelocity) {
        return implementsInterface(gradleEnterpriseOrDevelocity, GRADLE_ENTERPRISE_EXTENSION);
    }

    static boolean isBuildScanExtension(Object gradleEnterpriseOrDevelocity) {
        return implementsInterface(gradleEnterpriseOrDevelocity, BUILD_SCAN_EXTENSION);
    }

    static boolean implementsInterface(Object object, String interfaceName) {
        Class<?> clazz = object.getClass();
        while (clazz != null) {
            Class<?>[] interfaces = clazz.getInterfaces();
            boolean implementsInterface = Stream.concat(
                Arrays.stream(interfaces),
                Arrays.stream(interfaces).flatMap(it -> Arrays.stream(it.getInterfaces()))
            ).anyMatch(it -> interfaceName.equals(it.getName()));

            if (implementsInterface) {
                return true;
            }

            clazz = clazz.getSuperclass();
        }

        return false;
    }

    BuildScanAdapter getBuildScan();

    void buildScan(Action<? super BuildScanAdapter> action);

    void setServer(@Nullable String server);

    @Nullable
    String getServer();

    void setProjectId(@Nullable String projectId);

    @Nullable
    String getProjectId();

    void setAllowUntrustedServer(boolean allow);

    boolean getAllowUntrustedServer();

    void setAccessKey(@Nullable String accessKey);

    @Nullable
    String getAccessKey();

    @Nullable
    Class<? extends AbstractBuildCache> getBuildCache();

}
