package com.gradle;

import com.gradle.develocity.agent.gradle.adapters.BuildResultAdapter;
import com.gradle.develocity.agent.gradle.adapters.BuildScanAdapter;
import com.gradle.develocity.agent.gradle.adapters.DevelocityAdapter;
import org.gradle.api.Action;
import org.gradle.api.file.Directory;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

import java.io.File;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static final String PROJECT_PROP_ANDROID_STUDIO_AGENT = "android.studio.agent";
    private static final String SYSTEM_PROP_ECLIPSE_BUILD_ID = "eclipse.buildId";
    private static final String SYSTEM_PROP_IDEA_SYNC_ACTIVE = "idea.sync.active";
    private static final String ENV_VAR_VSCODE_PID = "VSCODE_PID";
    private static final String ENV_VAR_VSCODE_INJECTION = "VSCODE_INJECTION";

    private final DevelocityAdapter develocity;
    private final BuildScanAdapter buildScan;
    private final ProviderFactory providers;
    private final Gradle gradle;
    private final File projectDir;

    CustomBuildScanEnhancements(DevelocityAdapter develocity, ProviderFactory providers, Gradle gradle, File projectDir) {
        this.develocity = develocity;
        this.buildScan = develocity.getBuildScan();
        this.providers = providers;
        this.gradle = gradle;
        this.projectDir = projectDir;
    }

    // Apply all build scan enhancements via custom tags, links, and values
    void apply() {
        captureOs();
        captureIde();
        captureCiOrLocal();
        captureCiMetadata();
        captureGitMetadata();
        captureAgentMetadata();
    }

    private void captureOs() {
        // Process data at execution time so that the OS name does not become a configuration cache input
        buildScan.buildFinished(new CaptureOsAction(buildScan));
    }

    private static final class CaptureOsAction implements Action<BuildResultAdapter> {

        private final BuildScanAdapter buildScan;

        private CaptureOsAction(BuildScanAdapter buildScan) {
            this.buildScan = buildScan;
        }

        @Override
        public void execute(BuildResultAdapter buildResult) {
            sysProperty("os.name").ifPresent(buildScan::tag);
        }

    }

    private void captureIde() {
        // Prepare relevant properties for use at execution time
        Map<String, Provider<String>> ideProperties = new HashMap<>();
        ideProperties.put(SYSTEM_PROP_IDEA_VENDOR_NAME, systemPropertyProvider(SYSTEM_PROP_IDEA_VENDOR_NAME, providers));
        ideProperties.put(SYSTEM_PROP_IDEA_VERSION, systemPropertyProvider(SYSTEM_PROP_IDEA_VERSION, providers));
        ideProperties.put(PROJECT_PROP_ANDROID_INVOKED_FROM_IDE, gradlePropertyProvider(PROJECT_PROP_ANDROID_INVOKED_FROM_IDE, gradle, providers));
        ideProperties.put(PROJECT_PROP_ANDROID_STUDIO_VERSION, firstOrElseSecond(providers, gradlePropertyProvider(PROJECT_PROP_ANDROID_STUDIO_VERSION, gradle, providers), gradlePropertyProvider(PROJECT_PROP_ANDROID_STUDIO_VERSION_LEGACY, gradle, providers)));
        ideProperties.put(SYSTEM_PROP_ECLIPSE_BUILD_ID, systemPropertyProvider(SYSTEM_PROP_ECLIPSE_BUILD_ID, providers));
        ideProperties.put(SYSTEM_PROP_IDEA_SYNC_ACTIVE, systemPropertyProvider(SYSTEM_PROP_IDEA_SYNC_ACTIVE, providers));
        ideProperties.put(ENV_VAR_VSCODE_PID, environmentPropertyProvider(ENV_VAR_VSCODE_PID, providers));
        ideProperties.put(ENV_VAR_VSCODE_INJECTION, environmentPropertyProvider(ENV_VAR_VSCODE_INJECTION, providers));

        // Process data at execution time to ensure property initialization, and so that CI detection
        // does not become a configuration cache input
        buildScan.buildFinished(new CaptureIdeMetadataAction(buildScan, ideProperties));
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
            if (isCi()) {
                return;
            }

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
            } else if (props.get(ENV_VAR_VSCODE_PID).isPresent() || props.get(ENV_VAR_VSCODE_INJECTION).isPresent()) {
                tagIde("VS Code", "");
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
        // Process data at execution time so that CI detection does not become a configuration cache input
        buildScan.buildFinished(new CaptureCiOrLocalAction(buildScan));
    }

    private static final class CaptureCiOrLocalAction implements Action<BuildResultAdapter> {

        private final BuildScanAdapter buildScan;

        private CaptureCiOrLocalAction(BuildScanAdapter buildScan) {
            this.buildScan = buildScan;
        }

        @Override
        public void execute(BuildResultAdapter buildResult) {
            buildScan.tag(isCi() ? "CI" : "LOCAL");
        }

    }

    private void captureCiMetadata() {
        // Prepare project directory for use at execution time
        Provider<Directory> projectDirectory = providers.provider(() -> gradle.getRootProject().getLayout().getProjectDirectory());

        // Process data at execution time so that CI metadata does not become a configuration cache input
        buildScan.buildFinished(new CaptureCiMetadataAction(develocity, providers, projectDirectory));
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
            if (isJenkins() || isHudson()) {
                String ciProvider = isJenkins() ? "Jenkins" : "Hudson";
                String controllerUrlEnvVar = isJenkins() ? "JENKINS_URL" : "HUDSON_URL";

                Optional<String> buildUrl = envVariable("BUILD_URL");
                Optional<String> buildNumber = envVariable("BUILD_NUMBER");
                Optional<String> nodeName = envVariable("NODE_NAME");
                Optional<String> jobName = envVariable("JOB_NAME");
                Optional<String> stageName = envVariable("STAGE_NAME");
                Optional<String> controllerUrl = envVariable(controllerUrlEnvVar);

                buildScan.value("CI provider", ciProvider);
                buildUrl.ifPresent(url ->
                    buildScan.link(isJenkins() ? "Jenkins build" : "Hudson build", url));
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

            if (isTeamCity()) {
                buildScan.value("CI provider", "TeamCity");
                Optional<String> teamcityBuildPropertiesFile = envVariable("TEAMCITY_BUILD_PROPERTIES_FILE");
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

            if (isCircleCI()) {
                buildScan.value("CI provider", "CircleCI");
                envVariable("CIRCLE_BUILD_URL").ifPresent(url ->
                    buildScan.link("CircleCI build", url));
                envVariable("CIRCLE_BUILD_NUM").ifPresent(value ->
                    buildScan.value("CI build number", value));
                envVariable("CIRCLE_JOB").ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI job", value));
                envVariable("CIRCLE_WORKFLOW_ID").ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI workflow", value));
            }

            if (isBamboo()) {
                buildScan.value("CI provider", "Bamboo");
                envVariable("bamboo_resultsUrl").ifPresent(url ->
                    buildScan.link("Bamboo build", url));
                envVariable("bamboo_buildNumber").ifPresent(value ->
                    buildScan.value("CI build number", value));
                envVariable("bamboo_planName").ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI plan", value));
                envVariable("bamboo_buildPlanName").ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI build plan", value));
                envVariable("bamboo_agentId").ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI agent", value));
            }

            if (isGitHubActions()) {
                buildScan.value("CI provider", "GitHub Actions");

                Optional<String> workflow = envVariable("GITHUB_WORKFLOW");
                Optional<String> jobId = envVariable("GITHUB_JOB");
                Optional<String> actionNameOrStepId = envVariable("GITHUB_ACTION");
                Optional<String> runId = envVariable("GITHUB_RUN_ID");
                Optional<String> runAttempt = envVariable("GITHUB_RUN_ATTEMPT");
                Optional<String> runNumber = envVariable("GITHUB_RUN_NUMBER");
                Optional<String> headRef = envVariable("GITHUB_HEAD_REF").filter(value -> !value.isEmpty());
                Optional<String> baseRef = envVariable("GITHUB_BASE_REF").filter(value -> !value.isEmpty());
                Optional<String> serverUrl = envVariable("GITHUB_SERVER_URL");
                Optional<String> gitRepository = envVariable("GITHUB_REPOSITORY");
                Optional<String> refName = envVariable("GITHUB_REF_NAME");

                workflow.ifPresent(value ->
                        addCustomValueAndSearchLink(develocity, "CI workflow", value));
                jobId.ifPresent(value ->
                        addCustomValueAndSearchLink(develocity, "CI job", value));
                actionNameOrStepId.ifPresent(value ->
                        addCustomValueAndSearchLink(develocity, "CI step", value));

                runId.ifPresent(value ->
                        buildScan.value("CI run", value));
                runAttempt.ifPresent(value ->
                        buildScan.value("CI run attempt", value));
                runNumber.ifPresent(value ->
                        buildScan.value("CI run number", value));
                headRef.ifPresent(value ->
                        buildScan.value("PR branch", value));
                baseRef.ifPresent(value ->
                        buildScan.value("PR base branch", value));

                if (serverUrl.isPresent() && gitRepository.isPresent() && runId.isPresent()) {
                    StringBuilder githubActionsBuild = new StringBuilder(serverUrl.get())
                            .append("/").append(gitRepository.get())
                            .append("/actions/runs/").append(runId.get());
                    runAttempt.ifPresent(value -> githubActionsBuild.append("/attempts/").append(value));
                    buildScan.link("GitHub Actions build", githubActionsBuild.toString());
                }

                boolean isPullRequestBuild = headRef.isPresent();
                if (serverUrl.isPresent() && gitRepository.isPresent() && isPullRequestBuild && refName.isPresent()) {
                    Matcher matcher = Pattern.compile("^(\\d+)/merge$").matcher(refName.get());
                    if (matcher.matches()) {
                        String githubPullRequest = serverUrl.get() +
                                "/" + gitRepository.get() +
                                "/pull/" + matcher.group(1);
                        buildScan.link("GitHub pull request", githubPullRequest);
                    }
                }

                if (runId.isPresent()) {
                    Map<String, String> params = new HashMap<>();
                    params.put("CI run", runId.get());
                    runAttempt.ifPresent(value -> params.put("CI run attempt", value));
                    addSearchLink(develocity, "CI run", params);
                }
            }

            if (isGitLab()) {
                buildScan.value("CI provider", "GitLab");
                envVariable("CI_JOB_URL").ifPresent(url ->
                    buildScan.link("GitLab build", url));
                envVariable("CI_PIPELINE_URL").ifPresent(url ->
                    buildScan.link("GitLab pipeline", url));
                envVariable("CI_JOB_NAME").ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI job", value));
                envVariable("CI_JOB_STAGE").ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI stage", value));
            }

            if (isTravis()) {
                buildScan.value("CI provider", "Travis");
                envVariable("TRAVIS_BUILD_WEB_URL").ifPresent(url ->
                    buildScan.link("Travis build", url));
                envVariable("TRAVIS_BUILD_NUMBER").ifPresent(value ->
                    buildScan.value("CI build number", value));
                envVariable("TRAVIS_JOB_NAME").ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI job", value));
                envVariable("TRAVIS_EVENT_TYPE").ifPresent(buildScan::tag);
            }

            if (isBitrise()) {
                buildScan.value("CI provider", "Bitrise");
                envVariable("BITRISE_BUILD_URL").ifPresent(url ->
                    buildScan.link("Bitrise build", url));
                envVariable("BITRISE_BUILD_NUMBER").ifPresent(value ->
                    buildScan.value("CI build number", value));
            }

            if (isGoCD()) {
                buildScan.value("CI provider", "GoCD");
                Optional<String> pipelineName = envVariable("GO_PIPELINE_NAME");
                Optional<String> pipelineNumber = envVariable("GO_PIPELINE_COUNTER");
                Optional<String> stageName = envVariable("GO_STAGE_NAME");
                Optional<String> stageNumber = envVariable("GO_STAGE_COUNTER");
                Optional<String> jobName = envVariable("GO_JOB_NAME");
                Optional<String> goServerUrl = envVariable("GO_SERVER_URL");
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

            if (isAzurePipelines()) {
                buildScan.value("CI provider", "Azure Pipelines");
                Optional<String> azureServerUrl = envVariable("SYSTEM_TEAMFOUNDATIONCOLLECTIONURI");
                Optional<String> azureProject = envVariable("SYSTEM_TEAMPROJECT");
                Optional<String> buildId = envVariable("BUILD_BUILDID");
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

            if (isBuildkite()) {
                buildScan.value("CI provider", "Buildkite");
                envVariable("BUILDKITE_BUILD_URL")
                    .ifPresent(s -> buildScan.link("Buildkite build", s));
                envVariable("BUILDKITE_COMMAND").ifPresent(value ->
                    addCustomValueAndSearchLink(develocity, "CI command", value));
                envVariable("BUILDKITE_BUILD_ID").ifPresent(value ->
                    buildScan.value("CI build ID", value));

                Optional<String> buildkitePrRepo = envVariable("BUILDKITE_PULL_REQUEST_REPO");
                Optional<String> buildkitePrNumber = envVariable("BUILDKITE_PULL_REQUEST");
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
        buildScan.background(new CaptureGitMetadataAction(develocity, projectDir));
    }

    private static final class CaptureGitMetadataAction implements Action<BuildScanAdapter> {

        private final DevelocityAdapter develocity;
        private final File projectDir;

        private CaptureGitMetadataAction(DevelocityAdapter develocity, File projectDir) {
            this.develocity = develocity;
            this.projectDir = projectDir;
        }

        @Override
        public void execute(BuildScanAdapter buildScan) {
            if (!isGitInstalled()) {
                return;
            }

            String gitRepo = execAndGetStdOut(projectDir, "git", "config", "--get", "remote.origin.url");
            String gitCommitId = execAndGetStdOut(projectDir, "git", "rev-parse", "--verify", "HEAD");
            String gitCommitShortId = execAndGetStdOut(projectDir, "git", "rev-parse", "--short=8", "--verify", "HEAD");
            String gitBranchName = getGitBranchName(projectDir, () -> execAndGetStdOut(projectDir, "git", "rev-parse", "--abbrev-ref", "HEAD"));
            String gitStatus = execAndGetStdOut(projectDir, "git", "status", "--porcelain");

            if (isNotEmpty(gitRepo)) {
                redactUserInfo(gitRepo).ifPresent(redactedGitRepo -> buildScan.value("Git repository", redactedGitRepo));
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

            Optional<String> gitHubUrl = envVariable("GITHUB_SERVER_URL");
            Optional<String> gitRepository = envVariable("GITHUB_REPOSITORY");
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

        private String getGitBranchName(File projectDir, Supplier<String> gitCommand) {
            if (isJenkins() || isHudson()) {
                Optional<String> branchName = envVariable("BRANCH_NAME");
                if (branchName.isPresent()) {
                    return branchName.get();
                }

                Optional<String> gitBranch = envVariable("GIT_BRANCH");
                if (gitBranch.isPresent()) {
                    Optional<String> localBranch = getLocalBranch(projectDir, gitBranch.get());
                    if (localBranch.isPresent()) {
                        return localBranch.get();
                    }
                }
            } else if (isGitLab()) {
                Optional<String> branch = envVariable("CI_COMMIT_REF_NAME");
                if (branch.isPresent()) {
                    return branch.get();
                }
            } else if (isAzurePipelines()) {
                Optional<String> branch = envVariable("BUILD_SOURCEBRANCH");
                if (branch.isPresent()) {
                    return branch.get();
                }
            } else if (isBuildkite()) {
                Optional<String> branch = envVariable("BUILDKITE_BRANCH");
                if (branch.isPresent()) {
                    return branch.get();
                }
            } else if (isGitHubActions()) {
                Optional<String> branch = envVariable("GITHUB_REF_NAME");
                if (branch.isPresent()) {
                    return branch.get();
                }
            }
            return gitCommand.get();
        }

        private static Optional<String> getLocalBranch(File projectDir, String remoteBranch) {
            // This finds the longest matching remote name. This is because, for example, a local git clone could have
            // two remotes named `origin` and `origin/two`. In this scenario, we would want a remote branch of
            // `origin/two/main` to match to the `origin/two` remote, not to `origin`
            Function<String, Optional<String>> findLongestMatchingRemote = remotes -> Arrays.stream(remotes.split("\\R"))
                .filter(remote -> remoteBranch.startsWith(remote + "/"))
                .max(Comparator.comparingInt(String::length));

            return Optional.ofNullable(execAndGetStdOut(projectDir, "git", "remote"))
                .filter(Utils::isNotEmpty)
                .flatMap(findLongestMatchingRemote)
                .map(remote -> remoteBranch.replaceFirst("^" + remote + "/", ""));
        }
    }

    private void captureAgentMetadata() {
        Provider<String> androidStudioAgent = gradlePropertyProvider(PROJECT_PROP_ANDROID_STUDIO_AGENT, gradle, providers);

        // Process data at execution time so that agent metadata does not become a configuration cache input
        buildScan.buildFinished(new CaptureAgentMetadataAction(buildScan, androidStudioAgent));
    }

    private static final class CaptureAgentMetadataAction implements Action<BuildResultAdapter> {

        private final BuildScanAdapter buildScan;
        private final Provider<String> androidStudioAgent;

        private CaptureAgentMetadataAction(BuildScanAdapter buildScan, Provider<String> androidStudioAgent) {
            this.buildScan = buildScan;
            this.androidStudioAgent = androidStudioAgent;
        }

        @Override
        public void execute(BuildResultAdapter buildResult) {
            Optional<String> claudeCode = envVariable("CLAUDECODE");
            // Codex environment variables are not officially documented.
            // This is best effort detection until something more official is implemented by Codex.
            Optional<String> codexSandbox = envVariable("CODEX_SANDBOX_NETWORK_DISABLED");
            Optional<String> codexThreadId = envVariable("CODEX_THREAD_ID");
            Optional<String> cursor = envVariable("CURSOR_AGENT");
            Optional<String> openCode = envVariable("OPENCODE");
            Optional<String> gemini = envVariable("GEMINI_CLI");
            Optional<String> copilotCli = envVariable("COPILOT_CLI");
            Optional<String> copilotAgent = envVariable("COPILOT_AGENT");
            Optional<String> androidStudioAgentEnv = envVariable("ANDROID_STUDIO_AGENT");

            claudeCode.ifPresent(env -> {
                buildScan.tag("AI");
                buildScan.value("AI agent", "Claude Code");
            });
            if (codexSandbox.isPresent() || codexThreadId.isPresent()) {
                buildScan.tag("AI");
                buildScan.value("AI agent", "Codex");
            }
            cursor.ifPresent(env -> {
                buildScan.tag("AI");
                buildScan.value("AI agent", "Cursor");
            });
            openCode.ifPresent(env -> {
                buildScan.tag("AI");
                buildScan.value("AI agent", "OpenCode");
            });
            gemini.ifPresent(env -> {
                buildScan.tag("AI");
                buildScan.value("AI agent", "Gemini CLI");
            });
            if (copilotCli.isPresent() || copilotAgent.isPresent()) {
                buildScan.tag("AI");
                buildScan.value("AI agent", "Copilot");
            }
            if (androidStudioAgent.isPresent() || androidStudioAgentEnv.isPresent()) {
                buildScan.tag("AI");
                buildScan.value("AI agent", "Gemini in Android Studio");
            }
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

    private static Provider<String> environmentPropertyProvider(String name, ProviderFactory providers) {
        if (isGradle61OrNewer()) {
            return providers.environmentVariable(name);
        } else {
            return providers.provider(() -> System.getenv(name));
        }
    }

}
