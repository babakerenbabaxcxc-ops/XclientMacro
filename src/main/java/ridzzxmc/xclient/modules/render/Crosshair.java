package ridzzxmc.xclient.modules.render;

import net.minecraft.client.gui.DrawContext;
import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.RenderEvent;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.ColorSetting;
import ridzzxmc.xclient.core.settings.SliderSetting;

/**
 * Replaces the vanilla crosshair with a themed dot/cross - purely cosmetic,
 * does not affect hit-detection or accuracy.
 */
public class Crosshair extends Module {

    private final SliderSetting size = register(new SliderSetting("Size", 4, 2, 10, 1, true));
    private final ColorSetting color = register(new ColorSetting("Color", 0xFFE53935));

    public Crosshair() {
        super("Crosshair", "Replaces the vanilla crosshair with a themed dot.", ModuleCategory.RENDER);
    }

    @SubscribeEvent
    public void onRender(RenderEvent event) {
        if (event.getType() != RenderEvent.Type.HUD_2D || mc.options.hudHidden) return;

        DrawContext ctx = event.getDrawContext();
        int cx = mc.getWindow().getScaledWidth() / 2;
        int cy = mc.getWindow().getScaledHeight() / 2;
        int s = size.getValue().intValue();

        ctx.fill(cx - s / 2, cy - 1, cx + s / 2, cy + 1, color.getValue());
        ctx.fill(cx - 1, cy - s / 2, cx + 1, cy + s / 2, color.getValue());
    }
}
