package ridzzxmc.xclient.modules.movement;

import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.TickEvent;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;

/**
 * Sets {@code player.setSprinting(true)} whenever moving forward with enough
 * hunger, exactly what double-tapping W does manually - saves the double-tap,
 * doesn't change sprint speed, hunger cost, or attack-reset timing.
 */
public class AutoSprint extends Module {

    public AutoSprint() {
        super("Auto Sprint", "Automatically sprints while moving forward, like double-tapping W.",
                ModuleCategory.MOVEMENT);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || event.getPhase() != TickEvent.Phase.END) return;
        if (mc.player.forwardSpeed > 0 && mc.player.getHungerManager().getFoodLevel() > 6 && !mc.player.isSneaking()) {
            mc.player.setSprinting(true);
        }
    }
}
