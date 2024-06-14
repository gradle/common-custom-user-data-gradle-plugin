package com.gradle;

import com.gradle.develocity.agent.gradle.adapters.BuildScanAdapter;
import com.gradle.develocity.agent.gradle.adapters.DevelocityAdapter;
import com.gradle.develocity.agent.gradle.adapters.develocity.DevelocityConfigurationAdapter;
import com.gradle.develocity.agent.gradle.adapters.enterprise.BuildScanExtension_1_X_Adapter;
import com.gradle.develocity.agent.gradle.adapters.enterprise.GradleEnterpriseExtensionAdapter;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.caching.configuration.BuildCacheConfiguration;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.gradle.Utils.isGradle4OrNewer;
import static com.gradle.Utils.isGradle5OrNewer;
import static com.gradle.Utils.isGradle6OrNewer;

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

    public static void apply(Object develocityConfiguration, ProviderFactory providers, Settings settings) {
        applySettingsPlugin(
            new DevelocityConfigurationAdapter(develocityConfiguration),
            providers,
            settings
        );
    }

    private static void applySettingsPlugin(DevelocityAdapter develocity, ProviderFactory providers, Settings settings) {
        CustomDevelocityConfig customDevelocityConfig = new CustomDevelocityConfig();

        customDevelocityConfig.configureDevelocity(develocity);

        BuildScanAdapter buildScan = develocity.getBuildScan();
        customDevelocityConfig.configureBuildScanPublishing(buildScan);
        CustomBuildScanEnhancements buildScanEnhancements = new CustomBuildScanEnhancements(develocity, providers, settings.getGradle());
        buildScanEnhancements.apply();

        BuildCacheConfiguration buildCache = settings.getBuildCache();
        customDevelocityConfig.configureBuildCache(buildCache);

        // configuration changes applied in this block will override earlier configuration settings,
        // including those set in the settings.gradle(.kts)
        Action<Settings> settingsAction = __ -> {
            Overrides overrides = new Overrides(providers);
            overrides.configureDevelocity(develocity);
            overrides.configureBuildCache(buildCache, develocity.getBuildCache());
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
        AtomicBoolean somePluginAlreadyConfigured = new AtomicBoolean(false);
        settings.getPluginManager().withPlugin("com.gradle.develocity", __ -> {
            if (somePluginAlreadyConfigured.compareAndSet(false, true)) {
                applySettingsPlugin(createDevelocityAdapter(settings), providers, settings);
            }
        });
        settings.getPluginManager().withPlugin("com.gradle.enterprise", __ -> {
            if (somePluginAlreadyConfigured.compareAndSet(false, true)) {
                applySettingsPlugin(createDevelocityAdapter(settings), providers, settings);
            }
        });
    }

    private void applyProjectPluginGradle5(Project project) {
        ensureRootProject(project);
        AtomicBoolean somePluginAlreadyConfigured = new AtomicBoolean(false);
        project.getPluginManager().withPlugin("com.gradle.develocity", __ -> {
            if (somePluginAlreadyConfigured.compareAndSet(false, true)) {
                applyProjectPlugin(project, createDevelocityAdapter(project));
            }
        });
        project.getPluginManager().withPlugin("com.gradle.build-scan", __ -> {
            if (somePluginAlreadyConfigured.compareAndSet(false, true)) {
                applyProjectPlugin(project, createDevelocityAdapter(project));
            }
        });
    }

    private static <T extends ExtensionAware> DevelocityAdapter createDevelocityAdapter(T target) {
        ExtensionContainer extensions = target.getExtensions();

        Object develocityExtension = extensions.findByName("develocity");
        if (develocityExtension != null) {
            return new DevelocityConfigurationAdapter(develocityExtension);
        }

        return new GradleEnterpriseExtensionAdapter(extensions.getByName("gradleEnterprise"));
    }

    private void applyProjectPluginGradle4(Project project) {
        ensureRootProject(project);
        project.getPluginManager().withPlugin("com.gradle.build-scan",
            __ -> applyProjectPlugin(project, new BuildScanExtension_1_X_Adapter(project.getExtensions().getByName("buildScan"))));
    }

    private void applyProjectPlugin(Project project, DevelocityAdapter develocity) {
        CustomDevelocityConfig customDevelocityConfig = new CustomDevelocityConfig();

        customDevelocityConfig.configureDevelocity(develocity);

        BuildScanAdapter buildScan = develocity.getBuildScan();
        customDevelocityConfig.configureBuildScanPublishing(buildScan);
        CustomBuildScanEnhancements buildScanEnhancements = new CustomBuildScanEnhancements(develocity, providers, project.getGradle());
        buildScanEnhancements.apply();

        // Build cache configuration cannot be accessed from a project plugin

        // configuration changes applied within this block will override earlier configuration settings,
        // including those set in the root project's build.gradle(.kts)
        project.afterEvaluate(___ -> {
            Overrides overrides = new Overrides(providers);
            overrides.configureDevelocity(develocity);
        });
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
