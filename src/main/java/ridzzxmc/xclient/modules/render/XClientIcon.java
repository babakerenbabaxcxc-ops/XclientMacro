package ridzzxmc.xclient.modules.render;

import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;

/**
 * Toggle for the small X Client badge drawn next to your own nametag (the
 * actual rendering happens in {@code MixinPlayerEntityRenderer} /
 * {@code MixinLivingEntityRenderer} - this class only holds the on/off
 * state so it shows up as a normal module in the Mod Menu).
 * <p>
 * Purely cosmetic: it only changes how your own name renders locally, and
 * never touches, hides, or fakes any other player's name or identity.
 */
public class XClientIcon extends Module {

    public XClientIcon() {
        super("X Client Icon", "Shows a small X Client badge next to your own nametag.", ModuleCategory.RENDER, true);
    }
}
