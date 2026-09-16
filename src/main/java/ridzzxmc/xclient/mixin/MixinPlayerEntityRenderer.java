package ridzzxmc.xclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ridzzxmc.xclient.XClientMod;
import ridzzxmc.xclient.modules.render.XClientIcon;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws a small X Client badge next to the local player's own nametag, when
 * the {@link XClientIcon} module is enabled. This replaces the old
 * unregistered {@code MixinEntityRenderer} template - the signature below
 * (a render-state snapshot plus an {@link OrderedRenderCommandQueue} rather
 * than an immediate draw call) is the actual 1.21.11 shape of
 * {@code renderLabelIfPresent}, confirmed against a mod already built and
 * shipped for this exact Minecraft version and Yarn mapping build.
 * <p>
 * Purely additive: it only draws an extra quad next to the local player's own
 * name and never modifies the {@link Text} object itself, so it can't be
 * mistaken for changing, hiding, or spoofing anyone's actual name.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class MixinPlayerEntityRenderer {

    private static final Identifier BADGE_TEXTURE = Identifier.of(XClientMod.RESOURCE_NAMESPACE, "textures/icons/logo.png");
    private static final Identifier WHITE_TEXTURE = Identifier.ofVanilla("textures/misc/white.png");

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"))
    private void xclient$renderBadge(PlayerEntityRenderState state, MatrixStack matrices,
                                      OrderedRenderCommandQueue queue, CameraRenderState cameraRenderState,
                                      CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !xclient$isCurrentPlayer(state, client)) return;

        boolean iconEnabled = XClientMod.getModuleManager() != null
                && XClientMod.getModuleManager().getModule(XClientIcon.class).map(m -> m.isEnabled()).orElse(false);
        if (!iconEnabled) return;

        Text text = state.displayName != null ? state.displayName : state.playerName;
        if (text == null) return;

        int textWidth = client.textRenderer.getWidth(text);
        float badgeSize = 10.0F;
        float yOffset = state.height + 0.5F;

        matrices.push();
        try {
            matrices.translate(0.0, yOffset, 0.0);
            matrices.multiply(client.gameRenderer.getCamera().getRotation());
            matrices.scale(-0.025F, -0.025F, 0.025F);

            float badgeX = textWidth / 2.0F + 1.0F;
            float badgeY = -badgeSize / 10.0F;

            xclient$renderBadgeIcon(matrices, queue, client, state.light, badgeX, badgeY, badgeSize);
        } finally {
            matrices.pop();
        }
    }

    private void xclient$renderBadgeIcon(MatrixStack matrices, OrderedRenderCommandQueue queue, MinecraftClient client,
                                          int light, float x, float y, float size) {
        int overlay = OverlayTexture.DEFAULT_UV;
        int nametagBackgroundColor = xclient$nameTagBackgroundColor(client);

        xclient$renderQuad(matrices, queue, RenderLayers.entityTranslucent(WHITE_TEXTURE),
                light, overlay, x, y, size, -0.02F, nametagBackgroundColor);

        xclient$renderQuad(matrices, queue, RenderLayers.entityTranslucent(BADGE_TEXTURE),
                light, overlay, x, y, size, 0.0F, 0xFFFFFFFF);
    }

    private void xclient$renderQuad(MatrixStack matrices, OrderedRenderCommandQueue queue, RenderLayer layer,
                                     int light, int overlay, float x, float y, float size, float z, int argbColor) {
        queue.submitCustom(matrices, layer, (entry, buffer) -> {
            Matrix4f matrix = entry.getPositionMatrix();

            buffer.vertex(matrix, x, y + size, z)
                    .color(argbColor).texture(1, 1).overlay(overlay).light(light).normal(0, 0, 1);
            buffer.vertex(matrix, x + size, y + size, z)
                    .color(argbColor).texture(0, 1).overlay(overlay).light(light).normal(0, 0, 1);
            buffer.vertex(matrix, x + size, y, z)
                    .color(argbColor).texture(0, 0).overlay(overlay).light(light).normal(0, 0, 1);
            buffer.vertex(matrix, x, y, z)
                    .color(argbColor).texture(1, 0).overlay(overlay).light(light).normal(0, 0, 1);
        });
    }

    private boolean xclient$isCurrentPlayer(PlayerEntityRenderState playerState, MinecraftClient client) {
        if (client.player == null) return false;
        if (playerState.id == client.player.getId()) return true;

        var cameraEntity = client.getCameraEntity();
        return cameraEntity != null && playerState.id == cameraEntity.getId();
    }

    private int xclient$nameTagBackgroundColor(MinecraftClient client) {
        float opacity = client.options.getTextBackgroundOpacity(0.25F);
        int alpha = (int) (opacity * 255.0F) & 0xFF;
        return alpha << 24;
    }
}
