package ridzzxmc.xclient.modules.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.RenderEvent;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.BooleanSetting;
import ridzzxmc.xclient.gui.theme.XTheme;

/**
 * Shows the name (and, optionally, coordinates) of whichever block is
 * currently under the crosshair, in a small themed label just above it.
 * Purely informational - reads the same client-side raycast result vanilla
 * already uses for block breaking/placing ({@link net.minecraft.client.MinecraftClient#crosshairTarget}),
 * doesn't add reach, doesn't reveal anything not already visible on screen.
 */
public class BlockOverlayModule extends Module {

    private final BooleanSetting showCoords = register(new BooleanSetting("Show Coordinates", true));

    public BlockOverlayModule() {
        super("Block Overlay", "Shows the name of the block you're looking at.", ModuleCategory.RENDER);
    }

    @SubscribeEvent
    private void xclient$onRender(RenderEvent event) {
        if (event.getType() != RenderEvent.Type.HUD_2D || mc.options.hudHidden) return;
        if (mc.player == null || mc.world == null) return;

        HitResult target = mc.crosshairTarget;
        if (!(target instanceof BlockHitResult blockHit) || target.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = blockHit.getBlockPos();
        var state = mc.world.getBlockState(pos);
        if (state.isAir()) return;

        String name = state.getBlock().getName().getString();
        String label = showCoords.getValue()
                ? name + "  (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")"
                : name;

        DrawContext ctx = event.getDrawContext();
        var text = mc.textRenderer;
        int cx = mc.getWindow().getScaledWidth() / 2;
        int cy = mc.getWindow().getScaledHeight() / 2;
        int w = text.getWidth(label);
        int x = cx - w / 2;
        int y = cy - 24;

        ctx.fill(x - 4, y - 3, x + w + 4, y + 10, XTheme.PANEL_BG);
        ctx.fill(x - 4, y - 3, x - 2, y + 10, XTheme.ACCENT);
        ctx.drawText(text, label, x, y, XTheme.TEXT_PRIMARY, true);
    }
}
