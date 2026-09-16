package ridzzxmc.xclient.modules.hud;

import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.BooleanSetting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Prefixes each incoming chat line with a local timestamp - implemented as
 * a non-HUD module (it edits the chat HUD's stored text via mixin) rather
 * than a draggable widget, since it's not a standalone overlay.
 */
public class ChatTimestamps extends Module {

    private final BooleanSetting militaryTime = register(new BooleanSetting("24-Hour", false));

    public ChatTimestamps() {
        super("Chat Timestamps", "Prefixes chat messages with the time they were received.", ModuleCategory.HUD);
    }

    public String formatTimestamp() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(militaryTime.getValue() ? "[HH:mm] " : "[hh:mm a] ");
        return LocalTime.now().format(fmt);
    }
}
