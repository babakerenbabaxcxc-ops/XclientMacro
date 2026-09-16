package ridzzxmc.xclient.modules.render;

import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.ModeSetting;

import java.util.List;

/**
 * Overrides only the *rendered* sky angle on the client (via a mixin into
 * {@code ClientWorld.getSkyAngle}) - the server's actual world time, day
 * counter and mob-spawn conditions are untouched, so this can't be used to
 * see-through-night or manipulate spawn mechanics.
 */
public class TimeChanger extends Module {

    private final ModeSetting mode = register(new ModeSetting("Mode",
            List.of("Day", "Noon", "Sunset", "Night"), "Day"));

    public TimeChanger() {
        super("Time Changer", "Locally renders a fixed time of day for visual comfort.", ModuleCategory.RENDER);
    }

    public float getSkyAngleOverride() {
        return switch (mode.getValue()) {
            case "Day" -> 0.0f;
            case "Noon" -> 0.25f;
            case "Sunset" -> 0.5f;
            case "Night" -> 0.75f;
            default -> 0.0f;
        };
    }
}
