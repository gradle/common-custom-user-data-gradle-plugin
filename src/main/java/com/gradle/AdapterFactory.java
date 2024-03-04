package com.gradle;

import com.gradle.ccud.adapters.BuildCacheAdapter;
import com.gradle.ccud.adapters.DevelocityAdapter;
import com.gradle.ccud.adapters.develocity.DevelocityBuildCacheAdapter;
import com.gradle.ccud.adapters.develocity.DevelocityConfigurationAdapter;
import com.gradle.ccud.adapters.enterprise.BuildScanExtension_1_X_Adapter;
import com.gradle.ccud.adapters.enterprise.GradleEnterpriseBuildCacheAdapter;
import com.gradle.ccud.adapters.enterprise.GradleEnterpriseExtensionAdapter;
import com.gradle.ccud.adapters.enterprise.proxies.GradleEnterpriseBuildCacheProxy;
import com.gradle.ccud.adapters.reflection.ProxyFactory;
import org.gradle.caching.configuration.AbstractBuildCache;

import java.util.Arrays;
import java.util.stream.Stream;

import static com.gradle.Utils.isGradle5OrNewer;

class AdapterFactory {

    private AdapterFactory() {
    }

    private static final String DEVELOCITY_CONFIGURATION = "com.gradle.develocity.agent.gradle.DevelocityConfiguration";
    private static final String GRADLE_ENTERPRISE_EXTENSION = "com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension";
    private static final String BUILD_SCAN_EXTENSION = "com.gradle.scan.plugin.BuildScanExtension";

    static DevelocityAdapter createDevelocityAdapter(Object gradleEnterpriseOrDevelocity) {
        if (isDevelocityConfiguration(gradleEnterpriseOrDevelocity)) {
            return new DevelocityConfigurationAdapter(gradleEnterpriseOrDevelocity);
        } else if (isGradleEnterpriseExtension(gradleEnterpriseOrDevelocity)) {
            return new GradleEnterpriseExtensionAdapter(gradleEnterpriseOrDevelocity);
        } else if (!isGradle5OrNewer() && isBuildScanExtension(gradleEnterpriseOrDevelocity)) {
            // Build Scan plugin only exposes the buildScan extension with a limited functionality
            return new BuildScanExtension_1_X_Adapter(gradleEnterpriseOrDevelocity);
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

    static BuildCacheAdapter createBuildCacheAdapter(AbstractBuildCache cache, Class<? extends AbstractBuildCache> reportedCacheClass) {
        if (reportedCacheClass.getName().toLowerCase().contains("develocity")) {
            return new DevelocityBuildCacheAdapter(cache);
        }

        return new GradleEnterpriseBuildCacheAdapter(ProxyFactory.createProxy(cache, GradleEnterpriseBuildCacheProxy.class));
    }
}
