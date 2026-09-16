package ridzzxmc.xclient.modules.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import ridzzxmc.xclient.gui.theme.XTheme;

import java.util.Collection;

public class PotionHUD extends HudModule {

    public PotionHUD() {
        super("Potion HUD", "Lists your active status effects with remaining duration.", 6, 226);
    }

    @Override
    protected void renderContent(DrawContext ctx, int screenWidth, int screenHeight) {
        if (mc.player == null) return;
        Collection<StatusEffectInstance> effects = mc.player.getStatusEffects();
        if (effects.isEmpty()) return;

        int rowY = 0;
        for (StatusEffectInstance effect : effects) {
            String name = effect.getEffectType().value().getName().getString();
            int seconds = effect.getDuration() / 20;
            String text = name + " " + (effect.getAmplifier() + 1) + " (" + (seconds / 60) + ":" + String.format("%02d", seconds % 60) + ")";

            int width = mc.textRenderer.getWidth(text) + 12;
            ctx.fill(0, rowY, width, rowY + 14, XTheme.PANEL_BG);
            ctx.fill(0, rowY, 3, rowY + 14, XTheme.ACCENT);
            ctx.drawText(mc.textRenderer, text, 6, rowY + 3, XTheme.TEXT_PRIMARY, true);
            rowY += 16;
        }
    }

    @Override
    public int getWidth() {
        return 180;
    }

    @Override
    public int getHeight() {
        return mc.player == null ? 14 : Math.max(14, mc.player.getStatusEffects().size() * 16);
    }
}
