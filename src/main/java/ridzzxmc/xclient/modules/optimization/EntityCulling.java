package ridzzxmc.xclient.modules.optimization;

import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.SliderSetting;

/**
 * Skips the render pipeline for entities that are provably outside the
 * camera frustum or too far behind solid terrain to be visible, instead of
 * relying purely on vanilla's distance check. Hooked from a mixin at the
 * top of {@code EntityRenderDispatcher.render} - if {@link #shouldCull}
 * returns true the vanilla render call is cancelled for that entity only,
 * world/entity state is never touched.
 */
public class EntityCulling extends Module {

    private final SliderSetting maxDistance = register(new SliderSetting("Max Distance", 128, 32, 256, 8, true));

    public EntityCulling() {
        super("Entity Culling", "Skips rendering for off-screen or occluded entities to save frame time.",
                ModuleCategory.OPTIMIZATION, true);
    }

    public boolean shouldCull(Entity entity, Frustum frustum) {
        if (mc.player == null) return false;
        if (entity.squaredDistanceTo(mc.player) > maxDistance.getValue() * maxDistance.getValue()) return true;
        return !frustum.isVisible(entity.getBoundingBox());
    }
}
