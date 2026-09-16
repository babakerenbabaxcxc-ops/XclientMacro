package ridzzxmc.xclient.modules.optimization;

import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.SliderSetting;

/**
 * Caps how many non-player entities get a render pass per frame when the
 * count spikes (e.g. mob farms, item cannons), rendering only the closest N
 * to the camera. Entities beyond the cap are simply skipped that frame -
 * next frame the ordering is recomputed so nothing is permanently hidden.
 */
public class EntityLimiter extends Module {

    private final SliderSetting maxEntities = register(new SliderSetting("Max Rendered", 200, 20, 1000, 10, true));

    public EntityLimiter() {
        super("Entity Limiter", "Caps rendered entity count per frame during entity spikes.",
                ModuleCategory.OPTIMIZATION);
    }

    public int getMaxEntities() {
        return maxEntities.getValue().intValue();
    }
}
