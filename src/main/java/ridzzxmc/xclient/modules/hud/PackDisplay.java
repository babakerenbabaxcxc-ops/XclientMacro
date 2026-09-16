package ridzzxmc.xclient.modules.hud;

import net.minecraft.client.gui.DrawContext;
import ridzzxmc.xclient.gui.theme.XTheme;

public class PackDisplay extends HudModule {

    public PackDisplay() {
        super("Resource Pack", "Shows the name of your active resource pack(s).", 6, 372);
    }

    @Override
    protected void renderContent(DrawContext ctx, int screenWidth, int screenHeight) {
        var packs = mc.getResourcePackManager().getEnabledNames();
        String text = packs.isEmpty() ? "Default" : String.join(", ", packs);
        if (text.length() > 30) text = text.substring(0, 27) + "...";

        int width = mc.textRenderer.getWidth(text) + 12;
        ctx.fill(0, 0, width, 14, XTheme.PANEL_BG);
        ctx.fill(0, 0, 3, 14, XTheme.ACCENT);
        ctx.drawText(mc.textRenderer, text, 6, 3, XTheme.TEXT_PRIMARY, true);
    }

    @Override
    public int getWidth() {
        return 200;
    }

    @Override
    public int getHeight() {
        return 14;
    }
}
