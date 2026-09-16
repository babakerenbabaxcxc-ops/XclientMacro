package ridzzxmc.xclient.modules.utility;

import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.ModeSetting;

import java.util.List;

/**
 * Sends a friendly chat message (configurable text) when a "victory"/"game
 * over"-style title is detected, e.g. Bedwars/end-of-match screens. Purely
 * a convenience macro for a single sportsmanship message, not spam.
 */
public class AutoGG extends Module {

    private final ModeSetting message = register(new ModeSetting("Message",
            List.of("gg", "gg wp", "good game!"), "gg"));

    public AutoGG() {
        super("Auto GG", "Sends a sportsmanship message when a match ends.", ModuleCategory.UTILITY);
    }

    public String getMessage() {
        return message.getValue();
    }
}
