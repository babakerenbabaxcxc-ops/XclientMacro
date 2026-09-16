package ridzzxmc.xclient.modules.hud;

import net.minecraft.client.gui.DrawContext;
import ridzzxmc.xclient.gui.theme.XTheme;

public class WatermarkHUD extends HudModule {

    public WatermarkHUD() {
        super("Watermark", "Shows the X Client logo/wordmark in a screen corner.", 6, 6, true);
    }

    @Override
    protected void renderContent(DrawContext ctx, int screenWidth, int screenHeight) {
        var text = mc.textRenderer;
        int width = getWidth();

        ctx.fill(0, 0, width, 14, XTheme.PANEL_BG);
        ctx.fill(0, 0, 3, 14, XTheme.ACCENT);

        ctx.drawText(text, "X", 6, 3, XTheme.ACCENT, true);
        ctx.drawText(text, "Client", 6 + text.getWidth("X") + 2, 3, XTheme.TEXT_PRIMARY, true);
        ctx.drawText(text, "by ridzzxmc", 6 + text.getWidth("X Client") + 6, 3, XTheme.TEXT_PRIMARY, true);
    }

    @Override
    public int getWidth() {
        return 18 + mc.textRenderer.getWidth("X Client by ridzzxmc");
    }

    @Override
    public int getHeight() {
        return 14;
    }
}
