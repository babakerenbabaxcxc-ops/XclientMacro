package ridzzxmc.xclient.modules.render;

import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.RenderEvent;
import ridzzxmc.xclient.core.event.events.TickEvent;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.SliderSetting;
import ridzzxmc.xclient.util.OptionAccess;

/**
 * Raises brightness to (well past) maximum while enabled and restores the
 * user's previous value on disable. Doesn't touch lighting calculation,
 * chunk data, or give x-ray-style info - it's still just the gamma option,
 * pushed further than the options screen's own slider allows.
 * <p>
 * A previous version of this module clamped to 1.0 (vanilla's own maximum
 * "Bright" setting) because {@code SimpleOption#setValue} silently rejects
 * anything outside [0.0, 1.0] for gamma - which compiled fine but had no
 * real effect: even at 1.0, unlit blocks (caves, night, etc) still render
 * visibly dim, because the vanilla blend formula only interpolates toward
 * "bright" rather than being overridden by it. A genuine fullbright effect
 * needs a gamma value far past that clamp (in the same range OptiFine and
 * most other fullbright mods use), which {@link OptionAccess#setBestEffort}
 * provides by writing past the option's own validator - and safely falls
 * back to the old clamped-to-1.0 behavior if that's ever not possible.
 */
public class FullbrightModule extends Module {

    private final SliderSetting strength = register(new SliderSetting("Strength", 10, 2, 20, 1, true));

    private double previousGamma;

    public FullbrightModule() {
        super("Fullbright", "Raises brightness to maximum while enabled, even in caves and at night.", ModuleCategory.RENDER);
    }

    @Override
    protected void onEnable() {
        previousGamma = mc.options.getGamma().getValue();
        OptionAccess.setBestEffort(mc.options.getGamma(), strength.getValue());
    }

    @Override
    protected void onDisable() {
        OptionAccess.setBestEffort(mc.options.getGamma(), previousGamma);
    }

    // Reasserted every frame (not just once on enable) so that (a) dragging the
    // Strength slider while Fullbright is already on takes effect immediately,
    // and (b) anything else that touches gamma while enabled - the user opening
    // the vanilla video settings, another mod, etc - gets overridden back,
    // matching how a real "always on" fullbright effect is expected to behave.
    @SubscribeEvent
    private void xclient$onRender(RenderEvent event) {
        if (event.getType() != RenderEvent.Type.HUD_2D) return;
        OptionAccess.setBestEffort(mc.options.getGamma(), strength.getValue());
    }

    // Also reasserted once per tick (in addition to the render-phase call
    // above): HUD_2D fires during the 2D overlay pass, which happens AFTER
    // that frame's 3D world/lighting has already been drawn, so a value set
    // only there always trails the world render by one frame. Ticking is
    // decoupled from render order entirely, so this keeps gamma correct
    // going into the NEXT world render regardless of exactly when in the
    // frame that happens.
    @SubscribeEvent
    private void xclient$onTick(TickEvent event) {
        if (event.getPhase() != TickEvent.Phase.END) return;
        OptionAccess.setBestEffort(mc.options.getGamma(), strength.getValue());
    }
}
