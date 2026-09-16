package ridzzxmc.xclient.modules.misc;

import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.ConnectionEvent;
import ridzzxmc.xclient.core.event.events.TickEvent;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.notification.NotificationManager;
import ridzzxmc.xclient.core.notification.NotificationType;
import ridzzxmc.xclient.core.settings.SliderSetting;

/**
 * Reconnects to whichever server you last joined a few seconds after you get
 * disconnected from it - handy for restarts/crashes on a server you're
 * grinding on.
 * <p>
 * Off by default and worth calling out explicitly: {@code DISCONNECT} fires
 * on <b>any</b> disconnection, including you quitting to the title screen on
 * purpose. Enabling this means every manual quit is also followed by a
 * reconnect attempt a few seconds later, unless you leave through singleplayer
 * or close the game outright in that window.
 * <p>
 * Uses the same {@code ConnectScreen.connect(Screen, MinecraftClient,
 * ServerAddress, ServerInfo, boolean, CookieStorage)} entry point the vanilla
 * multiplayer screen itself calls. That signature (and the {@code ServerInfo}
 * constructor below) is Yarn-mapping/version sensitive - it's held steady
 * across recent 1.21.x releases, but if a future mappings bump renames or
 * reshuffles either one, this is the call site to fix.
 */
public class AutoReconnect extends Module {

    private final SliderSetting delaySeconds = register(new SliderSetting("Delay (s)", 5, 1, 30, 1, true));

    private String pendingAddress;
    private int ticksRemaining = -1;

    public AutoReconnect() {
        super("Auto Reconnect", "Reconnects to the last server a few seconds after a disconnect.", ModuleCategory.MISC);
    }

    @SubscribeEvent
    public void onConnectionEvent(ConnectionEvent event) {
        if (!isEnabled()) return;

        if (event.getPhase() == ConnectionEvent.Phase.JOIN) {
            // Successfully back in - cancel any reconnect attempt still counting down
            // from a previous drop (e.g. it connected on its own before we retried).
            ticksRemaining = -1;
            return;
        }

        if (event.getServerAddress() == null) return; // singleplayer, or no address ever recorded

        pendingAddress = event.getServerAddress();
        ticksRemaining = delaySeconds.getValue().intValue() * 20;
        NotificationManager.INSTANCE.push("Auto Reconnect",
                "Reconnecting to " + pendingAddress + " in " + delaySeconds.getValue().intValue() + "s...",
                NotificationType.INFO);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (event.getPhase() != TickEvent.Phase.END || ticksRemaining < 0) return;

        // Already back in a world (this reconnect or a manual one) - stop counting.
        if (mc.world != null) {
            ticksRemaining = -1;
            return;
        }

        if (--ticksRemaining > 0) return;
        ticksRemaining = -1;

        try {
            ServerAddress address = ServerAddress.parse(pendingAddress);
            ServerInfo info = new ServerInfo(pendingAddress, pendingAddress, ServerInfo.ServerType.OTHER);
            ConnectScreen.connect(mc.currentScreen, mc, address, info, false, null);
        } catch (Exception e) {
            System.err.println("[X Client] Auto Reconnect failed to reconnect to " + pendingAddress + ":");
            e.printStackTrace();
            NotificationManager.INSTANCE.push("Auto Reconnect", "Failed to reconnect.", NotificationType.ERROR);
        }
    }
}
