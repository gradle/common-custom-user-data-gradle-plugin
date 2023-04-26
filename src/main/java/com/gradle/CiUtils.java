package com.gradle;

import org.gradle.api.provider.ProviderFactory;

import static com.gradle.Utils.envVariable;
import static com.gradle.Utils.sysProperty;

final class CiUtils {

    private CiUtils() {
    }

    static boolean isCi(ProviderFactory providers) {
        return isGenericCI(providers)
                || isJenkins(providers)
                || isHudson(providers)
                || isTeamCity(providers)
                || isCircleCI(providers)
                || isBamboo(providers)
                || isGitHubActions(providers)
                || isGitLab(providers)
                || isTravis(providers)
                || isBitrise(providers)
                || isGoCD(providers)
                || isAzurePipelines(providers)
                || isBuildkite(providers);
    }

    static boolean isGenericCI(ProviderFactory providers) {
        return envVariable("CI", providers).isPresent()
                || sysProperty("CI", providers).isPresent();
    }

    static boolean isJenkins(ProviderFactory providers) {
        return envVariable("JENKINS_URL", providers).isPresent();
    }

    static boolean isHudson(ProviderFactory providers) {
        return envVariable("HUDSON_URL", providers).isPresent();
    }

    static boolean isTeamCity(ProviderFactory providers) {
        return envVariable("TEAMCITY_VERSION", providers).isPresent();
    }

    static boolean isCircleCI(ProviderFactory providers) {
        return envVariable("CIRCLE_BUILD_URL", providers).isPresent();
    }

    static boolean isBamboo(ProviderFactory providers) {
        return envVariable("bamboo_resultsUrl", providers).isPresent();
    }

    static boolean isGitHubActions(ProviderFactory providers) {
        return envVariable("GITHUB_ACTIONS", providers).isPresent();
    }

    static boolean isGitLab(ProviderFactory providers) {
        return envVariable("GITLAB_CI", providers).isPresent();
    }

    static boolean isTravis(ProviderFactory providers) {
        return envVariable("TRAVIS_JOB_ID", providers).isPresent();
    }

    static boolean isBitrise(ProviderFactory providers) {
        return envVariable("BITRISE_BUILD_URL", providers).isPresent();
    }

    static boolean isGoCD(ProviderFactory providers) {
        return envVariable("GO_SERVER_URL", providers).isPresent();
    }

    static boolean isAzurePipelines(ProviderFactory providers) {
        return envVariable("TF_BUILD", providers).isPresent();
    }

    static boolean isBuildkite(ProviderFactory providers) {
        return envVariable("BUILDKITE", providers).isPresent();
    }

}
