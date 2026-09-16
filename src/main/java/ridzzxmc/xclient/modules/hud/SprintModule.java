package ridzzxmc.xclient.modules.hud;

import net.minecraft.client.gui.DrawContext;
import ridzzxmc.xclient.gui.theme.XTheme;

public class SprintModule extends HudModule {

    public SprintModule() {
        super("Sprint Indicator", "Shows a small icon while you're sprinting.", 6, 408);
    }

    @Override
    protected void renderContent(DrawContext ctx, int screenWidth, int screenHeight) {
        if (mc.player == null || !mc.player.isSprinting()) return;

        String text = "SPRINTING";
        int width = mc.textRenderer.getWidth(text) + 12;
        ctx.fill(0, 0, width, 14, XTheme.PANEL_BG);
        ctx.fill(0, 0, 3, 14, XTheme.ACCENT);
        ctx.drawText(mc.textRenderer, text, 6, 3, XTheme.ACCENT, true);
    }

    @Override
    public int getWidth() {
        return 90;
    }

    @Override
    public int getHeight() {
        return 14;
    }
}
