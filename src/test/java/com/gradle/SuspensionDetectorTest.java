package com.gradle;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SuspensionDetectorTest {

    private static final long INTERVAL_MILLIS = 1_000;
    private static final long THRESHOLD_MILLIS = 10_000;

    private final AtomicLong now = new AtomicLong(1_000_000);
    private final SuspensionDetector detector = new SuspensionDetector(now::get, INTERVAL_MILLIS, THRESHOLD_MILLIS);

    @Test
    public void reportsNothingWhenSamplesArriveOnTime() {
        for (int i = 0; i < 60; i++) {
            advanceAndSample(INTERVAL_MILLIS);
        }

        assertEquals(0, detector.getSuspendedMillis());
    }

    @Test
    public void ignoresDelaysBelowThreshold() {
        advanceAndSample(INTERVAL_MILLIS + THRESHOLD_MILLIS - 1);

        assertEquals(0, detector.getSuspendedMillis());
    }

    @Test
    public void countsDelayEqualToThreshold() {
        advanceAndSample(INTERVAL_MILLIS + THRESHOLD_MILLIS);

        assertEquals(THRESHOLD_MILLIS, detector.getSuspendedMillis());
    }

    @Test
    public void countsTimeBeyondTheSampleIntervalAsSuspended() {
        advanceAndSample(INTERVAL_MILLIS);
        advanceAndSample(INTERVAL_MILLIS + 8 * 3_600_000);
        advanceAndSample(INTERVAL_MILLIS);

        assertEquals(8 * 3_600_000, detector.getSuspendedMillis());
    }

    @Test
    public void addsUpSeveralSuspensions() {
        advanceAndSample(INTERVAL_MILLIS + 60_000);
        advanceAndSample(INTERVAL_MILLIS);
        advanceAndSample(INTERVAL_MILLIS + 30_000);

        assertEquals(90_000, detector.getSuspendedMillis());
    }

    @Test
    public void ignoresWallClockSetBackwards() {
        advanceAndSample(-3_600_000);
        advanceAndSample(INTERVAL_MILLIS);

        assertEquals(0, detector.getSuspendedMillis());
    }

    private void advanceAndSample(long millis) {
        now.addAndGet(millis);
        detector.sample();
    }

}
