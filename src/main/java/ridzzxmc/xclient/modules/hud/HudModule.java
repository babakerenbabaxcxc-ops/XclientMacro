package ridzzxmc.xclient.modules.hud;

import net.minecraft.client.gui.DrawContext;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.SliderSetting;

/**
 * Base for every HUD widget. Handles the shared drag/resize/scale state that
 * the HUD editor screen manipulates - concrete HUD modules only implement
 * {@link #renderContent(DrawContext, int, int)} and report their own size.
 */
public abstract class HudModule extends Module {

    protected float x;
    protected float y;
    protected final SliderSetting scale = register(new SliderSetting("Scale", 1.0, 0.5, 2.0, 0.05));

    private final float defaultX;
    private final float defaultY;

    private boolean dragging = false;

    protected HudModule(String name, String description, float defaultX, float defaultY) {
        super(name, description, ModuleCategory.HUD);
        this.x = defaultX;
        this.y = defaultY;
        this.defaultX = defaultX;
        this.defaultY = defaultY;
    }

    protected HudModule(String name, String description, float defaultX, float defaultY, boolean enabledByDefault) {
        super(name, description, ModuleCategory.HUD, enabledByDefault);
        this.x = defaultX;
        this.y = defaultY;
        this.defaultX = defaultX;
        this.defaultY = defaultY;
    }

    /** Draws only this widget's content at (0,0); the caller translates to {@link #x}, {@link #y}. */
    protected abstract void renderContent(DrawContext ctx, int screenWidth, int screenHeight);

    /** Reported footprint in unscaled pixels, used by the HUD editor for the drag outline and overlap avoidance. */
    public abstract int getWidth();

    public abstract int getHeight();

    @ridzzxmc.xclient.core.event.SubscribeEvent
    private void xclient$onRenderEvent(ridzzxmc.xclient.core.event.events.RenderEvent event) {
        if (event.getType() != ridzzxmc.xclient.core.event.events.RenderEvent.Type.HUD_2D) return;
        render(event.getDrawContext(), mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
    }

    public final void render(DrawContext ctx, int screenWidth, int screenHeight) {
        if (!isEnabled()) return;
        ctx.getMatrices().pushMatrix();
        try {
            ctx.getMatrices().translate((float) x, (float) y);
            ctx.getMatrices().scale((float) scale.getValue(), (float) scale.getValue());
            renderContent(ctx, screenWidth, screenHeight);
        } catch (Exception e) {
            // Never let one broken widget corrupt the matrix stack or take the
            // rest of the HUD down with it - log once per occurrence and move on.
            System.err.println("[X Client] HUD module '" + getName() + "' failed to render:");
            e.printStackTrace();
        } finally {
            // Always balance the push above, even if renderContent() threw -
            // otherwise the matrix stack drifts a level every failed frame and
            // eventually crashes with an IllegalStateException of its own.
            ctx.getMatrices().popMatrix();
        }
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getScale() {
        return scale.getValue().floatValue();
    }

    /** Directly sets scale; SliderSetting clamps to [0.5, 2.0] so this can never go out of range. */
    public void setScale(float value) {
        scale.setValue((double) value);
    }

    /** Relative scale change, e.g. from a mouse-wheel step in the HUD editor. */
    public void adjustScale(float delta) {
        setScale(getScale() + delta);
    }

    /** Restores this widget's original position and scale (used by the HUD editor's per-widget reset). */
    public void resetLayout() {
        this.x = defaultX;
        this.y = defaultY;
        setScale(1.0f);
    }

    public boolean isDragging() {
        return dragging;
    }

    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }
}
