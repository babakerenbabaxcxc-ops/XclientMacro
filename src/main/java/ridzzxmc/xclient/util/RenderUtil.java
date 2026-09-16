package ridzzxmc.xclient.util;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import ridzzxmc.xclient.gui.theme.XTheme;

/**
 * Shared drawing helpers for the "Feather-like" red UI: rounded cards,
 * drop shadows and a background blur pass. Built on {@link DrawContext}
 * so it works identically in ClickGUI, HUD editor and toast rendering.
 */
public final class RenderUtil {

    private RenderUtil() {}

    /** Filled rounded rectangle, approximated with layered fills (cheap, no shader dependency). */
    public static void roundedRect(DrawContext ctx, int x, int y, int width, int height, int radius, int color) {
        radius = Math.min(radius, Math.min(width, height) / 2);

        // center cross
        ctx.fill(x + radius, y, x + width - radius, y + height, color);
        ctx.fill(x, y + radius, x + radius, y + height - radius, color);
        ctx.fill(x + width - radius, y + radius, x + width, y + height - radius, color);

        // corners via shrinking fills (cheap circle approximation)
        int steps = Math.max(4, radius / 2);
        for (int i = 0; i < steps; i++) {
            double t = (i + 1) / (double) steps;
            int inset = (int) Math.round(radius - radius * Math.sin(Math.PI / 2 * t));
            int rowHeight = Math.max(1, radius / steps);
            int rowY = y + (steps - i - 1) * rowHeight / steps + radius / steps;
            ctx.fill(x + inset, y + radius - (i + 1) * radius / steps, x + width - inset, y + radius - i * radius / steps, color);
            ctx.fill(x + inset, y + height - radius + i * radius / steps, x + width - inset, y + height - radius + (i + 1) * radius / steps, color);
        }
    }

    /** Soft drop shadow behind a card - several translucent layers offset outward. */
    public static void dropShadow(DrawContext ctx, int x, int y, int width, int height, int radius) {
        int layers = 6;
        for (int i = layers; i > 0; i--) {
            int alpha = (int) (28 * (i / (float) layers));
            int spread = i * 2;
            roundedRect(ctx, x - spread, y - spread, width + spread * 2, height + spread * 2, radius + spread,
                    (alpha << 24));
        }
    }

    /**
     * Backdrop blur behind the ClickGUI. True gaussian blur needs a shader pass;
     * this uses a cheap dimmed-overlay approximation that reads well with the
     * translucent card system and costs nothing extra on low-end mobile GPUs.
     */
    public static void blurBackdrop(DrawContext ctx, int screenWidth, int screenHeight) {
        ctx.fill(0, 0, screenWidth, screenHeight, 0x90121016);
    }

    public static void card(DrawContext ctx, int x, int y, int width, int height, boolean highlighted) {
        dropShadow(ctx, x, y, width, height, 10);
        roundedRect(ctx, x, y, width, height, 10, highlighted ? XTheme.CARD_HOVER : XTheme.CARD_BG);
        if (highlighted) {
            roundedRect(ctx, x, y, width, 2, 0, XTheme.ACCENT);
        }
    }

    public static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    /**
     * Draws a standalone (non-atlas) PNG icon, stretched to fill an x,y,size,size box.
     * <p>
     * Deliberately uses {@link DrawContext#drawTexture} (binds {@code icon} directly)
     * rather than {@link DrawContext#drawGuiTexture}. The latter resolves its
     * {@link Identifier} through the vanilla GUI *sprite atlas* - which only
     * contains sprites vanilla itself declared (plus anything a resource pack
     * explicitly adds via {@code atlases/gui.json}). A mod's own icon, living
     * under its own namespace with no such atlas entry, was never actually in
     * that atlas, so {@code drawGuiTexture} silently resolved it to the
     * atlas's own missing-sprite placeholder - the classic magenta/black
     * checkerboard - regardless of whether the PNG file itself was fine.
     * {@code drawTexture} instead binds the identifier as a plain standalone
     * texture (the same category of call already used, and already working,
     * for the entity-layer badge in {@code MixinPlayerEntityRenderer}), which
     * is exactly what a loose {@code assets/<namespace>/textures/...} PNG
     * needs.
     * <p>
     * NOTE: the {@code drawTexture} overload used below (pipeline, id, x, y,
     * u, v, width, height, textureWidth, textureHeight[, color]) mirrors the
     * already-confirmed {@code drawGuiTexture} shape elsewhere in this class.
     * If a future mappings update shifts its exact parameter order, this is
     * the one call site to check - nothing else in this file depends on it.
     */
    public static void drawIcon(DrawContext ctx, Identifier icon, int x, int y, int size) {
        drawIcon(ctx, icon, x, y, size, 0xFFFFFFFF);
    }

    /** Same as {@link #drawIcon} but with an explicit tint/alpha color (0xAARRGGBB, white = no tint). */
    public static void drawIcon(DrawContext ctx, Identifier icon, int x, int y, int size, int tintColor) {
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, icon, x, y, 0f, 0f, size, size, size, size, tintColor);
    }

    /**
     * Outline-only rectangle (straight corners), built from four plain fills so it
     * never depends on anything beyond {@link DrawContext#fill}. Used for the red
     * hover border around a highlighted card; the card body underneath already has
     * rounded corners from {@link #roundedRect}, so this reads as a clean highlight
     * without needing a punch-out trick (filling with a 0-alpha color would just
     * blend as a no-op, not erase anything, so a true rounded outline isn't worth
     * the extra complexity here).
     */
    public static void rectOutline(DrawContext ctx, int x, int y, int width, int height, int thickness, int color) {
        ctx.fill(x, y, x + width, y + thickness, color);
        ctx.fill(x, y + height - thickness, x + width, y + height, color);
        ctx.fill(x, y, x + thickness, y + height, color);
        ctx.fill(x + width - thickness, y, x + width, y + height, color);
    }

    /**
     * Same idea as {@link #drawIcon} but for a non-square box - stretches the
     * *whole* source image to fill an arbitrary width x height rectangle
     * (used for the cape artwork preview, which isn't square). Passing
     * {@code width}/{@code height} as both the region size AND the
     * "reference" texture size makes the normalized UV span exactly [0,1] on
     * both axes regardless of the image's real pixel dimensions - i.e. "draw
     * the entire texture, stretched to this box" - the same trick
     * {@link #drawIcon} relies on.
     */
    public static void drawIconRect(DrawContext ctx, Identifier icon, int x, int y, int width, int height) {
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, icon, x, y, 0f, 0f, width, height, width, height, 0xFFFFFFFF);
    }

    /**
     * Draws a {@code regionSize}-pixel square crop starting at texel
     * ({@code u}, {@code v}) out of a {@code textureSize}x{@code textureSize}
     * source texture, at 1:1 scale (no stretching) - used for the small face
     * crop out of a 64x64 player skin. Wrap in a
     * {@code ctx.getMatrices().pushMatrix()/scale(...)/popMatrix()} block (as
     * {@code HudModule} already does elsewhere) to enlarge it without
     * needing a second, stretching-aware overload.
     */
    public static void drawTextureRegion(DrawContext ctx, Identifier texture, int x, int y,
                                          int u, int v, int regionSize, int textureSize) {
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, regionSize, regionSize,
                textureSize, textureSize, 0xFFFFFFFF);
    }

    /**
     * Fallback "avatar badge" for a module with no matching icon asset: a colored
     * rounded square with a single centered letter, same idea as a Slack/Discord
     * placeholder avatar. Always available, never depends on a texture file existing.
     */
    public static void initialBadge(DrawContext ctx, TextRenderer textRenderer, char letter, int x, int y, int size, int bgColor) {
        roundedRect(ctx, x, y, size, size, size / 4, bgColor);
        String s = String.valueOf(Character.toUpperCase(letter));
        int textWidth = textRenderer.getWidth(s);
        int cx = x + size / 2;
        int cy = y + (size - textRenderer.fontHeight) / 2;
        ctx.drawText(textRenderer, s, cx - textWidth / 2, cy, 0xFFFFFFFF, true);
    }
}
