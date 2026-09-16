package ridzzxmc.xclient.modules.render;

import net.minecraft.client.gui.DrawContext;
import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.RenderEvent;
import ridzzxmc.xclient.core.event.events.TickEvent;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.gui.theme.XTheme;
import ridzzxmc.xclient.util.RenderUtil;

/**
 * Briefly flashes a "-N" popup above the crosshair whenever your own health
 * drops, by comparing {@code mc.player.getHealth()} tick over tick. Purely a
 * readability aid for a value already shown on the vanilla hunger/health
 * bars - it doesn't reveal anyone else's health or hook into combat in any
 * way.
 */
public class DamageIndicator extends Module {

    private static final long DURATION_MS = 900;

    private float lastHealth = -1f;
    private float shownAmount = 0f;
    private long shownAt = 0L;

    public DamageIndicator() {
        super("Damage Indicator", "Flashes how much health you just lost near the crosshair.", ModuleCategory.RENDER);
    }

    @Override
    protected void onDisable() {
        lastHealth = -1f;
        shownAmount = 0f;
    }

    @SubscribeEvent
    private void xclient$onTick(TickEvent event) {
        if (event.getPhase() != TickEvent.Phase.END || mc.player == null) return;

        float current = mc.player.getHealth();
        if (lastHealth >= 0f && current < lastHealth - 0.01f) {
            shownAmount = lastHealth - current;
            shownAt = System.currentTimeMillis();
        }
        lastHealth = current;
    }

    @SubscribeEvent
    private void xclient$onRender(RenderEvent event) {
        if (event.getType() != RenderEvent.Type.HUD_2D || mc.options.hudHidden) return;
        if (shownAmount <= 0f) return;

        long elapsed = System.currentTimeMillis() - shownAt;
        if (elapsed >= DURATION_MS) {
            shownAmount = 0f;
            return;
        }

        float t = elapsed / (float) DURATION_MS;
        int alpha = (int) (255 * (1f - t));

        String label = String.format("-%.1f", shownAmount);
        DrawContext ctx = event.getDrawContext();
        var text = mc.textRenderer;
        int cx = mc.getWindow().getScaledWidth() / 2;
        int cy = mc.getWindow().getScaledHeight() / 2;
        int w = text.getWidth(label);
        int y = cy - 24 - (int) (10 * t);

        ctx.drawText(text, label, cx - w / 2, y, RenderUtil.withAlpha(XTheme.ERROR, alpha), true);
    }
}
