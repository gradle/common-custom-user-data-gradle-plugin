package com.gradle;

import com.gradle.develocity.agent.gradle.adapters.BuildResultAdapter;
import com.gradle.develocity.agent.gradle.adapters.BuildScanAdapter;
import org.gradle.api.Action;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import static com.gradle.Utils.forUseAtConfigurationTime;

/**
 * Requires Gradle 6.1 or newer. Load this class only on those versions.
 * It references {@link ValueSource}, which older versions do not have.
 */
final class SuspensionDetection {

    static final long SAMPLE_INTERVAL_MILLIS = 1_000;
    // Below this, a late sample can come from a busy machine or a long GC pause rather than from a suspended one
    static final long THRESHOLD_MILLIS = 10_000;

    static final String THREAD_NAME = "Common Custom User Data suspension detector";

    private static final Logger LOGGER = Logging.getLogger(SuspensionDetection.class);

    private static Sampler sampler;

    private SuspensionDetection() {
    }

    static void apply(ProviderFactory providers, BuildScanAdapter buildScan) {
        // Gradle evaluates a value source at the start of every build, including builds that reuse the configuration cache,
        // to check whether the cache entry is still valid. BuildStarted always returns the same value, so it never invalidates the cache entry.
        forUseAtConfigurationTime(providers.of(BuildStarted.class, spec -> {
        })).get();

        buildScan.buildFinished(new CaptureSuspendedTimeAction(buildScan));
    }

    private static void startSampling() {
        startSampling(System::currentTimeMillis, SAMPLE_INTERVAL_MILLIS, THRESHOLD_MILLIS);
    }

    static synchronized void startSampling(LongSupplier wallClockMillis, long intervalMillis, long thresholdMillis) {
        if (sampler != null) {
            sampler.stop();
        }
        sampler = new Sampler(wallClockMillis, intervalMillis, thresholdMillis);
    }

    static synchronized long stopSampling() {
        if (sampler == null) {
            return 0;
        }
        Sampler finished = sampler;
        sampler = null;
        return finished.stop();
    }

    // Gradle 6.1 to 6.4 reject ValueSourceParameters.None, so declare an empty parameters type instead
    public interface NoParameters extends ValueSourceParameters {
    }

    public abstract static class BuildStarted implements ValueSource<Boolean, NoParameters> {

        @Override
        public Boolean obtain() {
            startSampling();
            return Boolean.TRUE;
        }

    }

    private static final class Sampler {

        private final SuspensionDetector detector;
        private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });

        private Sampler(LongSupplier wallClockMillis, long intervalMillis, long thresholdMillis) {
            detector = new SuspensionDetector(wallClockMillis, intervalMillis, thresholdMillis);
            executor.scheduleWithFixedDelay(detector::sample, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
        }

        private long stop() {
            executor.shutdownNow();
            // A last sample includes a suspension that ended after the previous sample
            detector.sample();
            return detector.getSuspendedMillis();
        }

    }

    static final class CaptureSuspendedTimeAction implements Action<BuildResultAdapter> {

        private final BuildScanAdapter buildScan;

        CaptureSuspendedTimeAction(BuildScanAdapter buildScan) {
            this.buildScan = buildScan;
        }

        @Override
        public void execute(BuildResultAdapter buildResult) {
            long suspendedMillis = stopSampling();
            if (suspendedMillis > 0) {
                LOGGER.info("The build process was suspended for {} ms", suspendedMillis);
                buildScan.tag("Suspended");
                buildScan.value("Suspended time (ms)", Long.toString(suspendedMillis));
            }
        }

    }

}
