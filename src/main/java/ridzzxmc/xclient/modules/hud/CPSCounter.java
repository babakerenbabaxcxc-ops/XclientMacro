package ridzzxmc.xclient.modules.hud;

import net.minecraft.client.gui.DrawContext;
import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.InputEvent;
import ridzzxmc.xclient.core.event.events.TickEvent;
import ridzzxmc.xclient.gui.theme.XTheme;

import java.util.ArrayDeque;
import java.util.Deque;

public class CPSCounter extends HudModule {

    private final Deque<Long> leftClicks = new ArrayDeque<>();
    private final Deque<Long> rightClicks = new ArrayDeque<>();

    public CPSCounter() {
        super("CPS Counter", "Displays your clicks-per-second (left/right).", 6, 60);
    }

    @SubscribeEvent
    public void onInput(InputEvent event) {
        if (!event.isMouse() || event.getAction() != InputEvent.Action.PRESS) return;
        long now = System.currentTimeMillis();
        // GLFW mouse button 0 = left, 1 = right
        if (event.getKey() == 0) leftClicks.addLast(now);
        if (event.getKey() == 1) rightClicks.addLast(now);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        long cutoff = System.currentTimeMillis() - 1000;
        leftClicks.removeIf(t -> t < cutoff);
        rightClicks.removeIf(t -> t < cutoff);
    }

    @Override
    protected void renderContent(DrawContext ctx, int screenWidth, int screenHeight) {
        String text = leftClicks.size() + " / " + rightClicks.size() + " CPS";
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
