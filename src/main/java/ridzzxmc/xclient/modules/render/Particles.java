package ridzzxmc.xclient.modules.render;

import net.minecraft.client.option.ParticlesMode;
import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.RenderEvent;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.ModeSetting;

import java.util.List;

/**
 * Forces vanilla's own "Particles" video option (All / Decreased / Minimal)
 * to a chosen level while enabled, and restores whatever it was set to
 * beforehand on disable - the same safe "drive a vanilla {@code SimpleOption}"
 * technique {@link FullbrightModule} and {@link ridzzxmc.xclient.modules.movement.ZoomModule}
 * already use, so this never depends on hooking the particle spawn pipeline
 * itself. A declutter tool for busy PvP fights, not a performance cap (see
 * {@link ridzzxmc.xclient.modules.optimization.ParticleLimiter} for that).
 */
public class Particles extends Module {

    private final ModeSetting mode = register(new ModeSetting("Mode",
            List.of("All", "Decreased", "Minimal"), "Minimal"));

    private ParticlesMode previous;

    public Particles() {
        super("Particles", "Forces a lighter particle detail level for a cleaner view.", ModuleCategory.RENDER);
    }

    @Override
    protected void onEnable() {
        previous = mc.options.getParticles().getValue();
        apply();
    }

    @Override
    protected void onDisable() {
        if (previous != null) {
            mc.options.getParticles().setValue(previous);
        }
    }

    @SubscribeEvent
    private void xclient$onRender(RenderEvent event) {
        if (event.getType() != RenderEvent.Type.HUD_2D) return;
        apply();
    }

    private void apply() {
        ParticlesMode target = switch (mode.getValue()) {
            case "All" -> ParticlesMode.ALL;
            case "Decreased" -> ParticlesMode.DECREASED;
            default -> ParticlesMode.MINIMAL;
        };
        if (mc.options.getParticles().getValue() != target) {
            mc.options.getParticles().setValue(target);
        }
    }
}
