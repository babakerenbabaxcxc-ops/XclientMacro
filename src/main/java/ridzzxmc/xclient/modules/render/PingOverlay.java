package ridzzxmc.xclient.modules.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.RenderEvent;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.ColorSetting;

/**
 * Small, fixed "123ms" label above the crosshair - reads the same latency
 * value already shown next to your name in the vanilla player list (tab
 * menu), via {@link PlayerListEntry#getLatency()}. Distinct from the
 * draggable {@code Ping} HUD widget ({@code PingDisplay}): this one is a
 * minimal always-in-view indicator meant for the Render tab rather than a
 * repositionable corner HUD element.
 */
public class PingOverlay extends Module {

    private final ColorSetting color = register(new ColorSetting("Color", 0xFFF5F0F0));

    public PingOverlay() {
        super("Ping Overlay", "Shows your current ping above the crosshair.", ModuleCategory.RENDER);
    }

    @SubscribeEvent
    private void xclient$onRender(RenderEvent event) {
        if (event.getType() != RenderEvent.Type.HUD_2D || mc.options.hudHidden) return;
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        if (entry == null) return;

        String label = entry.getLatency() + "ms";
        DrawContext ctx = event.getDrawContext();
        var text = mc.textRenderer;
        int cx = mc.getWindow().getScaledWidth() / 2;
        int cy = mc.getWindow().getScaledHeight() / 2;
        int w = text.getWidth(label);

        ctx.drawText(text, label, cx - w / 2, cy + 14, color.getValue(), true);
    }
}
