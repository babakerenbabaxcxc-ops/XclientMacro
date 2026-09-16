package ridzzxmc.xclient.modules.optimization;

import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.BooleanSetting;

/**
 * Enables occlusion-based chunk section culling: sections whose 6 faces are
 * all covered by opaque neighbor sections skip the mesh-build queue entirely
 * until a neighbor changes. This only defers *rendering*, never world data -
 * block updates, redstone, and chunk loading (the actual "world seed/output")
 * are computed identically with the module on or off.
 */
public class ChunkCuller extends Module {

    private final BooleanSetting aggressiveMode = register(new BooleanSetting("Aggressive Mode", false));

    public ChunkCuller() {
        super("Chunk Culler", "Skips mesh building for chunk sections fully hidden behind terrain.",
                ModuleCategory.OPTIMIZATION, true);
    }

    public boolean isAggressive() {
        return aggressiveMode.getValue();
    }
}
