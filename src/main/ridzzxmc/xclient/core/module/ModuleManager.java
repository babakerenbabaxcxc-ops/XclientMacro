package ridzzxmc.xclient.core.module;

import ridzzxmc.xclient.modules.cosmetic.*;
import ridzzxmc.xclient.modules.hud.*;
import ridzzxmc.xclient.modules.misc.*;
import ridzzxmc.xclient.modules.movement.*;
import ridzzxmc.xclient.modules.optimization.*;
import ridzzxmc.xclient.modules.render.*;
import ridzzxmc.xclient.modules.utility.*;
import ridzzxmc.xclient.modules.world.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central registry for every {@link Module}. Register new modules here -
 * the ClickGUI and HUD editor both read from this list, nothing else needs
 * to be touched to add a feature to the UI.
 */
public class ModuleManager {

    private final List<Module> modules = new ArrayList<>();

    public void registerAll() {
        // ---- HUD ----
        register(new ArmorHUD());
        register(new ClockHUD());
        register(new CoordinatesModule());
        register(new CPSCounter());
        register(new FPSModule());
        register(new Keystrokes());
        register(new PingDisplay());
        register(new PotionHUD());
        register(new SpeedHUD());
        register(new WatermarkHUD());
        register(new SessionInfoHUD());
        register(new BiomeHUD());
        register(new DayCounter());
        register(new DirectionHUD());
        register(new MemoryHUD());
        register(new PackDisplay());
        register(new ServerInfoHUD());
        register(new ChatTimestamps());
        register(new SprintModule());

        // ---- Render / Visual (also see the explicit "Render" ClickGUI tab list
        // in ClickGUIScreen.RENDER_TAB_MODULES, which must be kept in sync with
        // BlockOverlayModule / Crosshair / DamageIndicator / FullbrightModule /
        // HitboxModule / NoBossBar / NoHurtCam / Particles / PingOverlay /
        // ScoreboardMod / TimeChanger below) ----
        register(new BlockOverlayModule());
        register(new Crosshair());
        register(new DamageIndicator());
        register(new FullbrightModule());
        register(new HitboxModule());
        register(new KillEffect());
        register(new NoBossBar());
        register(new NoHurtCam());
        register(new Particles());
        register(new PingOverlay());
        register(new ScoreboardMod());
        register(new TimeChanger());
        register(new XClientIcon());

        // ---- Cosmetic ----
        register(new XClientCape());

        // ---- Optimization ----
        register(new EntityCulling());
        register(new ChunkCuller());
        register(new EntityLimiter());
        register(new ParticleLimiter());
        register(new SmartAnimations());
        register(new AsyncChunkBuilder());
        register(new LazyChunkLoading());
        register(new OcclusionCulling());

        // ---- Movement ----
        register(new ZoomModule());
        register(new AutoSprint());

        // ---- World ----
        register(new NoWeather());
        register(new ClearWater());

        // ---- Utility ----
        register(new AutoGG());
        register(new ToolWarning());
        register(new AutoText());

        // ---- Misc ----
        register(new AntiAFK());
        register(new AutoRespawn());
        register(new MessageLogger());
        register(new Waypoints());
        register(new AutoReconnect());
        register(new AutoLogin());
        register(new ClipboardCoordinates());

        modules.forEach(Module::init);
    }

    private void register(Module module) {
        modules.add(module);
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getByCategory(ModuleCategory category) {
        return modules.stream().filter(m -> m.getCategory() == category).collect(Collectors.toList());
    }

    public Optional<Module> getByName(String name) {
        return modules.stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst();
    }

    /** Typed lookup for code outside the GUI (e.g. a mixin) that needs a specific module's state. */
    @SuppressWarnings("unchecked")
    public <T extends Module> Optional<T> getModule(Class<T> clazz) {
        for (Module module : modules) {
            if (clazz.isInstance(module)) {
                return Optional.of((T) module);
            }
        }
        return Optional.empty();
    }
}
