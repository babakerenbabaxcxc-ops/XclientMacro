package ridzzxmc.xclient.modules.hud;

import net.minecraft.client.gui.DrawContext;
import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.TickEvent;
import ridzzxmc.xclient.gui.theme.XTheme;

public class SessionInfoHUD extends HudModule {

    private long ticksAlive = 0;

    public SessionInfoHUD() {
        super("Session Info", "Tracks how long you've been in your current session.", 6, 300);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (event.getPhase() == TickEvent.Phase.END) ticksAlive++;
    }

    @Override
    protected void renderContent(DrawContext ctx, int screenWidth, int screenHeight) {
        long totalSeconds = ticksAlive / 20;
        String text = String.format("Session: %02d:%02d:%02d", totalSeconds / 3600, (totalSeconds / 60) % 60, totalSeconds % 60);

        int width = mc.textRenderer.getWidth(text) + 12;
        ctx.fill(0, 0, width, 14, XTheme.PANEL_BG);
        ctx.fill(0, 0, 3, 14, XTheme.ACCENT);
        ctx.drawText(mc.textRenderer, text, 6, 3, XTheme.TEXT_PRIMARY, true);
    }

    @Override
    public int getWidth() {
        return 130;
    }

    @Override
    public int getHeight() {
        return 14;
    }
}
