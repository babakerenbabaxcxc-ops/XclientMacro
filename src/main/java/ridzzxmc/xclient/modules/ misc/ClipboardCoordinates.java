package ridzzxmc.xclient.modules.misc;

import org.lwjgl.glfw.GLFW;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.notification.NotificationManager;
import ridzzxmc.xclient.core.notification.NotificationType;
import ridzzxmc.xclient.core.settings.BooleanSetting;

/**
 * A "button" module rather than a toggle: click it (or hit its bound key)
 * and it copies your current coordinates to the system clipboard, then
 * immediately flips itself back off. Same one-shot-via-{@code onEnable()}
 * trick as a keybind-triggered action would use anywhere else in this
 * codebase - there's no separate "action" concept in {@link Module}, so
 * toggling straight back off in {@code onEnable()} is what makes this read
 * as a button instead of a switch in the ClickGUI.
 */
public class ClipboardCoordinates extends Module {

    private final BooleanSetting includeDimension = register(new BooleanSetting("Include Dimension", true));

    public ClipboardCoordinates() {
        super("Clipboard Coordinates", "Copies your current position to the clipboard. Acts as a button, not a toggle.", ModuleCategory.MISC);
    }

    @Override
    protected void onEnable() {
        try {
            if (mc.player == null) {
                NotificationManager.INSTANCE.push("Clipboard Coordinates", "Not in a world.", NotificationType.WARNING);
                return;
            }

            String coords = String.format("%.1f, %.1f, %.1f",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ());

            if (includeDimension.getValue() && mc.world != null) {
                coords += " (" + mc.world.getRegistryKey().getValue() + ")";
            }

            GLFW.glfwSetClipboardString(mc.getWindow().getHandle(), coords);
            NotificationManager.INSTANCE.push("Clipboard Coordinates", "Copied: " + coords, NotificationType.SUCCESS);
        } finally {
            // Always bounce back to "off", even if copying failed above - see class javadoc.
            setEnabled(false);
        }
    }
}
