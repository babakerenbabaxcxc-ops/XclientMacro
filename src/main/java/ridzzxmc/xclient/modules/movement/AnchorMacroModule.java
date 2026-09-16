package com.example.xclientaddon.macro;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.TickEvent;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.BooleanSetting;

/** One-key respawn-anchor sequence: place -> charge -> detonate.
 * The server still validates every interaction. */
public class AnchorMacroModule extends HotbarMacroModule {
    private final BooleanSetting autoCharge = register(new BooleanSetting("Auto Charge", true));
    private final BooleanSetting autoDetonate = register(new BooleanSetting("Auto Detonate", true));

    public AnchorMacroModule() {
        super("Anchor Macro", "One-key anchor place/charge/detonate sequence", ModuleCategory.WORLD);
        ridzzxmc.xclient.core.event.EventBus.INSTANCE.subscribe(this);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (!isEnabled() || event.getPhase() != TickEvent.Phase.END || !ready()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p.getMainHandStack().isEmpty()) { finish(); return; }
        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) { finish(); return; }

        int anchor = slot(primarySlot);
        int glow = slot(secondarySlot);
        if (!hasItem(anchor, Items.RESPAWN_ANCHOR)) { finish(); return; }
        select(anchor);
        mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, hit);

        if (autoCharge.getValue()) {
            if (hasItem(glow, Items.GLOWSTONE)) {
                select(glow);
                mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, hit);
            }
        }

        if (autoDetonate.getValue()) {
            // Use the anchor again after charging. On normal dimensions this is
            // the vanilla explosion interaction; the server remains authoritative.
            select(anchor);
            mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, hit);
        }
        finish();
    }
}
