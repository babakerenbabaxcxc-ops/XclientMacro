package ridzzxmc.xclient.modules.hud;

import net.minecraft.client.gui.DrawContext;
import ridzzxmc.xclient.gui.theme.XTheme;

public class ServerInfoHUD extends HudModule {

    public ServerInfoHUD() {
        super("Server Info", "Shows the current server address and version.", 6, 390);
    }

    @Override
    protected void renderContent(DrawContext ctx, int screenWidth, int screenHeight) {
        if (mc.getCurrentServerEntry() == null) return;

        String address = mc.getCurrentServerEntry().address;
        String text = address;

        int width = mc.textRenderer.getWidth(text) + 12;
        ctx.fill(0, 0, width, 14, XTheme.PANEL_BG);
        ctx.fill(0, 0, 3, 14, XTheme.ACCENT);
        ctx.drawText(mc.textRenderer, text, 6, 3, XTheme.TEXT_PRIMARY, true);
    }

    @Override
    public int getWidth() {
        return 220;
    }

    @Override
    public int getHeight() {
        return 14;
    }
}
