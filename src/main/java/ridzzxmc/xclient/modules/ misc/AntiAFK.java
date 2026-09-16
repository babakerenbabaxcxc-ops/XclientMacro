package ridzzxmc.xclient.modules.misc;

import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.TickEvent;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.SliderSetting;

/**
 * Sends a tiny look/jump input every N seconds to reset a server's AFK
 * timer while you step away - identical in effect to the player nudging
 * their mouse, no auto-farming, no combat automation.
 */
public class AntiAFK extends Module {

    private final SliderSetting intervalSeconds = register(new SliderSetting("Interval (s)", 45, 10, 300, 5, true));
    private long lastActionTick;

    public AntiAFK() {
        super("Anti AFK", "Nudges input periodically so you aren't kicked for being idle.", ModuleCategory.MISC);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || event.getPhase() != TickEvent.Phase.END) return;

        long now = System.currentTimeMillis();
        if (now - lastActionTick < intervalSeconds.getValue() * 1000) return;
        lastActionTick = now;

        mc.player.setYaw(mc.player.getYaw() + 1f);
        mc.player.setYaw(mc.player.getYaw() - 1f);
    }
}
