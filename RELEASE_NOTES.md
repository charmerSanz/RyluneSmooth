# RyluneSmooth v0.1.0

**Initial experimental release for Minecraft Java Edition 1.21.11 (Fabric).**

RyluneSmooth aims to improve frame-time consistency on low-end PCs by monitoring gameplay and conservatively adjusting *client render distance* when performance remains below the chosen target.

## Features
- Lightweight rolling frame-time sampler (360 samples).
- Live average FPS, P95 frame time, and approximate 1% low FPS metrics.
- Adaptive render-distance tuning: one chunk per adjustment, up to four below the original setting by default.
- Gradual recovery toward the original distance when performance headroom returns.
- Optional in-game performance HUD (**F8**).
- Three operation modes: **Adaptive / Monitor / Off** (**F9**).
- Target FPS options: **45 / 60 / 75 / 90 / 120** (**F10**).
- Saved configuration and reversible settings.

## Compatibility
- **Minecraft:** 1.21.11
- **Loader:** Fabric (Loader 0.18.4+)
- **Dependency:** Fabric API for 1.21.11
- **Java:** 21 or newer
- **Environment:** Client-side; singleplayer and multiplayer

> **Experimental:** FPS improvements are not guaranteed. This version does not replace Sodium or optimize terrain generation, CPU scheduling, or GPU memory management. Use Monitor mode to compare performance without automatic setting changes.

**made by Cps**
