package ridzzxmc.xclient.modules.hud;

import net.minecraft.client.gui.DrawContext;
import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.TickEvent;
import ridzzxmc.xclient.gui.theme.XTheme;

public class SpeedHUD extends HudModule {

    private double lastX, lastZ;
    private double bps;
    private boolean hasLastPos;

    public SpeedHUD() {
        super("Speed", "Shows your horizontal movement speed in blocks/second.", 6, 208);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (event.getPhase() != TickEvent.Phase.END || mc.player == null) return;

        if (hasLastPos) {
            double dx = mc.player.getX() - lastX;
            double dz = mc.player.getZ() - lastZ;
            // A tick is a fixed 1/20 second, unlike a render frame - this is what
            // makes "distance this tick * 20" an accurate blocks/second figure.
            bps = Math.sqrt(dx * dx + dz * dz) * 20;
        }
        lastX = mc.player.getX();
        lastZ = mc.player.getZ();
        hasLastPos = true;
    }

    @Override
    protected void renderContent(DrawContext ctx, int screenWidth, int screenHeight) {
        if (mc.player == null) return;

        String text = String.format("%.2f b/s", Math.min(bps, 43.0));
        int width = mc.textRenderer.getWidth(text) + 12;
        ctx.fill(0, 0, width, 14, XTheme.PANEL_BG);
        ctx.fill(0, 0, 3, 14, XTheme.ACCENT);
        ctx.drawText(mc.textRenderer, text, 6, 3, XTheme.TEXT_PRIMARY, true);
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
