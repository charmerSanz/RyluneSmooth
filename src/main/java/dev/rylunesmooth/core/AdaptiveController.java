package dev.rylunesmooth.core;

/**
 * Conservative controller that suggests one render-distance change at a time.
 * No Minecraft APIs, no threads, and no per-frame allocation.
 */
public final class AdaptiveController {
    public static final int COOLDOWN_SECONDS = 12;
    private int lowWindows;
    private int highWindows;
    private long lastChangeSecond = Long.MIN_VALUE / 2;

    public void reset() {
        lowWindows = 0;
        highWindows = 0;
        lastChangeSecond = Long.MIN_VALUE / 2;
    }

    /** Returns the recommended distance, or the same distance when no change is warranted. */
    public int suggest(FrameWindow.Metrics metrics, int current, int baseline,
                       int minimumDistance, int maxReduction, int targetFps, long elapsedSeconds) {
        if (metrics.count() < 60 || current < 2 || targetFps < 20) return current;
        if (elapsedSeconds - lastChangeSecond < COOLDOWN_SECONDS) return current;

        int min = Math.max(2, Math.min(baseline, Math.max(minimumDistance, baseline - maxReduction)));
        double fps = metrics.averageFps();
        double frameBudgetMs = 1000.0 / targetFps;

        // Require persistent poor frame times, not one one-off chunk spike.
        boolean struggling = fps < targetFps * 0.88 && metrics.p95Ms() > frameBudgetMs * 1.20;
        // Recovery deliberately takes longer than degradation: prevents oscillation.
        boolean comfortable = fps > targetFps * 1.15 && metrics.p95Ms() < frameBudgetMs * 1.15;

        lowWindows = struggling ? lowWindows + 1 : 0;
        highWindows = comfortable ? highWindows + 1 : 0;

        if (lowWindows >= 3 && current > min) {
            lowWindows = 0;
            highWindows = 0;
            lastChangeSecond = elapsedSeconds;
            return current - 1;
        }
        if (highWindows >= 6 && current < baseline) {
            lowWindows = 0;
            highWindows = 0;
            lastChangeSecond = elapsedSeconds;
            return current + 1;
        }
        return current;
    }
}
