package ridzzxmc.xclient.modules.render;

import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;

/**
 * Flag consumed by a mixin into {@code GameRenderer.bobViewWhenHurt} to skip
 * the hurt-camera tilt. Cosmetic comfort setting, comparable to vanilla's
 * "reduced screen effects" accessibility option.
 */
public class NoHurtCam extends Module {

    public NoHurtCam() {
        super("No Hurt Cam", "Disables the camera-shake effect when taking damage.", ModuleCategory.RENDER);
    }
}
