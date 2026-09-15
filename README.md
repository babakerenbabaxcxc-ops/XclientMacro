# X Client

**Author:** ridzzxmc ([@ridzzxmc](https://tiktok.com/@ridzzxmc))

This mod is created and maintained by ridzzxmc. Please do not redistribute,
rebrand, or claim this project as your own work without permission.

A modular Fabric client for Minecraft **1.21.11** - performance, modular
HUDs, and a red-themed "Mod Menu" ClickGUI. No blatant cheats: nothing here
reads through walls, auto-aims, auto-clicks unrealistically, or breaks
server hit-registration.

> **Version note:** 1.21.11 is the last obfuscated, Yarn-mapped Minecraft
> release before Fabric's tooling moved to Mojang mappings for the versions
> after it. `gradle.properties` is already pinned to the matching toolchain
> (`yarn 1.21.11+build.4`, `Fabric Loader 0.19.3`, `Fabric API
> 0.141.5+1.21.11`) and `build.gradle` uses Loom `1.14.10`, the version
> Fabric's own 1.21.11 announcement points developers at. If you move this
> project to a newer Minecraft version later, bump all four together -
> mismatched mappings/loader/API versions are the most common source of
> build failures in Fabric projects.

---

## 1. Requirements

- JDK 21 (Temurin/Adoptium recommended)
- Git (optional, for version control)
- ~4 GB free disk for Gradle + Minecraft/Yarn caches on first build

## 2. Getting the Gradle wrapper

This template ships without the wrapper jar (binary files aren't included).
Generate it once, from a machine with Gradle 8.8+ installed:

```bash
gradle wrapper --gradle-version 8.8
```

This creates `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar`.
Commit these to your repo afterward - from then on, collaborators just run
`./gradlew` without needing Gradle installed separately.

## 3. Build

```bash
./gradlew build
```

First run downloads Minecraft, Yarn mappings, and Fabric Loader/API - this
can take several minutes. The compiled mod jar lands in:

```
build/libs/x-client-1.0.0.jar
```

## 4. Run in a dev client (fastest way to test)

```bash
./gradlew runClient
```

This launches a dev-environment Minecraft client with X Client already
loaded and Fabric API alongside it - no need to install a separate launcher.

## 5. Installing into a real Fabric instance

1. Install Fabric Loader for 1.21.11 (via the official Fabric installer).
2. Drop `fabric-api-<version>.jar` and `build/libs/x-client-1.0.0.jar` into
   `.minecraft/mods/`.
3. Launch the "fabric-loader-1.21.11" profile.
4. In-game, press **Right Shift** to open the ClickGUI.

---

## Project structure

```
src/main/java/ridzzxmc/xclient/
  XClientMod.java              - client entrypoint, wires Fabric API -> EventBus
  core/
    module/                    - Module base class, ModuleManager, categories
    event/                     - EventBus, @SubscribeEvent, event types
    settings/                  - Boolean/Slider/Mode/Color settings
    config/                    - JSON save/load (ConfigManager)
    keybind/                   - Keybind + KeybindManager
    notification/              - Toast notification queue
  modules/
    hud/                       - Draggable HUD widgets (HudModule base + ~20 widgets)
    render/                    - Cosmetic visual tweaks (Crosshair, Fullbright, ...)
    optimization/              - Entity/chunk culling, dynamic FPS, async chunk build
    movement/                  - Zoom, auto-sprint, safe walk, parkour assist, ...
    world/                     - Weather/fog/sky client-side overrides
    utility/                   - Auto GG, auto text, tool warnings
    misc/                      - Anti-AFK, chat filter, friend/ignore lists
  gui/
    clickgui/                  - ClickGUIScreen (Mod Menu: sidebar + tabs + icon
                                  grid), ModuleMeta (icon/tag lookup), ModuleSettingsPanel
    hud/                       - HudEditorScreen (drag & drop), ToastRenderer
    theme/                     - XTheme, the single red color palette
  mixin/                       - Keyboard/Mouse -> InputEvent bridge; nametag
                                  badge hook exists but is NOT currently
                                  registered (see "Extending the nametag badge")
  util/                        - Easing curves, RenderUtil (rounded rect/shadow/blur/icons)
```

## How the pieces fit together

**EventBus.** Every module extends `Module` and, once enabled, is
`EventBus.INSTANCE.subscribe(this)`-d automatically (see `Module.setEnabled`).
Any method annotated `@SubscribeEvent` taking a single `Event` subclass
parameter gets called whenever that event fires - across the whole class
hierarchy, so a shared base class like `HudModule` can wire up rendering once
for every HUD widget.

```java
public class MyModule extends Module {
    public MyModule() {
        super("My Module", "Does a thing.", ModuleCategory.MISC);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (event.getPhase() != TickEvent.Phase.END) return;
        // runs once per client tick while this module is enabled
    }
}
```

Fabric API's native callbacks (`ClientTickEvents`, `HudRenderCallback`,
`WorldRenderEvents`, `ClientPlayConnectionEvents`) are bridged into our own
`TickEvent` / `RenderEvent` / `PacketEvent` inside `XClientMod` - modules
never touch Fabric API directly, which keeps the module code portable if you
ever want to reuse it against a different loader.

**Settings.** Each module registers its settings once, in its constructor,
via `register(new SliderSetting(...))`. `ModuleSettingsPanel` reads that list
reflectively to draw the right widget per type - add a new setting and it
shows up in the GUI with no other changes needed.

**HUD widgets.** `HudModule` (in `modules/hud`) extends `Module` and adds
`x`, `y`, a `scale` slider, and a private render hook that the base class
wires into `EventBus` automatically. Every concrete widget only implements
`renderContent(...)`, `getWidth()`, `getHeight()`. Positions are dragged in
`HudEditorScreen` (opened from the Mod Menu's HUD tab -> "Edit Layout")
and persisted by `ConfigManager` alongside enabled-state and settings.

**Config.** `ConfigManager.save/load` serializes every module's
enabled-state, keybind, settings, and (for HUD widgets) position to
`.minecraft/config/xclient/config.json`. Saved automatically on disconnect
and on closing the ClickGUI/HUD editor.

## The "Mod Menu" ClickGUI

`ClickGUIScreen` mirrors the reference design: a narrow icon sidebar on the
left (Mod Menu / General / Chat Options / Cosmetic / Graphics / Social),
a red header banner, and - on the Mod Menu page - filter tabs (All / New /
HUD / Hypixel / PvP) plus a search box above a scrollable 3-column card
grid. Each card shows an icon, the module name, a favorite dot, and the
same toggle pill as before.

**Icons.** `ModuleMeta` maps each module *class* (not its display name, to
avoid silent typos) to an icon file under
`assets/xclient/textures/icons/`, plus which filter tabs it should appear
under. A module with no entry there automatically falls back to a colored
"first letter" badge (`RenderUtil.initialBadge`) instead of a missing
texture - so adding a new module never breaks the grid, it just looks
plain until you give it an icon. To add one: drop a PNG under
`textures/icons/`, then add one line to the `static { ... }` block in
`ModuleMeta.java`.

**Sidebar pages other than Mod Menu** are convenience filters, not a second
copy of the module list - General/Cosmetic/Graphics reuse `ModuleCategory`,
while Chat Options/Social list specific module classes directly (see the
`*_MODULES` sets at the top of `ClickGUIScreen`) since those modules span
categories that don't map cleanly to a single sidebar page. Nothing is
hidden, though - the Mod Menu page's "All" tab always shows every module
regardless of whether it's also reachable from another page.

## Adding a new module - checklist

1. Create a class extending `Module` (or `HudModule` for a draggable widget)
   under the matching `modules/<category>` package.
2. Register any settings in the constructor with `register(new ...)`.
3. Override `onEnable()` / `onDisable()` for one-time setup/teardown, and add
   `@SubscribeEvent` methods for anything that needs to run repeatedly.
4. Add one line to `ModuleManager.registerAll()`.

That's it - the ClickGUI, config persistence, and (for HUD widgets) the
layout editor all pick it up automatically.

## Extending the nametag badge

`mixin/MixinEntityRenderer.java` has an injection point aimed at
`EntityRenderer.renderLabelIfPresent`, meant for drawing the small "X" badge
next to player nametags shown in the reference design - **but it is
currently NOT registered** in `xclient.mixins.json`, and its body is empty.

Why: that method's signature changed more than once between 1.21.6 and
1.21.11 (entity renderers now work off an immutable "render state" snapshot
instead of the live entity, and rendering itself moved to a queued-command
model), and the mixin is marked `required`, so a signature mismatch would
hard-crash the whole mod at startup instead of just failing to draw a badge.
Rather than ship a guess, it's left disabled.

To finish it:

1. Confirm the current signature of `EntityRenderer.renderLabelIfPresent`
   for the exact Yarn build you're compiling against (the class-level
   javadoc at `https://maven.fabricmc.net/docs/yarn-<your build>/net/minecraft/client/render/entity/EntityRenderer.html`
   is the fastest way - or just try compiling with a guess and read the
   Mixin error, it names the closest real method it found).
2. Update the `@Inject` method's parameter list in `MixinEntityRenderer.java`
   to match exactly.
3. Add an icon texture under `src/main/resources/assets/xclient/textures/icons/badge.png`.
4. In the mixin's injected method, bind that texture and draw a small quad
   offset from the label's screen position.
5. Gate it behind a new `Module` (e.g. `NametagBadge`) so it's toggleable
   like everything else.
6. Add `"MixinEntityRenderer"` back to the `client` list in
   `xclient.mixins.json`.

## What's deliberately NOT included

No ESP/wallhacks, no aim-assist, no auto-clicker, no reach/hitbox
modification, no auto-armor equip that reacts faster than legitimate play,
no fast-place/fast-break, no anti-knockback ("Velocity"), and no staff/anti-
cheat detection. These are the standard feature set of cheat clients like
Wurst/Impact and give an unfair advantage in PvP or against server rules -
they were intentionally left out of this build.

## Known limitations of this template

- `RenderUtil.roundedRect`/`blurBackdrop` are cheap CPU-side approximations,
  not true shader-based blur/AA - swap in a fragment shader pass if you want
  a sharper look later.
- `ModuleSettingsPanel`'s slider/color widgets are drawn but click/drag
  handling for them isn't wired into `ClickGUIScreen.mouseClicked` yet -
  only the boolean toggle pill and mode text are meant to be clicked as-is;
  extend `mouseClicked`/`mouseDragged` following the `HudEditorScreen`
  pattern to finish slider dragging and a color picker popup.
- `ScoreboardMod`, `NoBossBar`, `NoHurtCam`, `TimeChanger`'s actual rendering
  hooks are left as documented extension points (see the Javadoc in each
  class) rather than full mixins, since the exact injection point depends on
  which Yarn mappings build you land on - the pattern is identical to
  `MixinKeyboard`/`MixinMouse` once you've picked your version.
