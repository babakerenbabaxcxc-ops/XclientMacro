package ridzzxmc.xclient.modules.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import ridzzxmc.xclient.gui.theme.XTheme;

public class PingDisplay extends HudModule {

    public PingDisplay() {
        super("Ping", "Displays your connection latency to the server.", 6, 190);
    }

    @Override
    protected void renderContent(DrawContext ctx, int screenWidth, int screenHeight) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        int ping = entry != null ? entry.getLatency() : 0;

        String text = ping + " ms";
        int color = ping <= 80 ? XTheme.SUCCESS : ping <= 180 ? XTheme.WARNING : XTheme.ERROR;

        int width = mc.textRenderer.getWidth(text) + 12;
        ctx.fill(0, 0, width, 14, XTheme.PANEL_BG);
        ctx.fill(0, 0, 3, 14, color);
        ctx.drawText(mc.textRenderer, text, 6, 3, XTheme.TEXT_PRIMARY, true);
    }

    @Override
    public int getWidth() {
        return 70;
    }

    @Override
    public int getHeight() {
        return 14;
    }
}
