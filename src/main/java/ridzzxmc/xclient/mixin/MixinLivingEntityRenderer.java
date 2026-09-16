package ridzzxmc.xclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.Entity;
import ridzzxmc.xclient.XClientMod;
import ridzzxmc.xclient.modules.render.XClientIcon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Makes the local player's own nametag render even in first person, but only
 * while {@link XClientIcon} is enabled. Vanilla normally skips your own
 * label in first person (it compares the rendered entity against the camera
 * entity) - left alone, that would also hide the badge
 * {@link MixinPlayerEntityRenderer} draws next to it whenever the icon
 * module is on. When the module is off this is a complete no-op and vanilla
 * behavior is untouched.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer {

    @ModifyExpressionValue(
            method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;getCameraEntity()Lnet/minecraft/entity/Entity;"
            )
    )
    private Entity xclient$obfuscateCameraEntity(Entity original) {
        boolean iconEnabled = XClientMod.getModuleManager() != null
                && XClientMod.getModuleManager().getModule(XClientIcon.class).map(icon -> icon.isEnabled()).orElse(false);
        return iconEnabled ? null : original;
    }
}
