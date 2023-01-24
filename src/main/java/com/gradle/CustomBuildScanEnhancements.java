package com.gradle;

import com.gradle.scan.plugin.BuildResult;
import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.testing.Test;
import org.gradle.util.GradleVersion;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.gradle.Utils.appendIfMissing;
import static com.gradle.Utils.execAndCheckSuccess;
import static com.gradle.Utils.execAndGetStdOut;
import static com.gradle.Utils.isNotEmpty;
import static com.gradle.Utils.redactUserInfo;
import static com.gradle.Utils.urlEncode;

/**
 * Adds a standard set of useful tags, links and custom values to all build scans published.
 */
final class CustomBuildScanEnhancements {

    private final BuildScanExtension buildScan;
    private final ProviderFactory providers;
    private final CustomValueSearchLinker customValueSearchLinker;
    private final Gradle gradle;

    CustomBuildScanEnhancements(BuildScanExtension buildScan, ProviderFactory providers, Gradle gradle) {
        this.buildScan = buildScan;
        this.providers = providers;
        this.gradle = gradle;
        this.customValueSearchLinker = CustomValueSearchLinker.registerWith(buildScan);
    }

    // Apply all build scan enhancements via custom tags, links, and values
    void apply() {
        captureOs();
        captureIde();
        captureCiOrLocal();
        captureCiMetadata();
        captureGitMetadata();
        captureTestParallelization();
    }

    private void captureOs() {
        sysProperty("os.name").ifPresent(buildScan::tag);
    }

    private void captureIde() {
        if (!CiUtils.isCi(providers)) {
            // Wait for projects to load to ensure Gradle project properties are initialized
            gradle.projectsEvaluated(g -> {
                Optional<String> ideaVendorName = sysProperty("idea.vendor.name");
                Optional<String> ideaVersion = sysProperty("idea.version");
                Optional<String> invokedFromAndroidStudio = projectProperty("android.injected.invoked.from.ide");
                Optional<String> androidStudioVersion = projectProperty("android.injected.studio.version");
                Optional<String> eclipseVersion = sysProperty("eclipse.buildId");
                Optional<String> ideaSync = sysProperty("idea.sync.active");

                if (ideaVendorName.isPresent()) {
                    String ideaVendorNameValue = ideaVendorName.get();
                    if (ideaVendorNameValue.equals("Google")) {
                        // using androidStudioVersion instead of ideaVersion for compatibility reasons, those can be different (e.g. 2020.3.1 Patch 3 instead of 2020.3)
                        tagIde("Android Studio", androidStudioVersion.orElse(""));
                    } else if (ideaVendorNameValue.equals("JetBrains")) {
                        tagIde("IntelliJ IDEA", ideaVersion.orElse(""));
                    }
                } else if (invokedFromAndroidStudio.isPresent()) {
                    // this case should be handled by the ideaVendorName condition but keeping it for compatibility reason (ideaVendorName started with 2020.1)
                    tagIde("Android Studio", androidStudioVersion.orElse(""));
                } else if (ideaVersion.isPresent()) {
                    // this case should be handled by the ideaVendorName condition but keeping it for compatibility reason (ideaVendorName started with 2020.1)
                    tagIde("IntelliJ IDEA", ideaVersion.get());
                } else if (eclipseVersion.isPresent()) {
                    tagIde("Eclipse", eclipseVersion.get());
                } else {
                    buildScan.tag("Cmd Line");
                }

                if (ideaSync.isPresent()) {
                    buildScan.tag("IDE sync");
                }
            });
        }
    }

    private void tagIde(String ideLabel, String version) {
        buildScan.tag(ideLabel);
        if (!version.isEmpty()) {
            buildScan.value(ideLabel + " version", version);
        }
    }

    private void captureCiOrLocal() {
        buildScan.tag(CiUtils.isCi(providers) ? "CI" : "LOCAL");
    }

    private void captureCiMetadata() {
        if (CiUtils.isJenkins(providers) || CiUtils.isHudson(providers)) {
            Optional<String> buildUrl = envVariable("BUILD_URL");
            Optional<String> buildNumber = envVariable("BUILD_NUMBER");
            Optional<String> nodeName = envVariable("NODE_NAME");
            Optional<String> jobName = envVariable("JOB_NAME");
            Optional<String> stageName = envVariable("STAGE_NAME");

            buildUrl.ifPresent(url ->
                buildScan.link(CiUtils.isJenkins(providers) ? "Jenkins build" : "Hudson build", url));
            buildNumber.ifPresent(value ->
                buildScan.value("CI build number", value));
            nodeName.ifPresent(value ->
                customValueSearchLinker.addCustomValueAndSearchLink("CI node", value));
            jobName.ifPresent(value ->
                customValueSearchLinker.addCustomValueAndSearchLink("CI job", value));
            stageName.ifPresent(value ->
                customValueSearchLinker.addCustomValueAndSearchLink("CI stage", value));

            jobName.ifPresent(j -> buildNumber.ifPresent(b -> {
                Map<String, String> params = new LinkedHashMap<>();
                params.put("CI job", j);
                params.put("CI build number", b);
                customValueSearchLinker.registerLink("CI pipeline", params);
            }));
        }

        if (CiUtils.isTeamCity(providers)) {
            // Wait for projects to load to ensure Gradle project properties are initialized
            gradle.projectsEvaluated(g -> {
                Optional<String> teamCityConfigFile = projectProperty("teamcity.configuration.properties.file");
                Optional<String> buildId = projectProperty("teamcity.build.id");
                if (teamCityConfigFile.isPresent() && buildId.isPresent()) {
                    Properties properties = readPropertiesFile(teamCityConfigFile.get());
                    String teamCityServerUrl = properties.getProperty("teamcity.serverUrl");
                    if (teamCityServerUrl != null) {
                        String buildUrl = appendIfMissing(teamCityServerUrl, "/") + "viewLog.html?buildId=" + urlEncode(buildId.get());
                        buildScan.link("TeamCity build", buildUrl);
                    }
                }
                projectProperty("build.number").ifPresent(value ->
                    buildScan.value("CI build number", value));
                projectProperty("teamcity.buildType.id").ifPresent(value ->
                    customValueSearchLinker.addCustomValueAndSearchLink("CI build config", value));
                projectProperty("agent.name").ifPresent(value ->
                    customValueSearchLinker.addCustomValueAndSearchLink("CI agent", value));
            });
        }

        if (CiUtils.isCircleCI(providers)) {
            envVariable("CIRCLE_BUILD_URL").ifPresent(url ->
                buildScan.link("CircleCI build", url));
            envVariable("CIRCLE_BUILD_NUM").ifPresent(value ->
                buildScan.value("CI build number", value));
            envVariable("CIRCLE_JOB").ifPresent(value ->
                customValueSearchLinker.addCustomValueAndSearchLink("CI job", value));
            envVariable("CIRCLE_WORKFLOW_ID").ifPresent(value ->
                customValueSearchLinker.addCustomValueAndSearchLink("CI workflow", value));
        }

        if (CiUtils.isBamboo(providers)) {
            envVariable("bamboo_resultsUrl").ifPresent(url ->
                buildScan.link("Bamboo build", url));
            envVariable("bamboo_buildNumber").ifPresent(value ->
                buildScan.value("CI build number", value));
            envVariable("bamboo_planName").ifPresent(value ->
                customValueSearchLinker.addCustomValueAndSearchLink("CI plan", value));
            envVariable("bamboo_buildPlanName").ifPresent(value ->
                customValueSearchLinker.addCustomValueAndSearchLink("CI build plan", value));
            envVariable("bamboo_agentId").ifPresent(value ->
                customValueSearchLinker.addCustomValueAndSearchLink("CI agent", value));
        }

        if (CiUtils.isGitHubActions(providers)) {
            Optional<String> gitHubRepository = envVariable("GITHUB_REPOSITORY");
            Optional<String> gitHubRunId = envVariable("GITHUB_RUN_ID");
            if (gitHubRepository.isPresent() && gitHubRunId.isPresent()) {
                buildScan.link("GitHub Actions build", "https://github.com/" + gitHubRepository.get() + "/actions/runs/" + gitHubRunId.get());
            }
            envVariable("GITHUB_WORKFLOW").ifPresent(value ->
                customValueSearchLinker.addCustomValueAndSearchLink("CI workflow", value));
            envVariable("GITHUB_RUN_ID").ifPresent(value ->
                customValueSearchLinker.addCustomValueAndSearchLink("CI run", value));
        }

        if (CiUtils.isGitLab(providers)) {
            envVariable("CI_JOB_URL").ifPresent(url ->
                buildScan.link("GitLab build", url));
            envVariable("CI_PIPELINE_URL").ifPresent(url ->
                buildScan.link("GitLab pipeline", url));
            envVariable("CI_JOB_NAME").ifPresent(value ->
                customValueSearchLinker.addCustomValueAndSearchLink("CI job", value));
            envVariable("CI_JOB_STAGE").ifPresent(value ->
                customValueSearchLinker.addCustomValueAndSearchLink("CI stage", value));
        }

        if (CiUtils.isTravis(providers)) {
            envVariable("TRAVIS_BUILD_WEB_URL").ifPresent(url ->
                buildScan.link("Travis build", url));
            envVariable("TRAVIS_BUILD_NUMBER").ifPresent(value ->
                buildScan.value("CI build number", value));
            envVariable("TRAVIS_JOB_NAME").ifPresent(value ->
                customValueSearchLinker.addCustomValueAndSearchLink("CI job", value));
            envVariable("TRAVIS_EVENT_TYPE").ifPresent(buildScan::tag);
        }

        if (CiUtils.isBitrise(providers)) {
            envVariable("BITRISE_BUILD_URL").ifPresent(url ->
                buildScan.link("Bitrise build", url));
            envVariable("BITRISE_BUILD_NUMBER").ifPresent(value ->
                buildScan.value("CI build number", value));
        }

        if (CiUtils.isGoCD(providers)) {
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
                customValueSearchLinker.addCustomValueAndSearchLink("CI pipeline", value));
            jobName.ifPresent(value ->
                customValueSearchLinker.addCustomValueAndSearchLink("CI job", value));
            stageName.ifPresent(value ->
                customValueSearchLinker.addCustomValueAndSearchLink("CI stage", value));
        }

        if(CiUtils.isAzurePipelines(providers)) {
            Optional<String> azureServerUrl = envVariable("SYSTEM_TEAMFOUNDATIONCOLLECTIONURI");
            Optional<String> azureProject = envVariable("SYSTEM_TEAMPROJECT");
            Optional<String> buildId = envVariable("BUILD_BUILDID");
            if (Stream.of(azureServerUrl, azureProject, buildId).allMatch(Optional::isPresent)) {
                //noinspection OptionalGetWithoutIsPresent
                String buildUrl = String.format("%s%s/_build/results?buildId=%s",
                    azureServerUrl.get(), azureProject.get(), buildId.get());
                buildScan.link("Azure Pipelines build", buildUrl);
            } else if (azureServerUrl.isPresent()) {
                buildScan.link("Azure Pipelines", azureServerUrl.get());
            }

            buildId.ifPresent(value ->
                buildScan.value("CI build number", value));
        }
    }


    private void captureGitMetadata() {
        buildScan.background(new CaptureGitMetadataAction(providers, customValueSearchLinker));
    }

    private static final class CaptureGitMetadataAction implements Action<BuildScanExtension> {

        private final ProviderFactory providers;
        private final CustomValueSearchLinker customValueSearchLinker;

        private CaptureGitMetadataAction(ProviderFactory providers, CustomValueSearchLinker customValueSearchLinker) {
            this.providers = providers;
            this.customValueSearchLinker = customValueSearchLinker;
        }

        @Override
        public void execute(BuildScanExtension buildScan) {
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
                customValueSearchLinker.addCustomValueAndSearchLink("Git commit id", "Git commit id short", gitCommitShortId);
            }
            if (isNotEmpty(gitBranchName)) {
                buildScan.tag(gitBranchName);
                buildScan.value("Git branch", gitBranchName);
            }
            if (isNotEmpty(gitStatus)) {
                buildScan.tag("Dirty");
                buildScan.value("Git status", gitStatus);
            }

            if (isNotEmpty(gitRepo) && isNotEmpty(gitCommitId)) {
                if (gitRepo.contains("github.com/") || gitRepo.contains("github.com:")) {
                    Matcher matcher = Pattern.compile("(.*)github\\.com[/|:](.*)").matcher(gitRepo);
                    if (matcher.matches()) {
                        String rawRepoPath = matcher.group(2);
                        String repoPath = rawRepoPath.endsWith(".git") ? rawRepoPath.substring(0, rawRepoPath.length() - 4) : rawRepoPath;
                        buildScan.link("Github source", "https://github.com/" + repoPath + "/tree/" + gitCommitId);
                    }
                } else if (gitRepo.contains("gitlab.com/") || gitRepo.contains("gitlab.com:")) {
                    Matcher matcher = Pattern.compile("(.*)gitlab\\.com[/|:](.*)").matcher(gitRepo);
                    if (matcher.matches()) {
                        String rawRepoPath = matcher.group(2);
                        String repoPath = rawRepoPath.endsWith(".git") ? rawRepoPath.substring(0, rawRepoPath.length() - 4) : rawRepoPath;
                        buildScan.link("GitLab Source", "https://gitlab.com/" + repoPath + "/-/commit/" + gitCommitId);
                    }
                }
            }
        }

        private boolean isGitInstalled() {
            return execAndCheckSuccess("git", "--version");
        }

        private String getGitBranchName(Supplier<String> gitCommand) {
            if (CiUtils.isJenkins(providers) || CiUtils.isHudson(providers)) {
                Optional<String> branch = Utils.envVariable("BRANCH_NAME", providers);
                if (branch.isPresent()) {
                    return branch.get();
                }
            } else if (CiUtils.isGitLab(providers)) {
                Optional<String> branch = Utils.envVariable("CI_COMMIT_REF_NAME", providers);
                if (branch.isPresent()) {
                    return branch.get();
                }
            } else if (CiUtils.isAzurePipelines(providers)) {
                Optional<String> branch = Utils.envVariable("BUILD_SOURCEBRANCH", providers);
                if (branch.isPresent()) {
                    return branch.get();
                }
            }
            return gitCommand.get();
        }

    }

    /**
     * Collects custom values that should have a search link, and creates these links in `buildFinished`.
     * The actual construction of the links must be deferred to ensure the Server URL is set.
     * This functionality needs to be in a separate static class in order to work with configuration cache.
     */
    private static final class CustomValueSearchLinker implements Action<BuildResult> {

        private final BuildScanExtension buildScan;
        private final Map<String, String> customValueLinks;

        private CustomValueSearchLinker(BuildScanExtension buildScan) {
            this.buildScan = buildScan;
            this.customValueLinks = new LinkedHashMap<>();
        }

        private static CustomValueSearchLinker registerWith(BuildScanExtension buildScan) {
            CustomValueSearchLinker customValueSearchLinker = new CustomValueSearchLinker(buildScan);
            buildScan.buildFinished(customValueSearchLinker);
            return customValueSearchLinker;
        }

        private void addCustomValueAndSearchLink(String name, String value) {
            buildScan.value(name, value);
            registerLink(name, name, value);
        }

        public void addCustomValueAndSearchLink(String linkLabel, String name, String value) {
            buildScan.value(name, value);
            registerLink(linkLabel, name, value);
        }

        private void registerLink(String linkLabel, Map<String, String> values) {
            // the parameters for a link querying multiple custom values look like:
            // search.names=name1,name2&search.values=value1,value2
            // this reduction groups all names and all values together in order to properly generate the query
            values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // results in a deterministic order of link parameters
                .reduce((a, b) -> new AbstractMap.SimpleEntry<>(a.getKey() + "," + b.getKey(), a.getValue() + "," + b.getValue()))
                .ifPresent(x -> registerLink(linkLabel, x.getKey(), x.getValue()));
        }

        private synchronized void registerLink(String linkLabel, String name, String value) {
            String searchParams = "search.names=" + urlEncode(name) + "&search.values=" + urlEncode(value);
            customValueLinks.put(linkLabel, searchParams);
        }

        @Override
        public synchronized void execute(BuildResult buildResult) {
            String server = getServer(buildScan);
            if (server != null) {
                customValueLinks.forEach((linkLabel, searchParams) -> {
                    String url = appendIfMissing(server, "/") + "scans?" + searchParams + "#selection.buildScanB=" + urlEncode("{SCAN_ID}");
                    buildScan.link(linkLabel + " build scans", url);
                });
            }
        }

        private static String getServer(BuildScanExtension buildScan) {
            try {
                buildScan.getClass().getMethod("getServer");
                return buildScan.getServer();
            } catch (NoSuchMethodException e) {
                // not available in Gradle 4.x / Build Scan plugin 1.x
                return null;
            }
        }

    }

    private void captureTestParallelization() {
        gradle.afterProject(p -> {
            TaskCollection<Test> tests = p.getTasks().withType(Test.class);
            if (isGradle5OrNewer()) {
                tests.configureEach(captureMaxParallelForks(buildScan));
            } else {
                tests.all(captureMaxParallelForks(buildScan));
            }
        });
    }

    private static Action<Test> captureMaxParallelForks(BuildScanExtension buildScan) {
        return test -> {
            test.doFirst(new Action<Task>() {
                // use anonymous inner class to keep Test task instance cacheable
                // additionally, using lambdas as task actions is deprecated
                @Override
                public void execute(Task task) {
                    buildScan.value(test.getIdentityPath() + "#maxParallelForks", String.valueOf(test.getMaxParallelForks()));
                }
            });
        };
    }

    private Optional<String> envVariable(String name) {
        return Utils.envVariable(name, providers);
    }

    private Optional<String> projectProperty(String name) {
        return Utils.projectProperty(name, providers, gradle);
    }

    private Optional<String> sysProperty(String name) {
        return Utils.sysProperty(name, providers);
    }

    private Properties readPropertiesFile(String fileName) {
        return Utils.readPropertiesFile(fileName, providers, gradle);
    }

    private static boolean isGradle5OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("5.0")) >= 0;
    }

}
