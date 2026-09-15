package ridzzxmc.xclient.core.module;

import net.minecraft.client.MinecraftClient;
import ridzzxmc.xclient.core.event.EventBus;
import ridzzxmc.xclient.core.keybind.Keybind;
import ridzzxmc.xclient.core.settings.Setting;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class every feature (HUD element, render tweak, optimization pass...)
 * extends. Handles enable/disable lifecycle, event (un)subscription, keybind
 * and the setting list the ClickGUI renders automatically.
 */
public abstract class Module {

    protected final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String description;
    private final ModuleCategory category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final Keybind keybind = new Keybind();

    private boolean enabled;
    private boolean animating;
    private long lastToggleTime;

    protected Module(String name, String description, ModuleCategory category) {
        this(name, description, category, false);
    }

    protected Module(String name, String description, ModuleCategory category, boolean enabledByDefault) {
        this.name = name;
        this.description = description;
        this.category = category;
        if (enabledByDefault) {
            this.enabled = true;
        }
    }

    /** Called once at startup, after construction - safe place to build the setting list. */
    public void init() {
        // enabledByDefault modules were marked `enabled = true` directly in the
        // constructor above, rather than through setEnabled() - EventBus
        // subscription only happens inside setEnabled(), and calling the
        // overridable onEnable() mid-construction (before a subclass's own
        // fields are initialized) would be its own hazard. So default-enabled
        // modules were never actually subscribed and silently did nothing
        // despite showing as "on". Finishing that here, once construction of
        // every module is complete, fixes it safely.
        if (enabled) {
            EventBus.INSTANCE.subscribe(this);
            try {
                onEnable();
            } catch (Exception e) {
                System.err.println("[X Client] Module '" + name + "' failed to enable on startup:");
                e.printStackTrace();
                this.enabled = false;
                EventBus.INSTANCE.unsubscribe(this);
            }
        }
    }

    public final void toggle() {
        setEnabled(!enabled);
    }

    public final void setEnabled(boolean state) {
        if (this.enabled == state) return;
        this.enabled = state;
        this.lastToggleTime = System.currentTimeMillis();
        this.animating = true;

        try {
            if (state) {
                EventBus.INSTANCE.subscribe(this);
                onEnable();
            } else {
                onDisable();
                EventBus.INSTANCE.unsubscribe(this);
            }
        } catch (Exception e) {
            // A module's onEnable()/onDisable() can be called directly from GUI
            // click handlers, outside the EventBus's own try/catch - an
            // uncaught exception here would crash the whole game. Log it and
            // fall back to a safe disabled state instead.
            System.err.println("[X Client] Module '" + name + "' failed to " + (state ? "enable" : "disable") + ":");
            e.printStackTrace();
            this.enabled = false;
            EventBus.INSTANCE.unsubscribe(this);
        }
    }

    protected void onEnable() {}

    protected void onDisable() {}

    public boolean isEnabled() {
        return enabled;
    }

    /** 0..1 animation progress since last toggle, used for the ClickGUI card fade/slide. Eased in the GUI layer. */
    public float getAnimationProgress(int durationMs) {
        long elapsed = System.currentTimeMillis() - lastToggleTime;
        float t = Math.min(1f, elapsed / (float) durationMs);
        if (t >= 1f) animating = false;
        return t;
    }

    public boolean isAnimating() {
        return animating;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public Keybind getKeybind() {
        return keybind;
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }

    protected <T extends Setting<?>> T register(T setting) {
        settings.add(setting);
        return setting;
    }
}
