package ridzzxmc.xclient.modules.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.RenderEvent;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.BooleanSetting;
import ridzzxmc.xclient.gui.theme.XTheme;

/**
 * Shows the name and distance of whichever entity is currently under the
 * crosshair, in a small themed label just above it - reads the same
 * client-side "what am I looking at" state vanilla already tracks for its
 * own attack-indicator/interact logic ({@link net.minecraft.client.MinecraftClient#targetedEntity}).
 * Informational only: it doesn't enlarge hit boxes, change reach, or affect
 * hit detection in any way - purely a label describing what's already
 * directly under your crosshair.
 */
public class HitboxModule extends Module {

    private final BooleanSetting showDistance = register(new BooleanSetting("Show Distance", true));

    public HitboxModule() {
        super("Hitbox", "Shows the name and distance of the entity you're looking at.", ModuleCategory.RENDER);
    }

    @SubscribeEvent
    private void xclient$onRender(RenderEvent event) {
        if (event.getType() != RenderEvent.Type.HUD_2D || mc.options.hudHidden) return;
        if (mc.player == null) return;

        Entity target = mc.targetedEntity;
        if (target == null) return;

        String name = target.getName().getString();
        String label = showDistance.getValue()
                ? name + "  " + String.format("%.1fm", mc.player.distanceTo(target))
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
