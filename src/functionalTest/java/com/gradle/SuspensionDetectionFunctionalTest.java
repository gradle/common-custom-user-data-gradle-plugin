package com.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// SIGSTOP freezes the Gradle daemon the way a sleeping laptop does: every thread stops, and the wall clock keeps running
@EnabledOnOs({OS.LINUX, OS.MAC})
public class SuspensionDetectionFunctionalTest {

    private static final Pattern SUSPENDED_LINE = Pattern.compile("The build process was suspended for (\\d+) ms");
    // Longer than the detection threshold plus one sample interval, so the delay always counts
    private static final long SUSPEND_MILLIS = 13_000;

    private static final String AWAIT_RELEASE = String.join("\n",
        "class Signals {",
        "static void awaitRelease(File signals) {",
        "    signals.mkdirs()",
        "    def pid = new File(signals, 'pid.tmp')",
        "    pid.text = ProcessHandle.current().pid().toString()",
        "    java.nio.file.Files.move(pid.toPath(), new File(signals, 'pid').toPath(), java.nio.file.StandardCopyOption.ATOMIC_MOVE)",
        "    def release = new File(signals, 'release')",
        "    long deadline = System.nanoTime() + 120_000_000_000L",
        "    while (!release.exists()) {",
        "        if (System.nanoTime() > deadline) throw new GradleException('The test never released the build')",
        "        Thread.sleep(50)",
        "    }",
        "}",
        "}",
        "");

    @TempDir
    Path projectDir;

    private Path signals;

    @BeforeEach
    public void createProject() throws IOException {
        signals = projectDir.resolve("signals");
        write("settings.gradle", settingsScript("suspended", ""));
        write("build.gradle", AWAIT_RELEASE + String.join("\n",
            "abstract class AwaitRelease extends DefaultTask {",
            "    @Internal abstract DirectoryProperty getSignals()",
            "    @TaskAction void run() { Signals.awaitRelease(signals.get().asFile) }",
            "}",
            "tasks.register('awaitRelease', AwaitRelease) {",
            "    signals = layout.projectDirectory.dir('signals')",
            "}",
            ""));
    }

    @Test
    public void detectsSuspensionWhenTheConfigurationCacheIsReused() throws Exception {
        BuildResult storing = runReleasedAtOnce();
        assertNotSuspended(storing);

        BuildResult reusing = runSuspended();

        assertTrue(reusing.getOutput().contains("Reusing configuration cache."), reusing.getOutput());
        assertSuspendedAtLeast(reusing, SUSPEND_MILLIS - SuspensionDetection.SAMPLE_INTERVAL_MILLIS);
    }

    @Test
    public void doesNotCarrySuspendedTimeIntoTheNextBuildOnTheSameDaemon() throws Exception {
        BuildResult suspended = runSuspended();
        long suspendedDaemon = readPid();
        assertSuspendedAtLeast(suspended, SUSPEND_MILLIS - SuspensionDetection.SAMPLE_INTERVAL_MILLIS);

        BuildResult next = runReleasedAtOnce();

        assertEquals(suspendedDaemon, readPid(), "both builds must run on the same daemon");
        assertNotSuspended(next);
    }

    @Test
    public void includedBuildDoesNotRestartTheSampler() throws Exception {
        // The pause sits in the root settings script, before Gradle evaluates the included build's settings
        write("settings.gradle", settingsScript("suspended", AWAIT_RELEASE + String.join("\n",
            "Signals.awaitRelease(new File(settingsDir, 'signals'))",
            "includeBuild('included')",
            "")));
        write("build.gradle", "tasks.register('awaitRelease')\n");
        write("included/settings.gradle", settingsScript("included", ""));

        BuildResult result = runSuspended();

        assertSuspendedAtLeast(result, SUSPEND_MILLIS - SuspensionDetection.SAMPLE_INTERVAL_MILLIS);
    }

    private BuildResult runReleasedAtOnce() throws IOException {
        resetSignals();
        Files.createFile(signals.resolve("release"));
        return runner().build();
    }

    private BuildResult runSuspended() throws Exception {
        resetSignals();
        CompletableFuture<BuildResult> build = CompletableFuture.supplyAsync(() -> runner().build());
        String pid = Long.toString(awaitPid(build));

        signal("-STOP", pid);
        try {
            Thread.sleep(SUSPEND_MILLIS);
        } finally {
            signal("-CONT", pid);
        }

        Files.createFile(signals.resolve("release"));
        return build.get(2, TimeUnit.MINUTES);
    }

    private long awaitPid(CompletableFuture<BuildResult> build) throws Exception {
        Path pid = signals.resolve("pid");
        long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(2);
        while (!Files.exists(pid)) {
            if (build.isDone()) {
                build.get();
                throw new AssertionError("The build finished before it reached the pause");
            }
            if (System.nanoTime() > deadline) {
                throw new TimeoutException("The build never reached the pause");
            }
            Thread.sleep(50);
        }
        return readPid();
    }

    private long readPid() throws IOException {
        return Long.parseLong(new String(Files.readAllBytes(signals.resolve("pid")), StandardCharsets.UTF_8).trim());
    }

    private void resetSignals() throws IOException {
        Files.createDirectories(signals);
        Files.deleteIfExists(signals.resolve("pid"));
        Files.deleteIfExists(signals.resolve("release"));
    }

    private GradleRunner runner() {
        return GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments("awaitRelease", "--configuration-cache", "--info");
    }

    private static void signal(String signal, String pid) throws IOException, InterruptedException, ExecutionException {
        Process kill = new ProcessBuilder("kill", signal, pid).inheritIO().start();
        assertEquals(0, kill.waitFor(), "kill " + signal + " " + pid);
    }

    private static void assertSuspendedAtLeast(BuildResult result, long minimumMillis) {
        Matcher matcher = SUSPENDED_LINE.matcher(result.getOutput());
        assertTrue(matcher.find(), result.getOutput());
        long suspendedMillis = Long.parseLong(matcher.group(1));
        assertTrue(suspendedMillis >= minimumMillis, "suspended for " + suspendedMillis + " ms, expected at least " + minimumMillis);
        assertFalse(matcher.find(), "reported more than once: " + result.getOutput());
    }

    private static void assertNotSuspended(BuildResult result) {
        assertFalse(SUSPENDED_LINE.matcher(result.getOutput()).find(), result.getOutput());
    }

    private static String settingsScript(String name, String body) {
        return String.join("\n",
            "plugins {",
            "    id 'com.gradle.develocity' version '4.6.1'",
            "    id 'com.gradle.common-custom-user-data-gradle-plugin'",
            "}",
            "develocity {",
            "    buildScan {",
            "        publishing.onlyIf { false }",
            "    }",
            "}",
            "rootProject.name = '" + name + "'",
            body);
    }

    private void write(String path, String content) throws IOException {
        Path file = projectDir.resolve(path);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

}
