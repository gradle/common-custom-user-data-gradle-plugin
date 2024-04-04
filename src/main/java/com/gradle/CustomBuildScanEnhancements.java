package com.gradle;

import com.gradle.develocity.agent.gradle.adapters.BuildResultAdapter;
import com.gradle.develocity.agent.gradle.adapters.BuildScanAdapter;
import com.gradle.develocity.agent.gradle.adapters.DevelocityAdapter;
import org.gradle.api.Action;
import org.gradle.api.file.Directory;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

import java.net.URI;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.gradle.CiUtils.isAzurePipelines;
import static com.gradle.CiUtils.isBamboo;
import static com.gradle.CiUtils.isBitrise;
import static com.gradle.CiUtils.isBuildkite;
import static com.gradle.CiUtils.isCi;
import static com.gradle.CiUtils.isCircleCI;
import static com.gradle.CiUtils.isGitHubActions;
import static com.gradle.CiUtils.isGitLab;
import static com.gradle.CiUtils.isGoCD;
import static com.gradle.CiUtils.isHudson;
import static com.gradle.CiUtils.isJenkins;
import static com.gradle.CiUtils.isTeamCity;
import static com.gradle.CiUtils.isTravis;
import static com.gradle.Utils.appendIfMissing;
import static com.gradle.Utils.envVariable;
import static com.gradle.Utils.execAndCheckSuccess;
import static com.gradle.Utils.execAndGetStdOut;
import static com.gradle.Utils.isGradle43rNewer;
import static com.gradle.Utils.isGradle56OrNewer;
import static com.gradle.Utils.isGradle61OrNewer;
import static com.gradle.Utils.isGradle62OrNewer;
import static com.gradle.Utils.isNotEmpty;
import static com.gradle.Utils.readPropertiesFile;
import static com.gradle.Utils.redactUserInfo;
import static com.gradle.Utils.sysProperty;
import static com.gradle.Utils.toWebRepoUri;
import static com.gradle.Utils.urlEncode;

/**
 * Adds a standard set of useful tags, links and custom values to all build scans published.
 */
final class CustomBuildScanEnhancements {

    private static final String SYSTEM_PROP_IDEA_VENDOR_NAME = "idea.vendor.name";
    private static final String SYSTEM_PROP_IDEA_VERSION = "idea.version";
    private static final String PROJECT_PROP_ANDROID_INVOKED_FROM_IDE = "android.injected.invoked.from.ide";
    private static final String PROJECT_PROP_ANDROID_STUDIO_VERSION_LEGACY = "android.injected.studio.version";
    private static final String PROJECT_PROP_ANDROID_STUDIO_VERSION = "android.studio.version";
    private static final String SYSTEM_PROP_ECLIPSE_BUILD_ID = "eclipse.buildId";
    private static final String SYSTEM_PROP_IDEA_SYNC_ACTIVE = "idea.sync.active";

    private final DevelocityAdapter develocity;
    private final BuildScanAdapter buildScan;
    private final ProviderFactory providers;
    private final Gradle gradle;

    CustomBuildScanEnhancements(DevelocityAdapter develocity, ProviderFactory providers, Gradle gradle) {
        this.develocity = develocity;
        this.buildScan = develocity.getBuildScan();
        this.providers = providers;
        this.gradle = gradle;
    }

    // Apply all build scan enhancements via custom tags, links, and values
    void apply() {
        captureOs();
        captureIde();
        captureCiOrLocal();
        captureCiMetadata();
        captureGitMetadata();
    }

    private void captureOs() {
        sysProperty("os.name", providers).ifPresent(buildScan::tag);
    }

    private void captureIde() {
        if (!isCi(providers)) {
            // Prepare relevant properties for use at execution time
            Map<String, Provider<String>> ideProperties = new HashMap<>();
            ideProperties.put(SYSTEM_PROP_IDEA_VENDOR_NAME, systemPropertyProvider(SYSTEM_PROP_IDEA_VENDOR_NAME, providers));
            ideProperties.put(SYSTEM_PROP_IDEA_VERSION, systemPropertyProvider(SYSTEM_PROP_IDEA_VERSION, providers));
            ideProperties.put(PROJECT_PROP_ANDROID_INVOKED_FROM_IDE, gradlePropertyProvider(PROJECT_PROP_ANDROID_INVOKED_FROM_IDE, gradle, providers));
            ideProperties.put(PROJECT_PROP_ANDROID_STUDIO_VERSION, firstOrElseSecond(providers, gradlePropertyProvider(PROJECT_PROP_ANDROID_STUDIO_VERSION, gradle, providers), gradlePropertyProvider(PROJECT_PROP_ANDROID_STUDIO_VERSION_LEGACY, gradle, providers)));
            ideProperties.put(SYSTEM_PROP_ECLIPSE_BUILD_ID, systemPropertyProvider(SYSTEM_PROP_ECLIPSE_BUILD_ID, providers));
            ideProperties.put(SYSTEM_PROP_IDEA_SYNC_ACTIVE, systemPropertyProvider(SYSTEM_PROP_IDEA_SYNC_ACTIVE, providers));

            // Process data at execution time to ensure property initialization
            buildScan.buildFinished(new CaptureIdeMetadataAction(buildScan, ideProperties));
        }
    }

    private static final class CaptureIdeMetadataAction implements Action<BuildResultAdapter> {

        private final BuildScanAdapter buildScan;
        private final Map<String, Provider<String>> props;

        private CaptureIdeMetadataAction(BuildScanAdapter buildScan, Map<String, Provider<String>> props) {
            this.buildScan = buildScan;
            this.props = props;
        }

        @Override
        public void execute(BuildResultAdapter buildResult) {
            if (props.get(SYSTEM_PROP_IDEA_VENDOR_NAME).isPresent()) {
                String ideaVendorNameValue = props.get(SYSTEM_PROP_IDEA_VENDOR_NAME).get();
                if ("Google".equals(ideaVendorNameValue)) {
                    // using androidStudioVersion instead of ideaVersion for compatibility reasons, those can be different (e.g. 2020.3.1 Patch 3 instead of 2020.3)
                    tagIde("Android Studio", getOrEmpty(props.get(PROJECT_PROP_ANDROID_STUDIO_VERSION)));
                } else if ("JetBrains".equals(ideaVendorNameValue)) {
                    tagIde("IntelliJ IDEA", getOrEmpty(props.get(SYSTEM_PROP_IDEA_VERSION)));
                }
            } else if (props.get(PROJECT_PROP_ANDROID_INVOKED_FROM_IDE).isPresent()) {
                // this case should be handled by the ideaVendorName condition but keeping it for compatibility reason (ideaVendorName started with 2020.1)
                tagIde("Android Studio", getOrEmpty(props.get(PROJECT_PROP_ANDROID_STUDIO_VERSION)));
            } else if (props.get(SYSTEM_PROP_IDEA_VERSION).isPresent()) {
                // this case should be handled by the ideaVendorName condition but keeping it for compatibility reason (ideaVendorName started with 2020.1)
                tagIde("IntelliJ IDEA", props.get(SYSTEM_PROP_IDEA_VERSION).get());
            } else if (props.get(SYSTEM_PROP_ECLIPSE_BUILD_ID).isPresent()) {
                tagIde("Eclipse", props.get(SYSTEM_PROP_ECLIPSE_BUILD_ID).get());
            } else {
                buildScan.tag("Cmd Line");
            }

            if (props.get(SYSTEM_PROP_IDEA_SYNC_ACTIVE).isPresent()) {
                buildScan.tag("IDE sync");
            }
        }

        private String getOrEmpty(Provider<String> p) {
            if (isGradle43rNewer()) {
                return p.getOrElse("");
            } else {
                String value = p.getOrNull();
                return value != null ? value : "";
            }
        }

        private void tagIde(String ideLabel, String version) {
            buildScan.tag(ideLabel);
            if (!version.isEmpty()) {
                buildScan.value(ideLabel + " version", version);
            }
        }

    }

    private void captureCiOrLocal() {
        buildScan.tag(isCi(providers) ? "CI" : "LOCAL");
    }

    private void captureCiMetadata() {
        if (isCi(providers)) {
            // Prepare project directory for use at execution time
            Provider<Directory> projectDirectory = providers.provider(() -> gradle.getRootProject().getLayout().getProjectDirectory());

            // Process data at execution time not to have a CI environment variable (ie. build number) invalidates the configuration cache
            buildScan.buildFinished(new CaptureCiMetadataAction(develocity, providers, projectDirectory));
        }
    }

    private static final class CaptureCiMetadataAction implements Action<BuildResultAdapter> {

        private final DevelocityAdapter develocity;
        private final BuildScanAdapter buildScan;
        private final ProviderFactory providers;
        private final Provider<Directory> projectDirectory;

        private CaptureCiMetadataAction(DevelocityAdapter develocity, ProviderFactory providers, Provider<Directory> projectDirectory) {
            this.develocity = develocity;
            this.buildScan = develocity.getBuildScan();
            this.providers = providers;
            this.projectDirectory = projectDirectory;
        }

        @Override
        public void execute(BuildResultAdapter buildResult) {
            if (isJenkins(providers) || isHudson(providers)) {
                String ciProvider = isJenkins(providers) ? "Jenkins" : "Hudson";
                String controllerUrlEnvVar = isJenkins(providers) ? "JENKINS_URL" : "HUDSON_URL";

                Optional<String> buildUrl = envVariable("BUILD_URL", providers);
                Optional<String> buildNumber = envVariable("BUILD_NUMBER", providers);
                Optional<String> nodeName = envVariable("NODE_NAME", providers);
                Optional<String> jobName = envVariable("JOB_NAME", providers);
                Optional<String> stageName = envVariable("STAGE_NAME", providers);
                Optional<String> controllerUrl = envVariable(controllerUrlEnvVar, providers);

                buildScan.value("CI provider", ciProvider);
                buildUrl.ifPresent(url ->
                    buildScan.link(isJenkins(providers) ? "Jenkins build" : "Hudson build", url));
                buildNumber.ifPresent(value ->
                    buildScan.value("CI build number", value));
                nodeName.ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI node", value));
                jobName.ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI job", value));
                stageName.ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI stage", value));
                controllerUrl.ifPresent(value ->
                    buildScan.value("CI controller", value));

                jobName.ifPresent(j -> buildNumber.ifPresent(b -> {
                    Map<String, String> params = new LinkedHashMap<>();
                    params.put("CI job", j);
                    params.put("CI build number", b);
                    addSearchLink(develocity, "CI pipeline", params);
                }));
            }

            if (isTeamCity(providers)) {
                buildScan.value("CI provider", "TeamCity");
                Optional<String> teamcityBuildPropertiesFile = envVariable("TEAMCITY_BUILD_PROPERTIES_FILE", providers);
                if (teamcityBuildPropertiesFile.isPresent()) {
                    Properties buildProperties = readPropertiesFile(teamcityBuildPropertiesFile.get(), providers, projectDirectory.get());

                    String teamCityBuildId = buildProperties.getProperty("teamcity.build.id");
                    if (isNotEmpty(teamCityBuildId)) {
                        String teamcityConfigFile = buildProperties.getProperty("teamcity.configuration.properties.file");
                        if (isNotEmpty(teamcityConfigFile)) {
                            Properties configProperties = readPropertiesFile(teamcityConfigFile, providers, projectDirectory.get());

                            String teamCityServerUrl = configProperties.getProperty("teamcity.serverUrl");
                            if (isNotEmpty(teamCityServerUrl)) {
                                String buildUrl = appendIfMissing(teamCityServerUrl, '/') + "viewLog.html?buildId=" + urlEncode(teamCityBuildId);
                                buildScan.link("TeamCity build", buildUrl);
                            }
                        }
                    }

                    String teamCityBuildNumber = buildProperties.getProperty("build.number");
                    if (isNotEmpty(teamCityBuildNumber)) {
                        buildScan.value("CI build number", teamCityBuildNumber);
                    }
                    String teamCityBuildTypeId = buildProperties.getProperty("teamcity.buildType.id");
                    if (isNotEmpty(teamCityBuildTypeId)) {
                        addCustomValueAndSearchLink(develocity, "CI build config", teamCityBuildTypeId);
                    }
                    String teamCityAgentName = buildProperties.getProperty("agent.name");
                    if (isNotEmpty(teamCityAgentName)) {
                        addCustomValueAndSearchLink(develocity, "CI agent", teamCityAgentName);
                    }
                }
            }

            if (isCircleCI(providers)) {
                buildScan.value("CI provider", "CircleCI");
                envVariable("CIRCLE_BUILD_URL", providers).ifPresent(url ->
                    buildScan.link("CircleCI build", url));
                envVariable("CIRCLE_BUILD_NUM", providers).ifPresent(value ->
                    buildScan.value("CI build number", value));
                envVariable("CIRCLE_JOB", providers).ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI job", value));
                envVariable("CIRCLE_WORKFLOW_ID", providers).ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI workflow", value));
            }

            if (isBamboo(providers)) {
                buildScan.value("CI provider", "Bamboo");
                envVariable("bamboo_resultsUrl", providers).ifPresent(url ->
                    buildScan.link("Bamboo build", url));
                envVariable("bamboo_buildNumber", providers).ifPresent(value ->
                    buildScan.value("CI build number", value));
                envVariable("bamboo_planName", providers).ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI plan", value));
                envVariable("bamboo_buildPlanName", providers).ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI build plan", value));
                envVariable("bamboo_agentId", providers).ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI agent", value));
            }

            if (isGitHubActions(providers)) {
                buildScan.value("CI provider", "GitHub Actions");
                Optional<String> gitHubUrl = envVariable("GITHUB_SERVER_URL", providers);
                Optional<String> gitRepository = envVariable("GITHUB_REPOSITORY", providers);
                Optional<String> gitHubRunId = envVariable("GITHUB_RUN_ID", providers);
                if (gitHubUrl.isPresent() && gitRepository.isPresent() && gitHubRunId.isPresent()) {
                    buildScan.link("GitHub Actions build", gitHubUrl.get() + "/" + gitRepository.get() + "/actions/runs/" + gitHubRunId.get());
                }
                envVariable("GITHUB_WORKFLOW", providers).ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI workflow", value));
                envVariable("GITHUB_RUN_ID", providers).ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI run", value));
                envVariable("GITHUB_HEAD_REF", providers).filter(value -> !value.isEmpty()).ifPresent(value ->
                    buildScan.value("PR branch", value));
            }

            if (isGitLab(providers)) {
                buildScan.value("CI provider", "GitLab");
                envVariable("CI_JOB_URL", providers).ifPresent(url ->
                    buildScan.link("GitLab build", url));
                envVariable("CI_PIPELINE_URL", providers).ifPresent(url ->
                    buildScan.link("GitLab pipeline", url));
                envVariable("CI_JOB_NAME", providers).ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI job", value));
                envVariable("CI_JOB_STAGE", providers).ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI stage", value));
            }

            if (isTravis(providers)) {
                buildScan.value("CI provider", "Travis");
                envVariable("TRAVIS_BUILD_WEB_URL", providers).ifPresent(url ->
                    buildScan.link("Travis build", url));
                envVariable("TRAVIS_BUILD_NUMBER", providers).ifPresent(value ->
                    buildScan.value("CI build number", value));
                envVariable("TRAVIS_JOB_NAME", providers).ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI job", value));
                envVariable("TRAVIS_EVENT_TYPE", providers).ifPresent(buildScan::tag);
            }

            if (isBitrise(providers)) {
                buildScan.value("CI provider", "Bitrise");
                envVariable("BITRISE_BUILD_URL", providers).ifPresent(url ->
                    buildScan.link("Bitrise build", url));
                envVariable("BITRISE_BUILD_NUMBER", providers).ifPresent(value ->
                    buildScan.value("CI build number", value));
            }

            if (isGoCD(providers)) {
                buildScan.value("CI provider", "GoCD");
                Optional<String> pipelineName = envVariable("GO_PIPELINE_NAME", providers);
                Optional<String> pipelineNumber = envVariable("GO_PIPELINE_COUNTER", providers);
                Optional<String> stageName = envVariable("GO_STAGE_NAME", providers);
                Optional<String> stageNumber = envVariable("GO_STAGE_COUNTER", providers);
                Optional<String> jobName = envVariable("GO_JOB_NAME", providers);
                Optional<String> goServerUrl = envVariable("GO_SERVER_URL", providers);
                if (Stream.of(pipelineName, pipelineNumber, stageName, stageNumber, jobName, goServerUrl).allMatch(Optional::isPresent)) {
                    //noinspection OptionalGetWithoutIsPresent
                    String buildUrl = String.format("%s/tab/build/detail/%s/%s/%s/%s/%s",
                        goServerUrl.get(), pipelineName.get(),
                        pipelineNumber.get(), stageName.get(), stageNumber.get(), jobName.get());
                    buildScan.link("GoCD build", buildUrl);
                } else if (goServerUrl.isPresent()) {
                    buildScan.link("GoCD", goServerUrl.get());
                }
                pipelineName.ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI pipeline", value));
                jobName.ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI job", value));
                stageName.ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI stage", value));
            }

            if (isAzurePipelines(providers)) {
                buildScan.value("CI provider", "Azure Pipelines");
                Optional<String> azureServerUrl = envVariable("SYSTEM_TEAMFOUNDATIONCOLLECTIONURI", providers);
                Optional<String> azureProject = envVariable("SYSTEM_TEAMPROJECT", providers);
                Optional<String> buildId = envVariable("BUILD_BUILDID", providers);
                if (Stream.of(azureServerUrl, azureProject, buildId).allMatch(Optional::isPresent)) {
                    String buildUrl = String.format("%s%s/_build/results?buildId=%s",
                        azureServerUrl.get(), azureProject.get(), buildId.get());
                    buildScan.link("Azure Pipelines build", buildUrl);
                } else if (azureServerUrl.isPresent()) {
                    buildScan.link("Azure Pipelines", azureServerUrl.get());
                }

                buildId.ifPresent(value ->
                    buildScan.value("CI build number", value));
            }

            if (isBuildkite(providers)) {
                buildScan.value("CI provider", "Buildkite");
                envVariable("BUILDKITE_BUILD_URL", providers)
                    .ifPresent(s -> buildScan.link("Buildkite build", s));
                envVariable("BUILDKITE_COMMAND", providers).ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI command", value));
                envVariable("BUILDKITE_BUILD_ID", providers).ifPresent(value ->
                    buildScan.value("CI build ID", value));

                Optional<String> buildkitePrRepo = envVariable("BUILDKITE_PULL_REQUEST_REPO", providers);
                Optional<String> buildkitePrNumber = envVariable("BUILDKITE_PULL_REQUEST", providers);
                if (buildkitePrRepo.isPresent() && buildkitePrNumber.isPresent()) {
                    String prNumber = buildkitePrNumber.get();
                    toWebRepoUri(buildkitePrRepo.get())
                        .ifPresent(s -> buildScan.link("PR source", s + "/pull/" + prNumber));
                }
            }
        }

    }

    private void captureGitMetadata() {
        // Run expensive computation in background
        buildScan.background(new CaptureGitMetadataAction(providers, develocity));
    }

    private static final class CaptureGitMetadataAction implements Action<BuildScanAdapter> {

        private final ProviderFactory providers;
        private final DevelocityAdapter develocity;

        private CaptureGitMetadataAction(ProviderFactory providers, DevelocityAdapter develocity) {
            this.providers = providers;
            this.develocity = develocity;
        }

        @Override
        public void execute(BuildScanAdapter buildScan) {
            if (!isGitInstalled()) {
                return;
            }

            String gitRepo = execAndGetStdOut("git", "config", "--get", "remote.origin.url");
            String gitCommitId = execAndGetStdOut("git", "rev-parse", "--verify", "HEAD");
            String gitCommitShortId = execAndGetStdOut("git", "rev-parse", "--short=8", "--verify", "HEAD");
            String gitBranchName = getGitBranchName(() -> execAndGetStdOut("git", "rev-parse", "--abbrev-ref", "HEAD"));
            String gitStatus = execAndGetStdOut("git", "status", "--porcelain");

            if (isNotEmpty(gitRepo)) {
                buildScan.value("Git repository", redactUserInfo(gitRepo));
            }
            if (isNotEmpty(gitCommitId)) {
                buildScan.value("Git commit id", gitCommitId);
            }
            if (isNotEmpty(gitCommitShortId)) {
                // Ensure server URL is configured by deferring call at execution time
                buildScan.buildFinished(result -> addCustomValueAndSearchLink(develocity, "Git commit id", "Git commit id short", gitCommitShortId));
            }
            if (isNotEmpty(gitBranchName)) {
                buildScan.tag(gitBranchName);
                buildScan.value("Git branch", gitBranchName);
            }
            if (isNotEmpty(gitStatus)) {
                buildScan.tag("Dirty");
                buildScan.value("Git status", gitStatus);
            }

            Optional<String> gitHubUrl = envVariable("GITHUB_SERVER_URL", providers);
            Optional<String> gitRepository = envVariable("GITHUB_REPOSITORY", providers);
            if (gitHubUrl.isPresent() && gitRepository.isPresent() && isNotEmpty(gitCommitId)) {
                buildScan.link("GitHub source", gitHubUrl.get() + "/" + gitRepository.get() + "/tree/" + gitCommitId);
            } else if (isNotEmpty(gitRepo) && isNotEmpty(gitCommitId)) {
                Optional<URI> webRepoUri = toWebRepoUri(gitRepo);
                webRepoUri.ifPresent(uri -> {
                    if (uri.getHost().contains("github")) {
                        buildScan.link("GitHub source", uri + "/tree/" + gitCommitId);
                    } else if (uri.getHost().contains("gitlab")) {
                        buildScan.link("GitLab source", uri + "/-/commit/" + gitCommitId);
                    }
                });
            }
        }

        private boolean isGitInstalled() {
            return execAndCheckSuccess("git", "--version");
        }

        private String getGitBranchName(Supplier<String> gitCommand) {
            if (isJenkins(providers) || isHudson(providers)) {
                Optional<String> branchName = envVariable("BRANCH_NAME", providers);
                if (branchName.isPresent()) {
                    return branchName.get();
                }

                Optional<String> gitBranch = envVariable("GIT_BRANCH", providers);
                if (gitBranch.isPresent()) {
                    Optional<String> localBranch = getLocalBranch(gitBranch.get());
                    if (localBranch.isPresent()) {
                        return localBranch.get();
                    }
                }
            } else if (isGitLab(providers)) {
                Optional<String> branch = envVariable("CI_COMMIT_REF_NAME", providers);
                if (branch.isPresent()) {
                    return branch.get();
                }
            } else if (isAzurePipelines(providers)) {
                Optional<String> branch = envVariable("BUILD_SOURCEBRANCH", providers);
                if (branch.isPresent()) {
                    return branch.get();
                }
            } else if (isBuildkite(providers)) {
                Optional<String> branch = envVariable("BUILDKITE_BRANCH", providers);
                if (branch.isPresent()) {
                    return branch.get();
                }
            } else if (isGitHubActions(providers)) {
                Optional<String> branch = envVariable("GITHUB_REF_NAME", providers);
                if (branch.isPresent()) {
                    return branch.get();
                }
            }
            return gitCommand.get();
        }

        private static Optional<String> getLocalBranch(String remoteBranch) {
            // This finds the longest matching remote name. This is because, for example, a local git clone could have
            // two remotes named `origin` and `origin/two`. In this scenario, we would want a remote branch of
            // `origin/two/main` to match to the `origin/two` remote, not to `origin`
            Function<String, Optional<String>> findLongestMatchingRemote = remotes -> Arrays.stream(remotes.split("\\R"))
                .filter(remote -> remoteBranch.startsWith(remote + "/"))
                .max(Comparator.comparingInt(String::length));

            return Optional.ofNullable(execAndGetStdOut("git", "remote"))
                .filter(Utils::isNotEmpty)
                .flatMap(findLongestMatchingRemote)
                .map(remote -> remoteBranch.replaceFirst("^" + remote + "/", ""));
        }
    }

    private static void addCustomValueAndSearchLink(DevelocityAdapter develocity, String name, String value) {
        develocity.getBuildScan().value(name, value);
        addSearchLink(develocity, name, name, value);
    }

    private static void addCustomValueAndSearchLink(DevelocityAdapter develocity, String linkLabel, String name, String value) {
        develocity.getBuildScan().value(name, value);
        addSearchLink(develocity, linkLabel, name, value);
    }

    private static void addSearchLink(DevelocityAdapter develocity, String linkLabel, Map<String, String> values) {
        // the parameters for a link querying multiple custom values look like:
        // search.names=name1,name2&search.values=value1,value2
        // this reduction groups all names and all values together in order to properly generate the query
        values.entrySet().stream()
            .sorted(Map.Entry.comparingByKey()) // results in a deterministic order of link parameters
            .reduce((a, b) -> new AbstractMap.SimpleEntry<>(a.getKey() + "," + b.getKey(), a.getValue() + "," + b.getValue()))
            .ifPresent(x -> addSearchLink(develocity, linkLabel, x.getKey(), x.getValue()));
    }

    private static void addSearchLink(DevelocityAdapter develocity, String linkLabel, String name, String value) {
        String searchParams = "search.names=" + urlEncode(name) + "&search.values=" + urlEncode(value);
        String server = develocity.getServer();
        if (server != null) {
            String url = appendIfMissing(server, '/') + "scans?" + searchParams + "#selection.buildScanB=" + urlEncode("{SCAN_ID}");
            develocity.getBuildScan().link(linkLabel + " build scans", url);
        }
    }

    private static Provider<String> systemPropertyProvider(String name, ProviderFactory providers) {
        if (isGradle61OrNewer()) {
            return providers.systemProperty(name);
        } else {
            return providers.provider(() -> System.getProperty(name));
        }
    }

    private static Provider<String> gradlePropertyProvider(String name, Gradle gradle, ProviderFactory providers) {
        if (isGradle62OrNewer()) {
            return providers.gradleProperty(name);
        } else {
            return providers.provider(() -> (String) gradle.getRootProject().findProperty(name));
        }
    }

    private static <T> Provider<T> firstOrElseSecond(ProviderFactory providers, Provider<T> overrideProperty, Provider<T> mainProperty) {
        if (isGradle56OrNewer()) {
            return overrideProperty.orElse(mainProperty);
        } else {
            return providers.provider(() -> overrideProperty.isPresent() ? overrideProperty.get() : mainProperty.getOrNull());
        }
    }

}
