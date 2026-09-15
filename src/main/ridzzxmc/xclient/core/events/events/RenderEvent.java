package ridzzxmc.xclient.core.event.events;

import net.minecraft.client.gui.DrawContext;
import ridzzxmc.xclient.core.event.Event;

/**
 * Fired every frame. {@link Type#HUD_2D} carries a {@link DrawContext} for
 * overlay drawing (HUD widgets, watermark, ClickGUI backdrop effects).
 * {@link Type#WORLD_3D} is posted from WorldRenderEvents.AFTER_TRANSLUCENT for
 * modules that draw in world-space (glow outlines, particles tweaks, etc).
 */
public class RenderEvent extends Event {

    public enum Type { HUD_2D, WORLD_3D }

    private final Type type;
    private final DrawContext drawContext;
    private final float tickDelta;

    public RenderEvent(Type type, DrawContext drawContext, float tickDelta) {
        this.type = type;
        this.drawContext = drawContext;
        this.tickDelta = tickDelta;
    }

    public Type getType() {
        return type;
    }

    public DrawContext getDrawContext() {
        return drawContext;
    }

    public float getTickDelta() {
        return tickDelta;
    }
}
