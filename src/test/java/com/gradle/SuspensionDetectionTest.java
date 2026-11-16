package com.gradle;

import com.gradle.develocity.agent.gradle.adapters.BuildResultAdapter;
import com.gradle.develocity.agent.gradle.adapters.BuildScanAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SuspensionDetectionTest {

    private static final long INTERVAL_MILLIS = 1_000;
    private static final long THRESHOLD_MILLIS = 10_000;
    private static final BuildResultAdapter SUCCESSFUL_BUILD = Collections::emptyList;
    private static final BuildResultAdapter FAILED_BUILD = () -> Collections.singletonList(new RuntimeException("boom"));

    private final AtomicLong now = new AtomicLong(1_000_000);
    private final List<String> captured = new ArrayList<>();
    private final BuildScanAdapter buildScan = recordingBuildScan(captured);

    @AfterEach
    public void stopSampling() {
        SuspensionDetection.stopSampling();
    }

    @Test
    public void addsNothingWhenTheBuildWasNotSuspended() {
        SuspensionDetection.startSampling(now::get, INTERVAL_MILLIS, THRESHOLD_MILLIS);

        finishBuild(SUCCESSFUL_BUILD);

        assertEquals(Collections.emptyList(), captured);
    }

    @Test
    public void addsTagAndValueWhenTheBuildWasSuspended() {
        SuspensionDetection.startSampling(now::get, INTERVAL_MILLIS, THRESHOLD_MILLIS);
        now.addAndGet(INTERVAL_MILLIS + 60_000);

        finishBuild(SUCCESSFUL_BUILD);

        assertEquals(suspendedBy(60_000), captured);
    }

    @Test
    public void addsTagAndValueWhenTheSuspendedBuildFailed() {
        SuspensionDetection.startSampling(now::get, INTERVAL_MILLIS, THRESHOLD_MILLIS);
        now.addAndGet(INTERVAL_MILLIS + 60_000);

        finishBuild(FAILED_BUILD);

        assertEquals(suspendedBy(60_000), captured);
    }

    @Test
    public void doesNotCarrySuspendedTimeIntoTheNextBuild() {
        SuspensionDetection.startSampling(now::get, INTERVAL_MILLIS, THRESHOLD_MILLIS);
        now.addAndGet(INTERVAL_MILLIS + 60_000);
        finishBuild(SUCCESSFUL_BUILD);
        captured.clear();

        SuspensionDetection.startSampling(now::get, INTERVAL_MILLIS, THRESHOLD_MILLIS);
        finishBuild(SUCCESSFUL_BUILD);

        assertEquals(Collections.emptyList(), captured);
    }

    @Test
    public void restartingDiscardsTheUnfinishedBuild() {
        SuspensionDetection.startSampling(now::get, INTERVAL_MILLIS, THRESHOLD_MILLIS);
        now.addAndGet(INTERVAL_MILLIS + 60_000);

        SuspensionDetection.startSampling(now::get, INTERVAL_MILLIS, THRESHOLD_MILLIS);
        finishBuild(SUCCESSFUL_BUILD);

        assertEquals(Collections.emptyList(), captured);
    }

    @Test
    public void reportsNothingWhenSamplingNeverStarted() {
        assertEquals(0, SuspensionDetection.stopSampling());
    }

    @Test
    public void runsTheSamplerOnADaemonThreadUntilTheBuildFinishes() throws InterruptedException {
        SuspensionDetection.startSampling(now::get, INTERVAL_MILLIS, THRESHOLD_MILLIS);

        Thread sampler = samplerThread();
        assertTrue(sampler.isDaemon());

        finishBuild(SUCCESSFUL_BUILD);

        // shutdownNow does not wait for the thread to end, so give it a moment
        sampler.join(5_000);
        assertFalse(sampler.isAlive());
    }

    private void finishBuild(BuildResultAdapter result) {
        new SuspensionDetection.CaptureSuspendedTimeAction(buildScan).execute(result);
    }

    private static List<String> suspendedBy(long millis) {
        List<String> expected = new ArrayList<>();
        expected.add("tag Suspended");
        expected.add("value Suspended time (ms)=" + millis);
        return expected;
    }

    private static Thread samplerThread() {
        return Thread.getAllStackTraces().keySet().stream()
            .filter(thread -> thread.getName().equals(SuspensionDetection.THREAD_NAME))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No thread named " + SuspensionDetection.THREAD_NAME));
    }

    private static BuildScanAdapter recordingBuildScan(List<String> captured) {
        return (BuildScanAdapter) Proxy.newProxyInstance(
            SuspensionDetectionTest.class.getClassLoader(),
            new Class<?>[]{BuildScanAdapter.class},
            (proxy, method, args) -> {
                if (method.getName().equals("tag")) {
                    captured.add("tag " + args[0]);
                } else if (method.getName().equals("value")) {
                    captured.add("value " + args[0] + "=" + args[1]);
                } else {
                    throw new UnsupportedOperationException(method.getName());
                }
                return null;
            });
    }

}
