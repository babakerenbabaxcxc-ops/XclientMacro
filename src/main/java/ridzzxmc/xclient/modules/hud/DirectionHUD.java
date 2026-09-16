package ridzzxmc.xclient.modules.hud;

import net.minecraft.client.gui.DrawContext;
import ridzzxmc.xclient.gui.theme.XTheme;

public class DirectionHUD extends HudModule {

    private static final String[] DIRECTIONS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    public DirectionHUD() {
        super("Direction", "Shows the compass direction you're facing.", 6, 336);
    }

    @Override
    protected void renderContent(DrawContext ctx, int screenWidth, int screenHeight) {
        if (mc.player == null) return;

        float yaw = mc.player.getYaw() % 360;
        if (yaw < 0) yaw += 360;
        int index = Math.round(yaw / 45f) % 8;
        String text = DIRECTIONS[index] + String.format(" (%.0f°)", yaw);

        int width = mc.textRenderer.getWidth(text) + 12;
        ctx.fill(0, 0, width, 14, XTheme.PANEL_BG);
        ctx.fill(0, 0, 3, 14, XTheme.ACCENT);
        ctx.drawText(mc.textRenderer, text, 6, 3, XTheme.TEXT_PRIMARY, true);
    }

    @Override
    public int getWidth() {
        return 100;
    }

    @Override
    public int getHeight() {
        return 14;
    }
}
