package ridzzxmc.xclient.modules.hud;

import net.minecraft.client.gui.DrawContext;
import ridzzxmc.xclient.gui.theme.XTheme;

public class MemoryHUD extends HudModule {

    public MemoryHUD() {
        super("Memory", "Displays JVM heap memory usage.", 6, 354);
    }

    @Override
    protected void renderContent(DrawContext ctx, int screenWidth, int screenHeight) {
        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long maxMb = rt.maxMemory() / 1024 / 1024;
        String text = usedMb + " / " + maxMb + " MB";

        float ratio = usedMb / (float) maxMb;
        int color = ratio < 0.6f ? XTheme.SUCCESS : ratio < 0.85f ? XTheme.WARNING : XTheme.ERROR;

        int width = mc.textRenderer.getWidth(text) + 12;
        ctx.fill(0, 0, width, 14, XTheme.PANEL_BG);
        ctx.fill(0, 0, 3, 14, color);
        ctx.drawText(mc.textRenderer, text, 6, 3, XTheme.TEXT_PRIMARY, true);
    }

    @Override
    public int getWidth() {
        return 110;
    }

    @Override
    public int getHeight() {
        return 14;
    }
}
