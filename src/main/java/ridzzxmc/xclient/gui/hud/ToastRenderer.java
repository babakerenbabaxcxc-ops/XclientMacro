package ridzzxmc.xclient.gui.hud;

import net.minecraft.client.MinecraftClient;
import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.RenderEvent;
import ridzzxmc.xclient.core.notification.Notification;
import ridzzxmc.xclient.core.notification.NotificationManager;
import ridzzxmc.xclient.gui.theme.XTheme;
import ridzzxmc.xclient.util.Easing;
import ridzzxmc.xclient.util.RenderUtil;

/**
 * Renders the active {@link Notification} queue as toast cards sliding in
 * from the right, top-right corner, fading per {@link Notification#getOpacity()}.
 * Subscribed once in {@code XClientMod.onInitializeClient}.
 */
public class ToastRenderer {

    private static final int TOAST_WIDTH = 220;
    private static final int TOAST_HEIGHT = 40;
    private static final int GAP = 8;

    @SubscribeEvent
    public void onRender(RenderEvent event) {
        if (event.getType() != RenderEvent.Type.HUD_2D) return;

        var mc = MinecraftClient.getInstance();
        var ctx = event.getDrawContext();
        int screenWidth = mc.getWindow().getScaledWidth();

        int y = 8;
        for (Notification notification : NotificationManager.INSTANCE.getActive()) {
            float opacity = notification.getOpacity();
            float slideT = (float) Easing.easeInOutCubic(Math.min(1.0, notification.getAge() / 200.0));
            int x = screenWidth - TOAST_WIDTH - 8 + Math.round((1 - slideT) * 40);

            int alpha = Math.round(255 * opacity);
            RenderUtil.dropShadow(ctx, x, y, TOAST_WIDTH, TOAST_HEIGHT, 8);
            RenderUtil.roundedRect(ctx, x, y, TOAST_WIDTH, TOAST_HEIGHT, 8, RenderUtil.withAlpha(XTheme.CARD_BG, Math.min(238, alpha)));
            RenderUtil.roundedRect(ctx, x, y, 4, TOAST_HEIGHT, 0, RenderUtil.withAlpha(notification.getType().getColor(), alpha));

            ctx.drawText(mc.textRenderer, notification.getTitle(), x + 12, y + 8,
                    RenderUtil.withAlpha(XTheme.TEXT_PRIMARY, alpha), true);
            ctx.drawText(mc.textRenderer, notification.getMessage(), x + 12, y + 20,
                    RenderUtil.withAlpha(XTheme.TEXT_SECONDARY, alpha), false);

            y += TOAST_HEIGHT + GAP;
        }
    }
}
