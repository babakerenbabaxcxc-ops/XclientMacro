package ridzzxmc.xclient.modules.utility;

import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.notification.NotificationManager;
import ridzzxmc.xclient.core.notification.NotificationType;
import ridzzxmc.xclient.core.settings.SliderSetting;
import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.TickEvent;

public class ToolWarning extends Module {

    private final SliderSetting durabilityThreshold = register(new SliderSetting("Warn At (uses left)", 20, 1, 100, 1, true));
    private boolean warned;

    public ToolWarning() {
        super("Tool Warning", "Toasts a warning before your held item breaks.", ModuleCategory.UTILITY, true);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || event.getPhase() != TickEvent.Phase.END) return;
        var stack = mc.player.getMainHandStack();
        if (stack.isEmpty() || !stack.isDamageable()) {
            warned = false;
            return;
        }

        int remaining = stack.getMaxDamage() - stack.getDamage();
        if (remaining <= durabilityThreshold.getValue() && !warned) {
            warned = true;
            NotificationManager.INSTANCE.push("Tool Warning",
                    stack.getName().getString() + " is about to break!", NotificationType.WARNING);
        } else if (remaining > durabilityThreshold.getValue()) {
            warned = false;
        }
    }
}
