package com.gradle;

import static com.gradle.Utils.envVariable;
import static com.gradle.Utils.sysProperty;

final class CiUtils {

    private CiUtils() {
    }

    static boolean isCi() {
        return isGenericCI()
                || isJenkins()
                || isHudson()
                || isTeamCity()
                || isCircleCI()
                || isBamboo()
                || isGitHubActions()
                || isGitLab()
                || isTravis()
                || isBitrise()
                || isGoCD()
                || isAzurePipelines()
                || isBuildkite();
    }

    static boolean isGenericCI() {
        return envVariable("CI").isPresent()
                || sysProperty("CI").isPresent();
    }

    static boolean isJenkins() {
        return envVariable("JENKINS_URL").isPresent();
    }

    static boolean isHudson() {
        return envVariable("HUDSON_URL").isPresent();
    }

    static boolean isTeamCity() {
        return envVariable("TEAMCITY_VERSION").isPresent();
    }

    static boolean isCircleCI() {
        return envVariable("CIRCLE_BUILD_URL").isPresent();
    }

    static boolean isBamboo() {
        return envVariable("bamboo_resultsUrl").isPresent();
    }

    static boolean isGitHubActions() {
        return envVariable("GITHUB_ACTIONS").isPresent();
    }

    static boolean isGitLab() {
        return envVariable("GITLAB_CI").isPresent();
    }

    static boolean isTravis() {
        return envVariable("TRAVIS_JOB_ID").isPresent();
    }

    static boolean isBitrise() {
        return envVariable("BITRISE_BUILD_URL").isPresent();
    }

    static boolean isGoCD() {
        return envVariable("GO_SERVER_URL").isPresent();
    }

    static boolean isAzurePipelines() {
        return envVariable("TF_BUILD").isPresent();
    }

    static boolean isBuildkite() {
        return envVariable("BUILDKITE").isPresent();
    }

}
