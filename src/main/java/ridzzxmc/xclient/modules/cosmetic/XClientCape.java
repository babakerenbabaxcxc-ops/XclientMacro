package ridzzxmc.xclient.modules.cosmetic;

import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;

/**
 * "Equipped" flag for the X Client cosmetic cape. The Cosmetic ClickGUI page
 * ({@link ridzzxmc.xclient.gui.clickgui.CosmeticPanel}) is the only place this is
 * currently read - toggling it there flips this module on/off exactly like
 * any other module (so it saves/loads with the rest of the config), and the
 * panel re-renders its preview (cape art + your own skin) accordingly.
 * <p>
 * Scope note: this drives the in-menu preview only for now. Actually drawing
 * the cape on your back in the 3D world (for yourself and other players)
 * needs a further mixin into {@code PlayerEntityRenderer}'s body-model pass
 * that positions a textured quad using the entity's own body yaw - the badge
 * mixin already in {@code MixinPlayerEntityRenderer} proves the low-level
 * "draw a textured quad on the player" technique compiles and works on this
 * exact Minecraft build, but it billboards to the camera (fine for a flat
 * nametag icon, wrong for a cape that must instead rotate with the player's
 * back). That needs the render-state's body-yaw field name confirmed against
 * a real build first, so it's deliberately left for a follow-up rather than
 * shipping a cape that visibly doesn't rotate with the player.
 */
public class XClientCape extends Module {

    public XClientCape() {
        super("X Client Cape", "Equip the X Client cosmetic cape. Preview it on the Cosmetic page.",
                ModuleCategory.COSMETIC);
    }
}
