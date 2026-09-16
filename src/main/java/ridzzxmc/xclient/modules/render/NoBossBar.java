package ridzzxmc.xclient.modules.render;

import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;

/**
 * Client-side visibility flag read by a mixin/accessor into
 * {@code InGameHud.renderStatusBars} (BossBarHud draw call) - purely
 * cosmetic decluttering, has no effect on the actual boss fight or its data.
 */
public class NoBossBar extends Module {

    public NoBossBar() {
        super("No Boss Bar", "Hides the boss health bar overlay.", ModuleCategory.RENDER);
    }
}
