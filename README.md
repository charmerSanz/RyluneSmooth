<div align="center">
  <img src="src/main/resources/assets/rylunesmooth/icon.png" alt="RyluneSmooth icon" width="140" />

  # RyluneSmooth

  **Smoother frames. More consistent gameplay.**

  Lightweight, client-side performance monitoring and adaptive render-distance tuning for **Minecraft Java Edition 1.21.11 (Fabric)**.

  **Version 0.1.0** · **Client-side** · **Fabric**
</div>

---

## Overview

RyluneSmooth focuses on **frame-time consistency** rather than maximizing the FPS counter. It measures how consistently Minecraft presents frames and uses those measurements to make conservative adjustments to **client render distance** when performance remains below a chosen target.

When the game has sufficient performance headroom again, RyluneSmooth gradually restores the original render distance. The process is automatic in Adaptive mode, reversible, and does not alter server simulation or world generation.

## Features

### 1. Real-time frame monitoring

RyluneSmooth records the time between consecutive rendered frames during gameplay. The sampler uses a **fixed-size circular buffer of 360 frame intervals**, preventing unbounded memory growth and avoiding per-frame allocation in its sampling path.

The following metrics are calculated from recent samples:

| Metric | What it measures |
| --- | --- |
| **Average FPS** | Frames per second calculated from the average recorded frame interval. |
| **P95 frame time** | The frame duration at the 95th percentile, highlighting slower frames that the average can hide. |
| **Approximate 1% low FPS** | FPS derived from the average duration of the slowest 1% of sampled frames; useful for identifying stutter. |

Sampling pauses in menus and outside an active world. Invalid intervals and unusually long pauses (at least one second) are ignored. The rolling metrics are refreshed about once every 20 client ticks rather than recalculated on every rendered frame.

### 2. Adaptive render-distance optimization

In **Adaptive** mode, RyluneSmooth evaluates recent frame-time statistics against the selected FPS target. When it identifies a **sustained** performance shortfall, it decreases the client's render distance **one chunk at a time**.

Reducing render distance can lower the number of visible chunks Minecraft needs to render and maintain. This may improve frame times when world rendering contributes to the bottleneck.

By default, the mod:

- Begins tuning only after **10 seconds of gameplay sampling** in a world.
- Requires **three consecutive low-performance evaluations** before reducing distance.
- Waits at least **12 seconds between automatic distance changes**.
- Allows a maximum reduction of **four chunks below the user's baseline**, without crossing the configured minimum distance (default: **4 chunks**).

For a **60 FPS target**, the controller considers performance below target when average FPS is under **52.8 FPS** *and* P95 frame time exceeds **20 ms**. Both conditions must persist; a single lag spike does not immediately trigger a change.

> Adaptive adjustments affect **client render distance only**. They do not change simulation distance, tick speed, server render distance, or terrain-generation algorithms.

### 3. Gradual render-distance recovery

RyluneSmooth can restore visual range when performance consistently improves. Recovery is intentionally more conservative than reduction to avoid constantly switching between distances.

- Requires **six consecutive high-performance evaluations** before increasing distance.
- Increases distance **one chunk at a time**, subject to the same 12-second cooldown.
- Never automatically exceeds the user's original baseline render distance.

For a **60 FPS target**, the recovery condition requires average FPS above **69 FPS** and P95 frame time below approximately **19.2 ms**. If Minecraft is capped at the target FPS, the recovery condition might never be met, even when more rendering headroom is available.

### 4. Performance HUD

An optional in-game HUD shows live statistics without opening the debug screen:

```text
RyluneSmooth [ADAPTIVE]  86 FPS  P95 15.2 ms
1% low 57 FPS  10 chunks  target 90
```

*Values shown above are illustrative, not benchmark results.*

The overlay displays the active mode, average FPS, P95 frame time, approximate 1% low FPS, current render distance, and target FPS. It is **hidden by default** and can be toggled at any time.

### 5. Three operating modes

RyluneSmooth separates **performance measurement** from **automatic optimization**, so you can choose whether the mod only observes the game or actively adjusts render distance.

| Mode | Performance monitoring | Changes render distance | Purpose |
| --- | --- | --- | --- |
| **Adaptive** | Yes | Yes, when sustained conditions are met | Automatically balance visible distance against frame-time consistency. |
| **Monitor** | Yes | No | Measure your game's current performance while leaving your graphics settings alone. |
| **Off** | Lightweight frame sampling remains active internally | No | Stop adaptive adjustments and restore the saved baseline distance. |

#### Monitor mode — measurement without adjustments

**Monitor** mode keeps the performance statistics running, but **never lowers or raises your render distance**. It tracks the same recent frame intervals used in Adaptive mode and makes the resulting statistics available through the optional F8 HUD:

- **Average FPS** indicates the overall frame rate during the recent sampling window.
- **P95 frame time** highlights slower frames that can make the game feel uneven even when average FPS looks high.
- **Approximate 1% low FPS** provides a view of the worst-performing frames in the window.
- **Current render distance and FPS target** remain visible for reference, even though Monitor mode does not use the target to adjust settings.

Monitor mode is useful for comparing gameplay before and after enabling Adaptive mode, checking performance in different worlds or locations, and seeing whether frame-time spikes happen without allowing RyluneSmooth to change your settings. **The HUD is optional**: Monitor mode still records statistics when the overlay is hidden.

Switching from Adaptive to Monitor attempts to restore the render-distance baseline that was in place before automatic tuning and resets the controller's adjustment history. Monitor mode is **not** a separate FPS booster or a frame-rate limiter.

#### Adaptive and Off modes

**Adaptive** mode uses the collected statistics and selected FPS target to decide whether to decrease or gradually restore render distance. Changes are limited by the configured minimum, maximum reduction, consecutive-evaluation requirements, and cooldown.

**Off** disables automatic tuning and attempts to restore your baseline render distance. The lightweight frame sampler remains active internally; Off does not uninstall the mod or independently change Minecraft's FPS cap.

### 6. Customizable FPS targets

An **FPS target** tells RyluneSmooth what performance level its *Adaptive controller* should aim to support. It is a **decision threshold**, not a frame-rate cap, FPS boost, or guarantee that the computer will reach that number.

The built-in presets are **45, 60, 75, 90, and 120 FPS**. Pressing **F10** cycles through them; the selected value is saved between sessions. You can also specify a custom `target_fps` in `config/rylunesmooth.properties`.

#### How the target affects optimization

1. The mod measures average FPS and P95 frame time from recent gameplay frames.
2. In **Adaptive** mode, it compares both readings against thresholds derived from the selected FPS target.
3. If performance remains below those thresholds for several evaluations, it may lower render distance by one chunk, subject to the cooldown and configured limits.
4. If performance stays comfortably above the recovery thresholds, it may restore render distance one chunk at a time, up to the saved baseline.

For example, with a **60 FPS target**, automatic reduction is considered when **average FPS falls below 52.8** *and* **P95 frame time exceeds 20 ms** across three consecutive evaluations. Recovery is considered when **average FPS exceeds 69** *and* **P95 frame time is below approximately 19.2 ms** across six evaluations. Automatic changes have a **12-second cooldown**.

With a **90 FPS target**, the controller uses a more demanding performance reference than with a 60 FPS target. If your laptop cannot consistently reach the selected target, the mod may reach its permitted minimum render distance without ever achieving that FPS. Setting an unrealistically high target therefore does not create extra performance by itself.

#### What FPS targets do in each mode

| Mode | Effect of changing FPS target |
| --- | --- |
| **Adaptive** | Changes the thresholds used to decide when to reduce or restore render distance. |
| **Monitor** | Updates the displayed/saved target for reference, but does not change render distance. |
| **Off** | Saves the selected target for later use; no automatic tuning occurs. |

**FPS target vs. FPS limit:** Minecraft's own *Max Framerate* and VSync settings determine whether frames are capped. RyluneSmooth's target does not override those settings. If Minecraft is capped at 60 FPS while the RyluneSmooth target is 60 FPS, the controller may not observe enough headroom to restore render distance automatically. For meaningful comparisons, keep the game's FPS cap and VSync settings consistent.

### 7. Automatic settings restoration

The mod tracks the render distance the player selected before adaptive adjustments. It attempts to restore that baseline when:

- Adaptive mode is disabled.
- The active world changes or closes.
- Minecraft shuts down normally.

If the player manually changes render distance while playing, RyluneSmooth detects the change, treats it as the **new baseline**, and resets its controller. Restoration is best-effort and applies only when the current distance still matches the value the mod last set, avoiding overwriting a later manual change.

### 8. Lightweight client-side operation

RyluneSmooth is designed to minimize its own runtime overhead:

- Uses a **bounded frame history** instead of accumulating unlimited samples.
- Performs only a small timestamp update on each rendered gameplay frame.
- Computes statistics periodically, rather than sorting frame samples every frame.
- Uses cooldowns and consecutive-condition checks to limit repeated graphics-setting changes.
- Performs no server-side optimization or gameplay simulation changes.

Because it operates on the client, its monitoring and adaptive render-distance functionality can be used in both **singleplayer and multiplayer**.

## Controls

| Default key | Action |
| --- | --- |
| **F8** | Toggle the performance HUD. |
| **F9** | Cycle **Adaptive → Monitor → Off → Adaptive**. |
| **F10** | Cycle **45 → 60 → 75 → 90 → 120 FPS** targets. |

Keys can be rebound in Minecraft's Controls settings.

## Configuration

RyluneSmooth stores its preferences in `config/rylunesmooth.properties`.

```properties
mode=ADAPTIVE
show_hud=false
target_fps=60
minimum_render_distance=4
maximum_distance_reduction=4
```

| Property | Description |
| --- | --- |
| `mode` | Active operating mode: `ADAPTIVE`, `MONITOR`, or `OFF`. |
| `show_hud` | Whether the performance overlay is visible. |
| `target_fps` | FPS target used by the adaptive controller. |
| `minimum_render_distance` | Lower bound for automatic render-distance reduction. |
| `maximum_distance_reduction` | Maximum reduction allowed relative to the baseline distance. |

Changes made using the in-game shortcuts are saved automatically. Manual file changes are read when the mod initializes.

## How the adaptive controller decides

The controller combines **average FPS** with **P95 frame time**, rather than relying on one metric alone:

1. **Collect:** Record recent gameplay frame intervals.
2. **Evaluate:** Compare average FPS and P95 frame time with the selected target.
3. **Confirm:** Require several consecutive evaluations to distinguish sustained slowdown from isolated spikes.
4. **Adjust:** Reduce or restore render distance by one chunk when conditions and cooldown permit.
5. **Respect:** Keep changes within the configured limits and preserve the user's baseline settings.

This conservative approach is intended to avoid rapid adjustments that could create additional stutter.

## Scope and limitations

RyluneSmooth **v0.1.0** is an experimental performance-monitoring and render-distance management mod. Its current implementation **does not** directly accelerate chunk generation, optimize CPU thread scheduling, reduce Java heap usage, modify GPU drivers, implement dynamic resolution, or introduce a new rendering engine.

Performance improvements are **not guaranteed**. Reducing render distance is most useful when rendering visible chunks is a meaningful bottleneck; it may do little for simulation, driver, shader, network, or unrelated loading stalls. Changing render distance can itself briefly trigger chunk-related work. Frame-time measurements also reflect FPS limits and VSync, not just hardware workload.

The source prototype has **not yet been fully validated or benchmarked in Minecraft**, including compatibility testing with other rendering and performance mods.

## Credits

**RyluneSmooth — Smoother Frames, Better Gameplay.**

**made by Cps**