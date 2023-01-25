package com.gradle;

import com.gradle.scan.plugin.BuildResult;
import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.testing.Test;

import java.util.AbstractMap;
import java.util.HashMap;
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

    private static final String SYSTEM_PROP_IDEA_VENDOR_NAME = "idea.vendor.name";
    private static final String SYSTEM_PROP_IDEA_VERSION = "idea.version";
    private static final String PROJECT_PROP_ANDROID_INVOKED_FROM_IDE = "android.injected.invoked.from.ide";
    private static final String PROJECT_PROP_ANDROID_STUDIO_VERSION = "android.injected.studio.version";
    private static final String SYSTEM_PROP_ECLIPSE_BUILD_ID = "eclipse.buildId";
    private static final String SYSTEM_PROP_IDEA_SYNC_ACTIVE = "idea.sync.active";

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
        Utils.sysProperty("os.name", providers).ifPresent(buildScan::tag);
    }

    private void captureIde() {
        if (!CiUtils.isCi(providers)) {
            // Prepare relevant properties for use at execution time
            Map<String, Provider<String>> relevantProperties = new HashMap<>();
            relevantProperties.put(SYSTEM_PROP_IDEA_VENDOR_NAME, systemPropertyProvider(SYSTEM_PROP_IDEA_VENDOR_NAME, providers));
            relevantProperties.put(SYSTEM_PROP_IDEA_VERSION, systemPropertyProvider(SYSTEM_PROP_IDEA_VERSION, providers));
            relevantProperties.put(PROJECT_PROP_ANDROID_INVOKED_FROM_IDE, gradlePropertyProvider(PROJECT_PROP_ANDROID_INVOKED_FROM_IDE, providers));
            relevantProperties.put(PROJECT_PROP_ANDROID_STUDIO_VERSION, gradlePropertyProvider(PROJECT_PROP_ANDROID_STUDIO_VERSION, providers));
            relevantProperties.put(SYSTEM_PROP_ECLIPSE_BUILD_ID, systemPropertyProvider(SYSTEM_PROP_ECLIPSE_BUILD_ID, providers));
            relevantProperties.put(SYSTEM_PROP_IDEA_SYNC_ACTIVE, systemPropertyProvider(SYSTEM_PROP_IDEA_SYNC_ACTIVE, providers));

            // Process data at execution time to ensure property initialization
            buildScan.buildFinished(new CaptureIdeMetadataAction(buildScan, relevantProperties));
        }
    }

    private Provider<String> systemPropertyProvider(String name, ProviderFactory providers) {
        if(Utils.isGradle61OrNewer()) {
            return providers.systemProperty(name);
        } else {
            return providers.provider(() -> System.getProperty(name));
        }
    }

    private Provider<String> gradlePropertyProvider(String name, ProviderFactory providers) {
        if(Utils.isGradle62OrNewer()) {
            return providers.gradleProperty(name);
        } else {
            return providers.provider(() -> (String) gradle.getRootProject().findProperty(name));
        }
    }

    private static final class CaptureIdeMetadataAction implements Action<BuildResult> {

        private final BuildScanExtension buildScan;
        private final Map<String, Provider<String>> props;

        private CaptureIdeMetadataAction(BuildScanExtension buildScan, Map<String, Provider<String>> props) {
            this.buildScan = buildScan;
            this.props = props;
        }

        @Override
        public void execute(BuildResult buildResult) {
            if (props.get(SYSTEM_PROP_IDEA_VENDOR_NAME).isPresent()) {
                String ideaVendorNameValue = props.get(SYSTEM_PROP_IDEA_VENDOR_NAME).get();
                if (ideaVendorNameValue.equals("Google")) {
                    // using androidStudioVersion instead of ideaVersion for compatibility reasons, those can be different (e.g. 2020.3.1 Patch 3 instead of 2020.3)
                    tagIde("Android Studio", props.get(PROJECT_PROP_ANDROID_STUDIO_VERSION).getOrElse(""));
                } else if (ideaVendorNameValue.equals("JetBrains")) {
                    tagIde("IntelliJ IDEA", props.get(SYSTEM_PROP_IDEA_VERSION).getOrElse(""));
                }
            } else if (props.get(PROJECT_PROP_ANDROID_INVOKED_FROM_IDE).isPresent()) {
                // this case should be handled by the ideaVendorName condition but keeping it for compatibility reason (ideaVendorName started with 2020.1)
                tagIde("Android Studio", props.get(PROJECT_PROP_ANDROID_STUDIO_VERSION).getOrElse(""));
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

        private void tagIde(String ideLabel, String version) {
            buildScan.tag(ideLabel);
            if (!version.isEmpty()) {
                buildScan.value(ideLabel + " version", version);
            }
        }

    }

    private void captureCiOrLocal() {
        buildScan.tag(CiUtils.isCi(providers) ? "CI" : "LOCAL");
    }

    private void captureCiMetadata() {
        if (CiUtils.isCi(providers)) {
            // Prepare project directory for use at execution time
            Provider<Directory> projectDirectory = providers.provider(() -> gradle.getRootProject().getLayout().getProjectDirectory());

            // Process data at execution time not to have a CI environment variable (ie. build number) invalidates the configuration cache
            buildScan.buildFinished(new CaptureCiMetadataAction(buildScan, providers, customValueSearchLinker, projectDirectory));
        }
    }

    private static final class CaptureCiMetadataAction implements Action<BuildResult> {

        private final BuildScanExtension buildScan;
        private final ProviderFactory providers;
        private final CustomValueSearchLinker customValueSearchLinker;
        private final Provider<Directory> projectDirectory;

        private CaptureCiMetadataAction(BuildScanExtension buildScan, ProviderFactory providers, CustomValueSearchLinker customValueSearchLinker, Provider<Directory> projectDirectory) {
            this.buildScan = buildScan;
            this.providers = providers;
            this.customValueSearchLinker = customValueSearchLinker;
            this.projectDirectory = projectDirectory;
        }

        @Override
        public void execute(BuildResult buildResult) {
            if (CiUtils.isJenkins(providers) || CiUtils.isHudson(providers)) {
                Optional<String> buildUrl = Utils.envVariable("BUILD_URL", providers);
                Optional<String> buildNumber = Utils.envVariable("BUILD_NUMBER", providers);
                Optional<String> nodeName = Utils.envVariable("NODE_NAME", providers);
                Optional<String> jobName = Utils.envVariable("JOB_NAME", providers);
                Optional<String> stageName = Utils.envVariable("STAGE_NAME", providers);

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

            if(CiUtils.isTeamCity(providers)) {
                Optional<String> teamcityBuildPropertiesFile = Utils.envVariable("TEAMCITY_BUILD_PROPERTIES_FILE", providers);
                if(teamcityBuildPropertiesFile.isPresent()){
                    Properties buildProperties = Utils.readPropertiesFile(teamcityBuildPropertiesFile.get(), providers, projectDirectory.get());

                    String teamcityConfigFile = buildProperties.getProperty("teamcity.configuration.properties.file");
                    if(Utils.isNotEmpty(teamcityConfigFile)) {
                        Properties configProperties = Utils.readPropertiesFile(teamcityConfigFile, providers, projectDirectory.get());

                        String teamCityServerUrl = configProperties.getProperty("teamcity.serverUrl");
                        String teamCityBuildId = buildProperties.getProperty("teamcity.build.id");
                        if (Utils.isNotEmpty(teamCityServerUrl) && Utils.isNotEmpty(teamCityBuildId)) {
                            String buildUrl = appendIfMissing(teamCityServerUrl, "/") + "viewLog.html?buildId=" + urlEncode(teamCityBuildId);
                            buildScan.link("TeamCity build", buildUrl);
                        }
                    }

                    String teamCityBuildNumber = buildProperties.getProperty("build.number");
                    if (Utils.isNotEmpty(teamCityBuildNumber)) {
                        buildScan.value("CI build number", teamCityBuildNumber);
                    }
                    String teamCityBuildTypeId = buildProperties.getProperty("teamcity.buildType.id");
                    if (Utils.isNotEmpty(teamCityBuildTypeId)) {
                        buildScan.value("CI build config", teamCityBuildTypeId);
                    }
                    String teamCityAgentName = buildProperties.getProperty("agent.name");
                    if (Utils.isNotEmpty(teamCityAgentName)) {
                        buildScan.value("CI agent", teamCityAgentName);
                    }
                }
            }

            if (CiUtils.isCircleCI(providers)) {
                Utils.envVariable("CIRCLE_BUILD_URL", providers).ifPresent(url ->
                        buildScan.link("CircleCI build", url));
                Utils.envVariable("CIRCLE_BUILD_NUM", providers).ifPresent(value ->
                        buildScan.value("CI build number", value));
                Utils.envVariable("CIRCLE_JOB", providers).ifPresent(value ->
                        customValueSearchLinker.addCustomValueAndSearchLink("CI job", value));
                Utils.envVariable("CIRCLE_WORKFLOW_ID", providers).ifPresent(value ->
                        customValueSearchLinker.addCustomValueAndSearchLink("CI workflow", value));
            }

            if (CiUtils.isBamboo(providers)) {
                Utils.envVariable("bamboo_resultsUrl", providers).ifPresent(url ->
                        buildScan.link("Bamboo build", url));
                Utils.envVariable("bamboo_buildNumber", providers).ifPresent(value ->
                        buildScan.value("CI build number", value));
                Utils.envVariable("bamboo_planName", providers).ifPresent(value ->
                        customValueSearchLinker.addCustomValueAndSearchLink("CI plan", value));
                Utils.envVariable("bamboo_buildPlanName", providers).ifPresent(value ->
                        customValueSearchLinker.addCustomValueAndSearchLink("CI build plan", value));
                Utils.envVariable("bamboo_agentId", providers).ifPresent(value ->
                        customValueSearchLinker.addCustomValueAndSearchLink("CI agent", value));
            }

            if (CiUtils.isGitHubActions(providers)) {
                Optional<String> gitHubRepository = Utils.envVariable("GITHUB_REPOSITORY", providers);
                Optional<String> gitHubRunId = Utils.envVariable("GITHUB_RUN_ID", providers);
                if (gitHubRepository.isPresent() && gitHubRunId.isPresent()) {
                    buildScan.link("GitHub Actions build", "https://github.com/" + gitHubRepository.get() + "/actions/runs/" + gitHubRunId.get());
                }
                Utils.envVariable("GITHUB_WORKFLOW", providers).ifPresent(value ->
                        customValueSearchLinker.addCustomValueAndSearchLink("CI workflow", value));
                Utils.envVariable("GITHUB_RUN_ID", providers).ifPresent(value ->
                        customValueSearchLinker.addCustomValueAndSearchLink("CI run", value));
            }

            if (CiUtils.isGitLab(providers)) {
                Utils.envVariable("CI_JOB_URL", providers).ifPresent(url ->
                        buildScan.link("GitLab build", url));
                Utils.envVariable("CI_PIPELINE_URL", providers).ifPresent(url ->
                        buildScan.link("GitLab pipeline", url));
                Utils.envVariable("CI_JOB_NAME", providers).ifPresent(value ->
                        customValueSearchLinker.addCustomValueAndSearchLink("CI job", value));
                Utils.envVariable("CI_JOB_STAGE", providers).ifPresent(value ->
                        customValueSearchLinker.addCustomValueAndSearchLink("CI stage", value));
            }

            if (CiUtils.isTravis(providers)) {
                Utils.envVariable("TRAVIS_BUILD_WEB_URL", providers).ifPresent(url ->
                        buildScan.link("Travis build", url));
                Utils.envVariable("TRAVIS_BUILD_NUMBER", providers).ifPresent(value ->
                        buildScan.value("CI build number", value));
                Utils.envVariable("TRAVIS_JOB_NAME", providers).ifPresent(value ->
                        customValueSearchLinker.addCustomValueAndSearchLink("CI job", value));
                Utils.envVariable("TRAVIS_EVENT_TYPE", providers).ifPresent(buildScan::tag);
            }

            if (CiUtils.isBitrise(providers)) {
                Utils.envVariable("BITRISE_BUILD_URL", providers).ifPresent(url ->
                        buildScan.link("Bitrise build", url));
                Utils.envVariable("BITRISE_BUILD_NUMBER", providers).ifPresent(value ->
                        buildScan.value("CI build number", value));
            }

            if (CiUtils.isGoCD(providers)) {
                Optional<String> pipelineName = Utils.envVariable("GO_PIPELINE_NAME", providers);
                Optional<String> pipelineNumber = Utils.envVariable("GO_PIPELINE_COUNTER", providers);
                Optional<String> stageName = Utils.envVariable("GO_STAGE_NAME", providers);
                Optional<String> stageNumber = Utils.envVariable("GO_STAGE_COUNTER", providers);
                Optional<String> jobName = Utils.envVariable("GO_JOB_NAME", providers);
                Optional<String> goServerUrl = Utils.envVariable("GO_SERVER_URL", providers);
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
                Optional<String> azureServerUrl = Utils.envVariable("SYSTEM_TEAMFOUNDATIONCOLLECTIONURI", providers);
                Optional<String> azureProject = Utils.envVariable("SYSTEM_TEAMPROJECT", providers);
                Optional<String> buildId = Utils.envVariable("BUILD_BUILDID", providers);
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

    }

    private void captureGitMetadata() {
        // Run expensive computation in background
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
            if (Utils.isGradle5OrNewer()) {
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

}
