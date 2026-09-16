package ridzzxmc.xclient.modules.misc;

import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.TickEvent;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;

/** Automatically respawns you on death, same as clicking the "Respawn" button yourself. */
public class AutoRespawn extends Module {

    public AutoRespawn() {
        super("Auto Respawn", "Automatically clicks Respawn when you die.", ModuleCategory.MISC, true);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (event.getPhase() != TickEvent.Phase.END) return;
        if (mc.player != null && mc.player.isDead()) {
            mc.player.requestRespawn();
        }
    }
}
