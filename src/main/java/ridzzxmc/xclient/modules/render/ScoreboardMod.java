package ridzzxmc.xclient.modules.render;

import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.BooleanSetting;

/**
 * Flag + settings consumed by a mixin into {@code InGameHud.renderScoreboardSidebar}
 * to redraw the sidebar with the client's card/shadow style instead of vanilla's
 * flat gray box. Content (lines, scores) comes straight from the server - unchanged.
 */
public class ScoreboardMod extends Module {

    private final BooleanSetting compact = register(new BooleanSetting("Compact Rows", false));

    public ScoreboardMod() {
        super("Scoreboard", "Restyles the scoreboard sidebar to match the client theme.", ModuleCategory.RENDER, true);
    }

    public boolean isCompact() {
        return compact.getValue();
    }
}
