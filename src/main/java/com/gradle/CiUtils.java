package com.gradle;

import org.gradle.api.provider.ProviderFactory;

final class CiUtils {

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
                || isAzurePipelines(providers);
    }

    static boolean isGenericCI(ProviderFactory providers) {
        return Utils.envVariable("CI", providers).isPresent()
                || Utils.sysProperty("CI", providers).isPresent();
    }

    static boolean isJenkins(ProviderFactory providers) {
        return Utils.envVariable("JENKINS_URL", providers).isPresent();
    }

    static boolean isHudson(ProviderFactory providers) {
        return Utils.envVariable("HUDSON_URL", providers).isPresent();
    }

    static boolean isTeamCity(ProviderFactory providers) {
        return Utils.envVariable("TEAMCITY_VERSION", providers).isPresent();
    }

    static boolean isCircleCI(ProviderFactory providers) {
        return Utils.envVariable("CIRCLE_BUILD_URL", providers).isPresent();
    }

    static boolean isBamboo(ProviderFactory providers) {
        return Utils.envVariable("bamboo_resultsUrl", providers).isPresent();
    }

    static boolean isGitHubActions(ProviderFactory providers) {
        return Utils.envVariable("GITHUB_ACTIONS", providers).isPresent();
    }

    static boolean isGitLab(ProviderFactory providers) {
        return Utils.envVariable("GITLAB_CI", providers).isPresent();
    }

    static boolean isTravis(ProviderFactory providers) {
        return Utils.envVariable("TRAVIS_JOB_ID", providers).isPresent();
    }

    static boolean isBitrise(ProviderFactory providers) {
        return Utils.envVariable("BITRISE_BUILD_URL", providers).isPresent();
    }

    static boolean isGoCD(ProviderFactory providers) {
        return Utils.envVariable("GO_SERVER_URL", providers).isPresent();
    }

    static boolean isAzurePipelines(ProviderFactory providers) {
        return Utils.envVariable("TF_BUILD", providers).isPresent();
    }

}
