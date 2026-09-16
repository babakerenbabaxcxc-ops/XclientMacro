package ridzzxmc.xclient.modules.optimization;

import net.minecraft.client.option.ParticlesMode;
import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.RenderEvent;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.ModeSetting;
import ridzzxmc.xclient.core.settings.SliderSetting;

import java.util.List;

/**
 * Two independent levers on particle load: a hard count cap (dropping the
 * oldest/least-visible particles first once hit) plus a "Detail" mode that
 * drives vanilla's own Particles video option (All/Decreased/Minimal) - the
 * same category of particle vanilla itself already hides at Decreased/Minimal
 * (weather, most block-break dust, etc), so this is the safe, real lever for
 * "which particles are allowed to spawn at all" rather than a per-effect
 * allow-list, which would need hooking the particle-spawn pipeline directly
 * and has no stable, mapping-independent extension point to do that from.
 */
public class ParticleLimiter extends Module {

    private final SliderSetting maxParticles = register(new SliderSetting("Max Particles", 500, 50, 4000, 50, true));
    private final ModeSetting detail = register(new ModeSetting("Detail",
            List.of("All", "Decreased", "Minimal"), "All"));

    private ParticlesMode previous;

    public ParticleLimiter() {
        super("Particle Limiter", "Caps the number of simultaneously rendered particles.",
                ModuleCategory.OPTIMIZATION);
    }

    @Override
    protected void onEnable() {
        previous = mc.options.getParticles().getValue();
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
        ParticlesMode target = switch (detail.getValue()) {
            case "Decreased" -> ParticlesMode.DECREASED;
            case "Minimal" -> ParticlesMode.MINIMAL;
            default -> ParticlesMode.ALL;
        };
        if (mc.options.getParticles().getValue() != target) {
            mc.options.getParticles().setValue(target);
        }
    }

    public int getMaxParticles() {
        return maxParticles.getValue().intValue();
    }
}
