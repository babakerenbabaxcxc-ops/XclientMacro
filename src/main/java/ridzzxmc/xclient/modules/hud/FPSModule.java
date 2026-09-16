package ridzzxmc.xclient.modules.hud;

import net.minecraft.client.gui.DrawContext;
import ridzzxmc.xclient.gui.theme.XTheme;

public class FPSModule extends HudModule {

    public FPSModule() {
        super("FPS", "Displays current frames-per-second.", 6, 78, true);
    }

    @Override
    protected void renderContent(DrawContext ctx, int screenWidth, int screenHeight) {
        int fps = mc.getCurrentFps();
        String text = fps + " FPS";
        int color = fps >= 60 ? XTheme.SUCCESS : fps >= 30 ? XTheme.WARNING : XTheme.ERROR;

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
