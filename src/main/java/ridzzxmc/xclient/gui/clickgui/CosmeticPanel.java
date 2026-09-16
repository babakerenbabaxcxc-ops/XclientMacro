package ridzzxmc.xclient.gui.clickgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import ridzzxmc.xclient.XClientMod;
import ridzzxmc.xclient.gui.theme.XTheme;
import ridzzxmc.xclient.modules.cosmetic.XClientCape;
import ridzzxmc.xclient.util.RenderUtil;

/**
 * Content for {@code Page.COSMETIC}: unlike every other page, this isn't a
 * filtered module grid - it's a dedicated cape browser/equip screen, so its
 * layout lives here instead of being shoehorned into {@link ClickGUIScreen}'s
 * generic card-grid code.
 * <p>
 * Two cards side by side: the cape artwork with its name and an Equip toggle
 * on the left, and a small "preview" card on the right showing your own
 * skin's face next to a caption reflecting the equip state, so
 * equipping/unequipping is visible without leaving the menu. A full rotating
 * 3D body preview would need the entity-preview render helper vanilla's own
 * skin customization screen uses, whose exact call shape after the
 * OrderedRenderCommandQueue rendering rewrite isn't confirmed for this build
 * (see {@link XClientCape}'s class doc) - this face+swatch preview is the
 * safe subset that only needs the same plain 2D texture draws already used
 * everywhere else in this file.
 */
final class CosmeticPanel {

    private static final Identifier CAPE_TEXTURE =
            Identifier.of(XClientMod.RESOURCE_NAMESPACE, "textures/cosmetics/xclient_cape.png");

    private static final int CAPE_ASPECT_H_PER_W = 155; // cape art is ~512x792 -> ~155% of its width tall

    private CosmeticPanel() {
    }

    private record Layout(int leftX, int leftW, int rightX, int rightW,
                           int capeArtX, int capeArtY, int capeArtW, int capeArtH,
                           int toggleX, int toggleY, int toggleW, int toggleH) {
    }

    private static Layout layout(int x, int y, int w, int bottom) {
        int gap = 10;
        int leftW = (int) (w * 0.56);
        int rightW = w - leftW - gap;
        int rightX = x + leftW + gap;

        int capeArtW = Math.min(leftW - 24, 150);
        int capeArtH = capeArtW * CAPE_ASPECT_H_PER_W / 100;
        int capeArtX = x + 12;
        int capeArtY = y + 30;

        int toggleW = leftW - 24;
        int toggleH = 20;
        int toggleX = x + 12;
        int toggleY = Math.min(bottom - toggleH - 12, capeArtY + capeArtH + 34);

        return new Layout(x, leftW, rightX, rightW, capeArtX, capeArtY, capeArtW, capeArtH,
                toggleX, toggleY, toggleW, toggleH);
    }

    static void render(DrawContext ctx, int x, int y, int w, int bottom, int mouseX, int mouseY) {
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        var cape = XClientMod.getModuleManager().getModule(XClientCape.class).orElse(null);
        boolean equipped = cape != null && cape.isEnabled();

        Layout l = layout(x, y, w, bottom);
        int cardH = bottom - y;

        // ---- left card: cape artwork + equip toggle ----
        RenderUtil.card(ctx, l.leftX(), y, l.leftW(), cardH, false);
        ctx.drawText(mc.textRenderer, "X Client Cape", l.leftX() + 12, y + 10, XTheme.ACCENT, true);

        RenderUtil.dropShadow(ctx, l.capeArtX(), l.capeArtY(), l.capeArtW(), l.capeArtH(), 4);
        RenderUtil.drawIconRect(ctx, CAPE_TEXTURE, l.capeArtX(), l.capeArtY(), l.capeArtW(), l.capeArtH());

        boolean toggleHovered = mouseX >= l.toggleX() && mouseX <= l.toggleX() + l.toggleW()
                && mouseY >= l.toggleY() && mouseY <= l.toggleY() + l.toggleH();
        RenderUtil.roundedRect(ctx, l.toggleX(), l.toggleY(), l.toggleW(), l.toggleH(), 8,
                equipped ? XTheme.ACCENT : (toggleHovered ? XTheme.CARD_HOVER : XTheme.TOGGLE_OFF));
        String toggleLabel = equipped ? "Equipped" : "Equip Cape";
        ctx.drawText(mc.textRenderer, toggleLabel,
                l.toggleX() + (l.toggleW() - mc.textRenderer.getWidth(toggleLabel)) / 2, l.toggleY() + 6,
                0xFFFFFFFF, true);

        // ---- right card: your skin's face + caption, reflecting equip state ----
        RenderUtil.card(ctx, l.rightX(), y, l.rightW(), cardH, false);
        ctx.drawText(mc.textRenderer, "Preview", l.rightX() + 12, y + 10, XTheme.TEXT_SECONDARY, true);

        int faceScale = 6;
        int faceSize = 8 * faceScale;
        int faceX = l.rightX() + (l.rightW() - faceSize) / 2;
        int faceY = y + 26;

        if (mc.player != null) {
            try {
                Identifier skin = mc.player.getSkinTextures().texture();
                ctx.getMatrices().pushMatrix();
                ctx.getMatrices().translate(faceX, faceY);
                ctx.getMatrices().scale(faceScale, faceScale);
                RenderUtil.drawTextureRegion(ctx, skin, 0, 0, 8, 8, 8, 64);
                ctx.getMatrices().popMatrix();
            } catch (Exception ignored) {
                // Skin texture not downloaded/available yet (e.g. straight after
                // joining a world) - just skip the face this frame rather than
                // leaving a half-drawn preview or crashing the menu over it.
            }
        }

        String caption = equipped ? "wearing the cape" : "cape not equipped";
        ctx.drawText(mc.textRenderer, caption,
                l.rightX() + (l.rightW() - mc.textRenderer.getWidth(caption)) / 2, faceY + faceSize + 10,
                equipped ? XTheme.SUCCESS : XTheme.TEXT_SECONDARY, false);
    }

    /** @return true if the click landed on something this panel handles (the equip toggle). */
    static boolean mouseClicked(int x, int y, int w, int bottom, double mouseX, double mouseY) {
        Layout l = layout(x, y, w, bottom);
        if (mouseX >= l.toggleX() && mouseX <= l.toggleX() + l.toggleW()
                && mouseY >= l.toggleY() && mouseY <= l.toggleY() + l.toggleH()) {
            XClientMod.getModuleManager().getModule(XClientCape.class).ifPresent(
                    ridzzxmc.xclient.core.module.Module::toggle);
            return true;
        }
        return false;
    }
}
