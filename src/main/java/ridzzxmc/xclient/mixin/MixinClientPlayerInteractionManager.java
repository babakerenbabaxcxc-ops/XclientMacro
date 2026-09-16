package ridzzxmc.xclient.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ridzzxmc.xclient.XClientMod;
import ridzzxmc.xclient.modules.render.KillEffect;

/**
 * The only client-side signal that "you attacked this entity" exists at all -
 * there's no packet or event telling the client it landed a killing blow, so
 * {@link KillEffect} has to start from this and watch for the target dying
 * shortly after (see that class's javadoc for the full reasoning).
 * <p>
 * {@code attackEntity(PlayerEntity, Entity)} has kept this exact name and
 * signature across every Yarn build going back years (confirmed present,
 * unchanged, through 1.21.11) - low risk as mixin targets go, but it's still
 * the one line to check first if this ever fails to find its target method.
 */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class MixinClientPlayerInteractionManager {

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void xclient$onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (XClientMod.getModuleManager() == null) return;
        XClientMod.getModuleManager().getModule(KillEffect.class).ifPresent(m -> m.onAttack(target));
    }
}
