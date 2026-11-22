package dev.rylunesmooth.client;

import dev.rylunesmooth.core.AdaptiveController;
import dev.rylunesmooth.core.FrameWindow;
import dev.rylunesmooth.core.SmoothSettings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.Locale;

public final class RyluneSmoothClient implements ClientModInitializer {
    private static final FrameWindow FRAME_WINDOW = new FrameWindow();
    private static final AdaptiveController CONTROLLER = new AdaptiveController();
    private static FrameWindow.Metrics metrics = FRAME_WINDOW.snapshot();
    private static SmoothSettings settings;
    private static Path settingsFile;
    private static KeyBinding toggleHud;
    private static KeyBinding changeMode;
    private static KeyBinding changeTarget;
    private static long lastFrameNanos;
    private static long secondCounter;
    private static int ticks;
    private static int baselineViewDistance = -1;
    private static int appliedDistance = -1;
    private static Object lastWorld;
    private static String firstHudLine = "RyluneSmooth: collecting samples...";
    private static String secondHudLine = "F8 HUD  F9 mode  F10 target";

    @Override
    public void onInitializeClient() {
        settingsFile = net.fabricmc.loader.api.FabricLoader.getInstance()
                .getConfigDir().resolve("rylunesmooth.properties");
        settings = SmoothSettings.load(settingsFile);
        settings.save(settingsFile);

        toggleHud = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rylunesmooth.toggle_hud", GLFW.GLFW_KEY_F8, KeyBinding.Category.MISC));
        changeMode = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rylunesmooth.toggle_mode", GLFW.GLFW_KEY_F9, KeyBinding.Category.MISC));
        changeTarget = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rylunesmooth.target_fps", GLFW.GLFW_KEY_F10, KeyBinding.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(RyluneSmoothClient::onClientTick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(RyluneSmoothClient::onStopping);
        HudElementRegistry.addLast(Identifier.of("rylunesmooth", "metrics"), RyluneSmoothClient::drawHud);
    }

    // Called at the HEAD of MinecraftClient.render; about as small as possible.
    public static void onFrameStart(long nowNanos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.currentScreen != null) {
            lastFrameNanos = 0;
            return;
        }
        if (lastFrameNanos != 0 && nowNanos > lastFrameNanos) {
            FRAME_WINDOW.addNanos(nowNanos - lastFrameNanos);
        }
        lastFrameNanos = nowNanos;
    }

    private static void onClientTick(MinecraftClient client) {
        while (toggleHud.wasPressed()) {
            settings.showHud = !settings.showHud;
            settings.save(settingsFile);
        }
        while (changeMode.wasPressed()) {
            settings.mode = switch (settings.mode) {
                case ADAPTIVE -> SmoothSettings.Mode.MONITOR;
                case MONITOR -> SmoothSettings.Mode.OFF;
                case OFF -> SmoothSettings.Mode.ADAPTIVE;
            };
            if (settings.mode != SmoothSettings.Mode.ADAPTIVE) restoreBaseline(client);
            CONTROLLER.reset();
            settings.save(settingsFile);
        }
        while (changeTarget.wasPressed()) {
            settings.targetFps = switch (settings.targetFps) {
                case 45 -> 60;
                case 60 -> 75;
                case 75 -> 90;
                case 90 -> 120;
                default -> 45;
            };
            CONTROLLER.reset();
            settings.save(settingsFile);
        }

        // Do not tune in menus or while loading/switching worlds.
        if (client.world != lastWorld) {
            restoreBaseline(client);
            lastWorld = client.world;
            resetSampling();
            secondCounter = 0;
        }
        if (client.world == null || client.currentScreen != null) {
            resetSampling();
            return;
        }

        // Respect a user's own manual render-distance changes.
        int actualDistance = client.options.getViewDistance().getValue();
        if (baselineViewDistance < 0) {
            baselineViewDistance = actualDistance;
            appliedDistance = actualDistance;
        } else if (actualDistance != appliedDistance) {
            baselineViewDistance = actualDistance;
            appliedDistance = actualDistance;
            CONTROLLER.reset();
        }

        ticks++;
        if (ticks < 20) return;
        ticks = 0;
        secondCounter++;
        metrics = FRAME_WINDOW.snapshot();
        firstHudLine = String.format(Locale.ROOT, "RyluneSmooth [%s]  %.0f FPS  P95 %.1f ms",
                settings.mode, metrics.averageFps(), metrics.p95Ms());
        secondHudLine = String.format(Locale.ROOT, "1%% low %.0f FPS  %d chunks  target %d",
                metrics.low1PercentFps(), actualDistance, settings.targetFps);
        if (settings.mode != SmoothSettings.Mode.ADAPTIVE || secondCounter < 10) return;

        int suggested = CONTROLLER.suggest(metrics, actualDistance, baselineViewDistance,
                settings.minimumDistance, settings.maximumReduction, settings.targetFps, secondCounter);
        if (suggested != actualDistance) {
            client.options.getViewDistance().setValue(suggested);
            appliedDistance = suggested;
        }
    }

    private static void resetSampling() {
        FRAME_WINDOW.clear();
        metrics = FRAME_WINDOW.snapshot();
        lastFrameNanos = 0;
        ticks = 0;
    }

    private static void restoreBaseline(MinecraftClient client) {
        if (baselineViewDistance >= 0 && appliedDistance >= 0 &&
                client.options.getViewDistance().getValue() == appliedDistance) {
            client.options.getViewDistance().setValue(baselineViewDistance);
        }
        baselineViewDistance = -1;
        appliedDistance = -1;
        CONTROLLER.reset();
    }

    private static void onStopping(MinecraftClient client) {
        restoreBaseline(client);
    }

    private static void drawHud(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (settings == null || !settings.showHud || client.world == null) return;

        int x = 7, y = 7;
        context.fill(x - 3, y - 3, x + 258, y + 25, 0x990A101A);
        context.drawTextWithShadow(client.textRenderer, firstHudLine, x, y, 0xFFE7F4FF);
        context.drawTextWithShadow(client.textRenderer, secondHudLine, x, y + 11, 0xFF9EDBFF);
    }
}
