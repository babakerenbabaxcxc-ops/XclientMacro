package ridzzxmc.xclient.modules.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import ridzzxmc.xclient.gui.theme.XTheme;

public class ArmorHUD extends HudModule {

    public ArmorHUD() {
        super("Armor HUD", "Displays equipped armor pieces and durability.", 6, 260);
    }

    @Override
    protected void renderContent(DrawContext ctx, int screenWidth, int screenHeight) {
        if (mc.player == null) return;

        int slot = 0;
        for (ItemStack stack : mc.player.getArmorItems()) {
            int x = slot * 20;
            ctx.fill(x, 0, x + 18, 18, XTheme.CARD_BG);
            if (!stack.isEmpty()) {
                ctx.drawItem(stack, x + 1, 1);
                if (stack.isDamaged()) {
                    float ratio = 1f - (stack.getDamage() / (float) stack.getMaxDamage());
                    int barColor = ratio > 0.5f ? XTheme.SUCCESS : ratio > 0.2f ? XTheme.WARNING : XTheme.ERROR;
                    ctx.fill(x + 1, 16, x + 1 + Math.round(16 * ratio), 18, barColor);
                }
            }
            slot++;
        }
    }

    @Override
    public int getWidth() {
        return 80;
    }

    @Override
    public int getHeight() {
        return 18;
    }
}
