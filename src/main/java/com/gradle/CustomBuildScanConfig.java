package com.gradle;

import com.gradle.scan.plugin.BuildScanExtension;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CustomBuildScanConfig {

    static void configureBuildScan(BuildScanExtension buildScan) {
        tagOs(buildScan);
        tagIde(buildScan);
        tagCiOrLocal(buildScan);
        addCiMetadata(buildScan);
        addGitMetadata(buildScan);
    }

    private static void tagOs(BuildScanExtension buildScan) {
        buildScan.tag(sysProperty("os.name"));
    }

    private static void tagIde(BuildScanExtension buildScan) {
        if (sysPropertyPresent("idea.version") || sysPropertyKeyStartingWith("idea.version")) {
            buildScan.tag("IntelliJ IDEA");
        } else if (sysPropertyPresent("eclipse.buildId")) {
            buildScan.tag("Eclipse");
        } else if (!isCi()) {
            buildScan.tag("Cmd Line");
        }
    }

    private static void tagCiOrLocal(BuildScanExtension buildScan) {
        buildScan.tag(isCi() ? "CI" : "LOCAL");
    }

    private static void addCiMetadata(BuildScanExtension buildScan) {
        if (isJenkins()) {
            if (envVariablePresent("BUILD_URL")) {
                buildScan.link("Jenkins build", envVariable("BUILD_URL"));
            }
            if (envVariablePresent("BUILD_NUMBER")) {
                buildScan.value("CI build number", envVariable("BUILD_NUMBER"));
            }
            if (envVariablePresent("NODE_NAME")) {
                String nodeNameLabel = "CI node";
                String nodeName = envVariable("NODE_NAME");
                buildScan.value(nodeNameLabel, nodeName);
                addCustomLinkWithSearchTerms(buildScan, "CI node build scans", nodeNameLabel, nodeName);
            }
            if (envVariablePresent("JOB_NAME")) {
                String jobNameLabel = "CI job";
                String jobName = envVariable("JOB_NAME");
                buildScan.value(jobNameLabel, jobName);
                addCustomLinkWithSearchTerms(buildScan, "CI job build scans", jobNameLabel, jobName);
            }
            if (envVariablePresent("STAGE_NAME")) {
                String stageNameLabel = "CI stage";
                String stageName = envVariable("STAGE_NAME");
                buildScan.value(stageNameLabel, stageName);
                addCustomLinkWithSearchTerms(buildScan, "CI stage build scans", stageNameLabel, stageName);
            }
        }

        if (isTeamCity()) {
            if (sysPropertyPresent("teamcity.configuration.properties.file")) {
                Properties properties = readPropertiesFile(sysProperty("teamcity.configuration.properties.file"));
                String teamCityServerUrl = properties.getProperty("teamcity.serverUrl");
                if (teamCityServerUrl != null && sysPropertyPresent("build.number") && sysPropertyPresent("teamcity.buildType.id")) {
                    String buildNumber = sysProperty("build.number");
                    String buildTypeId = sysProperty("teamcity.buildType.id");
                    buildScan.link("TeamCity build", appendIfMissing(teamCityServerUrl, "/") + "viewLog.html?buildNumber=" + buildNumber + "&buildTypeId=" + buildTypeId);
                }
            }
            if (sysPropertyPresent("build.number")) {
                buildScan.value("CI build number", sysProperty("build.number"));
            }
            if (sysPropertyPresent("agent.name")) {
                String agentNameLabel = "CI agent";
                String agentName = sysProperty("agent.name");
                buildScan.value(agentNameLabel, agentName);
                addCustomLinkWithSearchTerms(buildScan, "CI agent build scans", agentNameLabel, agentName);
            }
        }

        if (isCircleCI()) {
            if (envVariablePresent("CIRCLE_BUILD_URL")) {
                buildScan.link("CircleCI build", envVariable("CIRCLE_BUILD_URL"));
            }
            if (envVariablePresent("CIRCLE_BUILD_NUM")) {
                buildScan.value("CI build number", envVariable("CIRCLE_BUILD_NUM"));
            }
            if (envVariablePresent("CIRCLE_JOB")) {
                String jobNameLabel = "CI job";
                String jobName = envVariable("CIRCLE_JOB");
                buildScan.value(jobNameLabel, jobName);
                addCustomLinkWithSearchTerms(buildScan, "CI job build scans", jobNameLabel, jobName);
            }
            if (envVariablePresent("CIRCLE_WORKFLOW_ID")) {
                String workflowIdLabel = "CI workflow";
                String workflowId = envVariable("CIRCLE_WORKFLOW_ID");
                buildScan.value(workflowIdLabel, workflowId);
                addCustomLinkWithSearchTerms(buildScan, "CI workflow build scans", workflowIdLabel, workflowId);
            }
        }

        if (isBamboo()) {
            if (envVariablePresent("bamboo_resultsUrl")) {
                buildScan.link("Bamboo build", envVariable("bamboo_resultsUrl"));
            }
            if (envVariablePresent("bamboo_buildNumber")) {
                buildScan.value("CI build number", envVariable("bamboo_buildNumber"));
            }
            if (envVariablePresent("bamboo_planName")) {
                String planNameLabel = "CI plan";
                String planName = envVariable("bamboo_planName");
                buildScan.value(planNameLabel, planName);
                addCustomLinkWithSearchTerms(buildScan, "CI plan build scans", planNameLabel, planName);
            }
            if (envVariablePresent("bamboo_buildPlanName")) {
                String buildPlanNameLabel = "CI build plan";
                String buildPlanName = envVariable("bamboo_buildPlanName");
                buildScan.value(buildPlanNameLabel, buildPlanName);
                addCustomLinkWithSearchTerms(buildScan, "CI build plan build scans", buildPlanNameLabel, buildPlanName);
            }
            if (envVariablePresent("bamboo_agentId")) {
                String agentIdLabel = "CI agent";
                String agentId = envVariable("bamboo_agentId");
                buildScan.value(agentIdLabel, agentId);
                addCustomLinkWithSearchTerms(buildScan, "CI agent build scans", agentIdLabel, agentId);
            }
        }

        if (isGitHubActions()) {
            if (envVariablePresent("GITHUB_REPOSITORY") && envVariablePresent("GITHUB_RUN_ID")) {
                buildScan.link("GitHub Actions build", "https://github.com/" + envVariable("GITHUB_REPOSITORY") + "/actions/runs/" + envVariable("GITHUB_RUN_ID"));
            }
            if (envVariablePresent("GITHUB_WORKFLOW")) {
                String workflowNameLabel = "GitHub workflow";
                String workflowName = envVariable("GITHUB_WORKFLOW");
                buildScan.value(workflowNameLabel, workflowName);
                addCustomLinkWithSearchTerms(buildScan, "GitHub workflow build scans", workflowNameLabel, workflowName);
            }
        }

        if (isGitLab()) {
            if (envVariablePresent("CI_JOB_URL")) {
                buildScan.link("GitLab build", envVariable("CI_JOB_URL"));
            }
            if (envVariablePresent("CI_PIPELINE_URL")) {
                buildScan.link("GitLab pipeline", envVariable("CI_PIPELINE_URL"));
            }
            if (envVariablePresent("CI_JOB_NAME")) {
                String jobNameLabel = "CI job";
                String jobName = envVariable("CI_JOB_NAME");
                buildScan.value(jobNameLabel, jobName);
                addCustomLinkWithSearchTerms(buildScan, "CI job build scans", jobNameLabel, jobName);
            }
            if (envVariablePresent("CI_JOB_STAGE")) {
                String stageNameLabel = "CI stage";
                String stageName = envVariable("CI_JOB_STAGE");
                buildScan.value(stageNameLabel, stageName);
                addCustomLinkWithSearchTerms(buildScan, "CI stage build scans", stageNameLabel, stageName);
            }
        }

        if (isTravis()) {
            if (envVariablePresent("TRAVIS_BUILD_WEB_URL")) {
                buildScan.link("Travis build", envVariable("TRAVIS_BUILD_WEB_URL"));
            }
            if (envVariablePresent("TRAVIS_BUILD_NUMBER")) {
                buildScan.value("CI build number", envVariable("TRAVIS_BUILD_NUMBER"));
            }
            if (envVariablePresent("TRAVIS_JOB_NAME")) {
                String jobNameLabel = "CI job";
                String jobName = envVariable("TRAVIS_JOB_NAME");
                buildScan.value(jobNameLabel, jobName);
                addCustomLinkWithSearchTerms(buildScan, "CI job build scans", jobNameLabel, jobName);
            }
            if (envVariablePresent("TRAVIS_EVENT_TYPE")) {
                buildScan.tag(envVariable("TRAVIS_EVENT_TYPE"));
            }
        }
    }

    static void addGitMetadata(BuildScanExtension buildScan) {
        buildScan.background(api -> {
            if (!isGitInstalled()) {
                return;
            }

            String gitCommitId = execAndGetStdOut("git", "rev-parse", "--short=8", "--verify", "HEAD");
            String gitBranchName = execAndGetStdOut("git", "rev-parse", "--abbrev-ref", "HEAD");
            String gitStatus = execAndGetStdOut("git", "status", "--porcelain");

            if (gitCommitId != null) {
                String gitCommitIdLabel = "Git commit id";
                api.value(gitCommitIdLabel, gitCommitId);
                addCustomLinkWithSearchTerms(api, "Git commit id build scans", gitCommitIdLabel, gitCommitId);

                String originUrl = execAndGetStdOut("git", "config", "--get", "remote.origin.url");
                if (!isNullOrEmpty(originUrl)) {
                    if (originUrl.contains("github.com/") || originUrl.contains("github.com:")) {
                        Matcher matcher = Pattern.compile("(.*)github\\.com[/|:](.*)").matcher(originUrl);
                        if (matcher.matches()) {
                            String rawRepoPath = matcher.group(2);
                            String repoPath = rawRepoPath.endsWith(".git") ? rawRepoPath.substring(0, rawRepoPath.length() - 4) : rawRepoPath;
                            api.link("Github source", "https://github.com/" + repoPath + "/tree/" + gitCommitId);
                        }
                    } else if (originUrl.contains("gitlab.com/") || originUrl.contains("gitlab.com:")) {
                        Matcher matcher = Pattern.compile("(.*)gitlab\\.com[/|:](.*)").matcher(originUrl);
                        if (matcher.matches()) {
                            String rawRepoPath = matcher.group(2);
                            String repoPath = rawRepoPath.endsWith(".git") ? rawRepoPath.substring(0, rawRepoPath.length() - 4) : rawRepoPath;
                            api.link("GitLab Source", "https://gitlab.com/" + repoPath + "/-/commit/" + gitCommitId);
                        }
                    }
                }
            }
            if (gitBranchName != null) {
                api.tag(gitBranchName);
                api.value("Git branch", gitBranchName);
            }
            if (gitStatus != null) {
                api.tag("Dirty");
                api.value("Git status", gitStatus);
            }
        });
    }

    private static boolean isCi() {
        return isJenkins() || isTeamCity() || isCircleCI() || isBamboo() || isGitHubActions() || isGitLab() || isTravis();
    }

    private static boolean isJenkins() {
        return envVariablePresent("JENKINS_URL");
    }

    private static boolean isTeamCity() {
        return envVariablePresent("TEAMCITY_VERSION");
    }

    private static boolean isCircleCI() {
        return envVariablePresent("CIRCLE_BUILD_URL");
    }

    private static boolean isBamboo() {
        return envVariablePresent("bamboo_resultsUrl");
    }

    private static boolean isGitHubActions() {
        return envVariablePresent("GITHUB_ACTIONS");
    }

    private static boolean isGitLab() {
        return envVariablePresent("GITLAB_CI");
    }

    private static boolean isTravis() {
        return envVariablePresent("TRAVIS_JOB_ID");
    }

    private static boolean isGitInstalled() {
        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        try {
            process = runtime.exec(new String[]{"git", "--version"});
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (IOException | InterruptedException ignored) {
            return false;
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private static String execAndGetStdOut(String... args) {
        Runtime runtime = Runtime.getRuntime();
        Process process;
        try {
            process = runtime.exec(args);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (Reader standard = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
            try (Reader error = new BufferedReader(new InputStreamReader(process.getErrorStream(), Charset.defaultCharset()))) {
                String standardText = readFully(standard);
                String ignore = readFully(error);

                boolean finished = process.waitFor(10, TimeUnit.SECONDS);
                return finished && process.exitValue() == 0 ? trimAtEnd(standardText) : null;
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            process.destroyForcibly();
        }
    }

    private static void addCustomLinkWithSearchTerms(BuildScanExtension buildScan, String title, String name, String value) {
        String server = buildScan.getServer();
        if (server != null) {
            String searchParams = "search.names=" + urlEncode(name) + "&search.values=" + urlEncode(value);
            String url = appendIfMissing(server, "/") + "scans?" + searchParams + "#selection.buildScanB=" + urlEncode("{SCAN_ID}");
            buildScan.link(title, url);
        }
    }

    private static String sysProperty(String name) {
        return System.getProperty(name);
    }

    private static boolean sysPropertyPresent(String name) {
        return !isNullOrEmpty(sysProperty(name));
    }

    private static boolean sysPropertyKeyStartingWith(String keyPrefix) {
        for (Object key : System.getProperties().keySet()) {
            if (key instanceof String) {
                String stringKey = (String) key;
                if (stringKey.startsWith(keyPrefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String envVariable(String name) {
        return System.getenv(name);
    }

    private static boolean envVariablePresent(String name) {
        return !isNullOrEmpty(envVariable(name));
    }

    private static String appendIfMissing(String str, String suffix) {
        return str.endsWith(suffix) ? str : str + suffix;
    }

    private static String trimAtEnd(String str) {
        return ('x' + str).trim().substring(1);
    }

    private static String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static Properties readPropertiesFile(String name) {
        try (InputStream input = new FileInputStream(name)) {
            Properties properties = new Properties();
            properties.load(input);
            return properties;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static String readFully(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[1024];
        int nRead;
        while ((nRead = reader.read(buf)) != -1) {
            sb.append(buf, 0, nRead);
        }
        return sb.toString();
    }

    static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }

    private CustomBuildScanConfig() {
    }

}