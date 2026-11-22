package dev.rylunesmooth.core;

import java.util.Arrays;

/**
 * Bounded, allocation-free hot path. One sample per rendered game frame.
 * HUD/menu dwell times are excluded by the caller.
 */
public final class FrameWindow {
    private static final int CAPACITY = 360;
    private final double[] historyMs = new double[CAPACITY];
    private int size;
    private int next;

    public void clear() {
        size = 0;
        next = 0;
    }

    public void addNanos(long durationNanos) {
        // Ignore invalid samples and alt-tab/system suspend pauses.
        if (durationNanos <= 0 || durationNanos >= 1_000_000_000L) return;
        historyMs[next] = durationNanos / 1_000_000.0;
        next = (next + 1) % CAPACITY;
        if (size < CAPACITY) size++;
    }

    public Metrics snapshot() {
        if (size < 30) return new Metrics(size, 0, 0, 0, 0);
        double[] ordered = Arrays.copyOf(historyMs, size);
        Arrays.sort(ordered);
        double sum = 0;
        for (double ms : ordered) sum += ms;
        double avgMs = sum / size;
        int p95Index = Math.min(size - 1, (int) Math.ceil(size * .95) - 1);
        double p95Ms = ordered[p95Index];
        int tailSamples = Math.max(1, (int) Math.ceil(size * .01));
        double tailSum = 0;
        for (int i = size - tailSamples; i < size; i++) tailSum += ordered[i];
        double onePercentLowFps = 1000.0 / (tailSum / tailSamples);
        int longFrames = 0;
        for (double ms : ordered) if (ms >= 33.3333) longFrames++;
        return new Metrics(size, avgMs, p95Ms, onePercentLowFps, (double) longFrames / size);
    }

    public record Metrics(int count, double avgMs, double p95Ms, double low1PercentFps, double over33msRate) {
        public double averageFps() {
            return avgMs > 0 ? 1000.0 / avgMs : 0;
        }
    }
}
