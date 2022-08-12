package com.gradle;

import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension;
import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.caching.configuration.BuildCacheConfiguration;
import org.gradle.util.GradleVersion;

import javax.inject.Inject;
import java.util.Arrays;

public class CommonCustomUserDataGradlePlugin implements Plugin<Object> {

    private final ProviderFactory providers;

    @Inject
    public CommonCustomUserDataGradlePlugin(ProviderFactory providers) {
        this.providers = providers;
    }

    public void apply(Object target) {
        if (target instanceof Settings) {
            if (!isGradle6OrNewer()) {
                throw new GradleException("For Gradle versions prior to 6.0, common-custom-user-data-gradle-plugin must be applied to the Root project");
            } else {
                applySettingsPlugin((Settings) target);
            }
        } else if (target instanceof Project) {
            if (isGradle6OrNewer()) {
                throw new GradleException("For Gradle versions 6.0 and newer, common-custom-user-data-gradle-plugin must be applied to Settings");
            } else if (isGradle5OrNewer()) {
                applyProjectPluginGradle5((Project) target);
            } else if (isGradle4OrNewer()) {
                applyProjectPluginGradle4((Project) target);
            } else {
                throw new GradleException("For Gradle versions prior to 4.0, common-custom-user-data-gradle-plugin is not supported");
            }
        }
    }

    public static void apply(Object gradleEnterprise, ProviderFactory providers, Settings settings) {
        applySettingsPlugin(
                ProxyFactory.createProxy(gradleEnterprise, GradleEnterpriseExtension.class),
                providers,
                settings
        );
    }

    private static void applySettingsPlugin(GradleEnterpriseExtension gradleEnterprise, ProviderFactory providers, Settings settings) {
        CustomGradleEnterpriseConfig customGradleEnterpriseConfig = new CustomGradleEnterpriseConfig();

        customGradleEnterpriseConfig.configureGradleEnterprise(gradleEnterprise);

        BuildScanExtension buildScan = gradleEnterprise.getBuildScan();
        customGradleEnterpriseConfig.configureBuildScanPublishing(buildScan);
        CustomBuildScanEnhancements buildScanEnhancements = new CustomBuildScanEnhancements(buildScan, providers, settings.getGradle());
        buildScanEnhancements.apply();

        BuildCacheConfiguration buildCache = settings.getBuildCache();
        customGradleEnterpriseConfig.configureBuildCache(buildCache);

        // configuration changes applied in this block will override earlier configuration settings,
        // including those set in the settings.gradle(.kts)
        Action<Settings> settingsAction = __ -> {
            Overrides overrides = new Overrides(providers);
            overrides.configureGradleEnterprise(gradleEnterprise);
            overrides.configureBuildCache(buildCache);
        };

        // it is possible that the settings have already been evaluated by now, in which case
        // a settingsEvaluated callback would not fire anymore
        if (settingsHaveBeenEvaluated()) {
            settingsAction.execute(settings);
        } else {
            settings.getGradle().settingsEvaluated(settingsAction);
        }
    }

    private void applySettingsPlugin(Settings settings) {
        settings.getPluginManager().withPlugin("com.gradle.enterprise", __ -> applySettingsPlugin(ProxyFactory.createProxy(settings.getExtensions().getByName("gradleEnterprise"), GradleEnterpriseExtension.class), providers, settings));
    }

    private void applyProjectPluginGradle5(Project project) {
        ensureRootProject(project);
        project.getPluginManager().withPlugin("com.gradle.build-scan", __ -> {
            CustomGradleEnterpriseConfig customGradleEnterpriseConfig = new CustomGradleEnterpriseConfig();

            Object extension = project.getExtensions().getByName("gradleEnterprise");
            GradleEnterpriseExtension gradleEnterprise = ProxyFactory.createProxy(extension, GradleEnterpriseExtension.class);
            customGradleEnterpriseConfig.configureGradleEnterprise(gradleEnterprise);

            BuildScanExtension buildScan = gradleEnterprise.getBuildScan();
            customGradleEnterpriseConfig.configureBuildScanPublishing(buildScan);
            CustomBuildScanEnhancements buildScanEnhancements = new CustomBuildScanEnhancements(buildScan, providers, project.getGradle());
            buildScanEnhancements.apply();

            // Build cache configuration cannot be accessed from a project plugin

            // configuration changes applied within this block will override earlier configuration settings,
            // including those set in the root project's build.gradle(.kts)
            project.afterEvaluate(___ -> {
                Overrides overrides = new Overrides(providers);
                overrides.configureGradleEnterprise(gradleEnterprise);
            });
        });
    }

    private void applyProjectPluginGradle4(Project project) {
        ensureRootProject(project);
        project.getPluginManager().withPlugin("com.gradle.build-scan", __ -> {
            CustomGradleEnterpriseConfig customGradleEnterpriseConfig = new CustomGradleEnterpriseConfig();

            Object extension = project.getExtensions().getByName("buildScan");
            BuildScanExtension buildScan = ProxyFactory.createProxy(extension, BuildScanExtension.class);
            customGradleEnterpriseConfig.configureGradleEnterpriseOnGradle4(buildScan);

            customGradleEnterpriseConfig.configureBuildScanPublishingOnGradle4(buildScan);
            CustomBuildScanEnhancements buildScanEnhancements = new CustomBuildScanEnhancements(buildScan, providers, project.getGradle());
            buildScanEnhancements.apply();

            // Build cache configuration cannot be accessed from a project plugin

            // configuration changes applied within this block will override earlier configuration settings,
            // including those set in the root project's build.gradle(.kts)
            project.afterEvaluate(___ -> {
                Overrides overrides = new Overrides(providers);
                overrides.configureGradleEnterpriseOnGradle4(buildScan);
            });
        });
    }

    private static boolean isGradle6OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("6.0")) >= 0;
    }

    private static boolean isGradle5OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("5.0")) >= 0;
    }

    private static boolean isGradle4OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("4.0")) >= 0;
    }

    private static void ensureRootProject(Project project) {
        if (!project.equals(project.getRootProject())) {
            throw new GradleException("common-custom-user-data-gradle-plugin may only be applied to the Root project");
        }
    }

    private static boolean settingsHaveBeenEvaluated() {
        return Arrays.stream(Thread.currentThread().getStackTrace())
                .map(StackTraceElement::getMethodName)
                .anyMatch(s -> s.contains("settingsEvaluated"));
    }

}
