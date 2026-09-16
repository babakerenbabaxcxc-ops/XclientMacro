package ridzzxmc.xclient.modules.world;

import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.SliderSetting;

/**
 * Reduces the client-rendered underwater fog density/tint - a graphics
 * option, not a vision hack: it doesn't reveal anything beyond normal
 * render distance, just makes nearby water less murky to look at.
 */
public class ClearWater extends Module {

    private final SliderSetting fogReduction = register(new SliderSetting("Fog Reduction", 0.7, 0, 1, 0.05));

    public ClearWater() {
        super("Clear Water", "Reduces underwater fog density for a clearer view.", ModuleCategory.WORLD);
    }

    public double getFogReduction() {
        return fogReduction.getValue();
    }
}
