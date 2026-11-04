package com.gradle;

import java.util.function.LongSupplier;

/**
 * Counts the wall-clock time beyond the sample interval as suspended once it reaches the threshold.
 * The detector uses the wall clock because {@link System#nanoTime()} does not advance while macOS and Linux machines sleep.
 * It also counts a forward jump of the wall clock by at least the threshold as suspended time.
 */
final class SuspensionDetector {

    private final LongSupplier wallClockMillis;
    private final long intervalMillis;
    private final long thresholdMillis;

    private long lastSampleMillis;
    private long suspendedMillis;

    SuspensionDetector(LongSupplier wallClockMillis, long intervalMillis, long thresholdMillis) {
        this.wallClockMillis = wallClockMillis;
        this.intervalMillis = intervalMillis;
        this.thresholdMillis = thresholdMillis;
        this.lastSampleMillis = wallClockMillis.getAsLong();
    }

    synchronized void sample() {
        long now = wallClockMillis.getAsLong();
        long delay = now - lastSampleMillis - intervalMillis;
        if (delay >= thresholdMillis) {
            suspendedMillis += delay;
        }
        lastSampleMillis = now;
    }

    synchronized long getSuspendedMillis() {
        return suspendedMillis;
    }

}
