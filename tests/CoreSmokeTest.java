import dev.rylunesmooth.core.AdaptiveController;
import dev.rylunesmooth.core.FrameWindow;
import dev.rylunesmooth.core.SmoothSettings;

import java.nio.file.Files;
import java.nio.file.Path;

public final class CoreSmokeTest {
    public static void main(String[] args) throws Exception {
        FrameWindow window = new FrameWindow();
        for (int i = 0; i < 360; i++) window.addNanos(20_000_000L);
        FrameWindow.Metrics measurements = window.snapshot();
        check(measurements.count() == 360, "ring buffer size");
        check(Math.abs(measurements.averageFps() - 50) < 0.001, "average FPS");
        check(Math.abs(measurements.p95Ms() - 20) < 0.001, "P95");
        check(Math.abs(measurements.low1PercentFps() - 50) < 0.001, "1% low");
        window.addNanos(5_000_000_000L); // alt-tab pause ignored
        check(window.snapshot().count() == 360, "long pauses ignored");
        window.clear();
        check(window.snapshot().count() == 0, "clear");

        AdaptiveController c = new AdaptiveController();
        FrameWindow.Metrics slow = new FrameWindow.Metrics(360, 25, 35, 20, .2);
        int v = 10;
        v = c.suggest(slow, v, 10, 4, 4, 60, 20);
        check(v == 10, "no instantaneous decrease");
        v = c.suggest(slow, v, 10, 4, 4, 60, 21);
        check(v == 10, "need sustained pressure");
        v = c.suggest(slow, v, 10, 4, 4, 60, 22);
        check(v == 9, "decrease after 3 slow windows");
        for (int sec = 23; sec < 34; sec++) {
            v = c.suggest(slow, v, 10, 4, 4, 60, sec);
            check(v == 9, "cooldown stable");
        }
        FrameWindow.Metrics fast = new FrameWindow.Metrics(360, 10, 12, 90, 0);
        // first subsequent polls after cooldown can start counting comfort
        for (int sec = 34; sec < 40; sec++) v = c.suggest(fast, v, 10, 4, 4, 60, sec);
        check(v == 10, "restore slowly under headroom");
        c.reset();
        FrameWindow.Metrics invalid = new FrameWindow.Metrics(3, 30, 30, 30, 0);
        check(c.suggest(invalid, 6, 8, 4, 4, 60, 100) == 6, "ignore few frames");

        Path configFile = Files.createTempDirectory("rylunesmooth-test").resolve("test.properties");
        SmoothSettings defaults = SmoothSettings.load(configFile);
        check(defaults.targetFps == 60, "default target");
        defaults.targetFps = 90;
        defaults.mode = SmoothSettings.Mode.MONITOR;
        defaults.save(configFile);
        SmoothSettings read = SmoothSettings.load(configFile);
        check(read.targetFps == 90 && read.mode == SmoothSettings.Mode.MONITOR, "config roundtrip");
        Files.deleteIfExists(configFile);
        Files.deleteIfExists(configFile.getParent());
        System.out.println("PASS: 13 core checks + frame pacing/controller/config simulations");
    }
    static void check(boolean ok, String reason) {
        if (!ok) throw new AssertionError(reason);
    }
}
