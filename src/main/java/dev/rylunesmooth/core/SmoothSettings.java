package dev.rylunesmooth.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class SmoothSettings {
    public enum Mode { ADAPTIVE, MONITOR, OFF }

    public Mode mode = Mode.ADAPTIVE;
    public boolean showHud = false;
    public int targetFps = 60;
    public int minimumDistance = 4;
    public int maximumReduction = 4;

    public static SmoothSettings load(Path file) {
        SmoothSettings settings = new SmoothSettings();
        if (!Files.exists(file)) return settings;
        Properties p = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            p.load(input);
            settings.mode = Mode.valueOf(p.getProperty("mode", "ADAPTIVE").trim().toUpperCase());
            settings.showHud = Boolean.parseBoolean(p.getProperty("show_hud", "false"));
            settings.targetFps = clamp(Integer.parseInt(p.getProperty("target_fps", "60")), 30, 240);
            settings.minimumDistance = clamp(Integer.parseInt(p.getProperty("minimum_render_distance", "4")), 2, 20);
            settings.maximumReduction = clamp(Integer.parseInt(p.getProperty("maximum_distance_reduction", "4")), 0, 16);
        } catch (IOException | IllegalArgumentException ignored) {
            // Invalid/partially written user config falls back to defaults.
        }
        return settings;
    }

    public void save(Path file) {
        Properties p = new Properties();
        p.setProperty("mode", mode.name());
        p.setProperty("show_hud", Boolean.toString(showHud));
        p.setProperty("target_fps", Integer.toString(targetFps));
        p.setProperty("minimum_render_distance", Integer.toString(minimumDistance));
        p.setProperty("maximum_distance_reduction", Integer.toString(maximumReduction));
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream output = Files.newOutputStream(file)) {
                p.store(output, "RyluneSmooth 0.1.0 | Restart to apply manual config edits");
            }
        } catch (IOException ignored) {
            // Mod should never crash the client because of config permissions.
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
