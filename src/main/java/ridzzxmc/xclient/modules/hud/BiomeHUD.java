package ridzzxmc.xclient.modules.hud;

import net.minecraft.client.gui.DrawContext;
import ridzzxmc.xclient.gui.theme.XTheme;

public class BiomeHUD extends HudModule {

    public BiomeHUD() {
        super("Biome", "Shows the biome you're currently standing in.", 6, 300);
    }

    @Override
    protected void renderContent(DrawContext ctx, int screenWidth, int screenHeight) {
        if (mc.player == null || mc.world == null) return;

        var biomeEntry = mc.world.getBiome(mc.player.getBlockPos());
        String name = biomeEntry.getKey()
                .map(key -> key.getValue().getPath().replace('_', ' '))
                .orElse("unknown");
        String text = capitalize(name);

        int width = mc.textRenderer.getWidth(text) + 12;
        ctx.fill(0, 0, width, 14, XTheme.PANEL_BG);
        ctx.fill(0, 0, 3, 14, XTheme.ACCENT);
        ctx.drawText(mc.textRenderer, text, 6, 3, XTheme.TEXT_PRIMARY, true);
    }

    private String capitalize(String s) {
        if (s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder();
        for (String word : s.split(" ")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    @Override
    public int getWidth() {
        return 160;
    }

    @Override
    public int getHeight() {
        return 14;
    }
}
