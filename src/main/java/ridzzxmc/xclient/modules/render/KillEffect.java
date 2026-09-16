package ridzzxmc.xclient.modules.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.RenderEvent;
import ridzzxmc.xclient.core.event.events.TickEvent;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.BooleanSetting;
import ridzzxmc.xclient.core.settings.ColorSetting;
import ridzzxmc.xclient.core.settings.SliderSetting;
import ridzzxmc.xclient.util.RenderUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Tints the whole screen for a moment right after you get a kill - the same
 * "world flashes a color" beat FiveM's kill feed effect uses. This is a
 * full-screen 2D overlay, not an edit to the actual atmospheric sky/fog
 * renderer: recoloring everything on screen (sky included) reads the same to
 * the player, and it only needs {@code RenderEvent.Type.HUD_2D}, which is
 * still bridged (unlike the removed {@code WORLD_3D} hook other modules'
 * comments mention - see {@code Waypoints}).
 * <p>
 * <b>How a "kill" is detected:</b> the client is never told "you got the
 * kill" the way a server plugin would tell FiveM - health/removal sync is all
 * it has. {@code MixinClientPlayerInteractionManager} records the moment you
 * attack a {@link LivingEntity}; if that entity dies within
 * {@link #TRACK_TIMEOUT_MS} of your hit, this counts it as your kill. That's
 * an approximation (someone else finishing off the same mob in that window
 * would also count) but it's the same approach every client-side "killstreak"
 * or "kill effect" mod uses, since none of them have real server confirmation
 * either.
 */
public class KillEffect extends Module {

    private static final long TRACK_TIMEOUT_MS = 3000;

    private final SliderSetting duration = register(new SliderSetting("Duration (s)", 2.0, 0.5, 8.0, 0.5));
    private final SliderSetting maxOpacity = register(new SliderSetting("Max Opacity (%)", 45, 5, 100, 5, true));
    private final ColorSetting color = register(new ColorSetting("Color", 0xFFAA0000));
    private final BooleanSetting playersOnly = register(new BooleanSetting("Players Only", false));

    /** Entity id -> when we last hit it, so a death shortly after can plausibly be credited to us. */
    private final Map<Integer, Long> recentlyAttacked = new HashMap<>();

    private long triggeredAt = -1;

    public KillEffect() {
        super("Kill Effect", "Flashes the screen a color for a moment after you get a kill.", ModuleCategory.RENDER);
    }

    @Override
    protected void onDisable() {
        recentlyAttacked.clear();
        triggeredAt = -1;
    }

    /** Called from {@code MixinClientPlayerInteractionManager} the instant you attack something. */
    public void onAttack(Entity target) {
        if (!isEnabled() || !(target instanceof LivingEntity)) return;
        if (playersOnly.getValue() && !(target instanceof PlayerEntity)) return;
        recentlyAttacked.put(target.getId(), System.currentTimeMillis());
    }

    @SubscribeEvent
    private void xclient$onTick(TickEvent event) {
        if (event.getPhase() != TickEvent.Phase.END || mc.world == null) return;

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, Long>> it = recentlyAttacked.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Long> tracked = it.next();
            if (now - tracked.getValue() > TRACK_TIMEOUT_MS) {
                it.remove();
                continue;
            }

            var entity = mc.world.getEntityById(tracked.getKey());
            boolean dead = entity == null || entity.isRemoved() || (entity instanceof LivingEntity le && le.isDead());
            if (dead) {
                it.remove();
                triggeredAt = now;
            }
        }
    }

    @SubscribeEvent
    private void xclient$onRender(RenderEvent event) {
        if (event.getType() != RenderEvent.Type.HUD_2D || triggeredAt < 0) return;

        long elapsed = System.currentTimeMillis() - triggeredAt;
        long durationMs = (long) (duration.getValue() * 1000);
        if (elapsed >= durationMs) {
            triggeredAt = -1;
            return;
        }

        // Quick fade in (first 15%), hold, then fade out over the last 40% -
        // a flash rather than a hard cut in or out.
        float t = elapsed / (float) durationMs;
        float strength;
        if (t < 0.15f) {
            strength = t / 0.15f;
        } else if (t > 0.6f) {
            strength = 1f - (t - 0.6f) / 0.4f;
        } else {
            strength = 1f;
        }

        int maxAlpha = (int) (255 * (maxOpacity.getValue() / 100.0));
        int alpha = Math.max(0, Math.min(255, (int) (maxAlpha * strength)));

        DrawContext ctx = event.getDrawContext();
        ctx.fill(0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(),
                RenderUtil.withAlpha(color.getValue(), alpha));
    }
}
