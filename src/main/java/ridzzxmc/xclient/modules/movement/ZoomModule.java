package ridzzxmc.xclient.modules.movement;

import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.RenderEvent;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.SliderSetting;
import ridzzxmc.xclient.util.OptionAccess;
import org.lwjgl.glfw.GLFW;

/**
 * Classic "OptiFine-style" zoom: press the bound key (C by default) to toggle
 * zoom, FOV is smoothly reduced toward {@link #zoomFov} while zoomed and
 * instantly restored on release. No effect on hitboxes, reach, or aiming
 * precision - purely a camera FOV change identical to manually adjusting the
 * FOV slider (just able to go further than that slider's own vanilla range).
 * <p>
 * "Zoomed" and "enabled" are the same state here (unlike most modules): the
 * bound key toggles the module on/off directly through the shared
 * {@link ridzzxmc.xclient.core.keybind.KeybindManager}, and being enabled IS being
 * zoomed in. That reuses the exact same press-to-toggle plumbing every other
 * module already relies on, instead of adding a second, module-specific key
 * listener that could fall out of sync with it (and that would still have had
 * to fight the generic listener over the same key).
 */
public class ZoomModule extends Module {

    /** How quickly FOV eases toward the target each frame - higher = snappier, lower = smoother. */
    private static final double LERP_FACTOR = 0.35;

    private final SliderSetting zoomFov = register(new SliderSetting("Zoom FOV", 15, 5, 50, 1, true));
    private double previousFov;

    public ZoomModule() {
        super("Zoom", "Press the keybind to smoothly zoom in, like a spyglass. Bound to C by default.",
                ModuleCategory.MOVEMENT, false);
        getKeybind().setKey(GLFW.GLFW_KEY_C);
    }

    @Override
    protected void onEnable() {
        previousFov = mc.options.getFov().getValue();
    }

    @Override
    protected void onDisable() {
        // Snap back instantly rather than lerping - onDisable() runs once,
        // synchronously, right before this module unsubscribes from the event
        // bus, so there's no further frame left in which a smooth lerp-out
        // could continue to run.
        OptionAccess.setBestEffort(mc.options.getFov(), previousFov);
    }

    @SubscribeEvent
    private void xclient$onRender(RenderEvent event) {
        if (event.getType() != RenderEvent.Type.HUD_2D) return;

        double current = mc.options.getFov().getValue();
        double target = zoomFov.getValue();
        double next = current + (target - current) * LERP_FACTOR;
        if (Math.abs(next - target) < 0.05) next = target;

        if (Math.round(next) != Math.round(current)) {
            OptionAccess.setBestEffort(mc.options.getFov(), next);
        }
    }

    public double getZoomFov() {
        return zoomFov.getValue();
    }
}
