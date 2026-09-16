package com.example.xclientaddon.macro;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.TickEvent;
import ridzzxmc.xclient.core.module.ModuleCategory;

/** Performs a sword hit, then swaps to the configured mace slot and attacks once. */
public class SwordMaceSwapModule extends HotbarMacroModule {
    public SwordMaceSwapModule() {
        super("Sword -> Mace Sweep", "One-key sword hit then mace swap", ModuleCategory.WORLD);
        ridzzxmc.xclient.core.event.EventBus.INSTANCE.subscribe(this);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (!isEnabled() || event.getPhase() != TickEvent.Phase.END || !ready()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!(mc.crosshairTarget instanceof EntityHitResult ehr) || mc.crosshairTarget.getType() != HitResult.Type.ENTITY) { finish(); return; }
        Entity target = ehr.getEntity();
        int sword = slot(primarySlot);
        int mace = slot(secondarySlot);
        if (!hasItem(mace, Items.MACE)) { finish(); return; }

        select(sword);
        if (!mc.player.getMainHandStack().isEmpty()) mc.interactionManager.attackEntity(mc.player, target);
        select(mace);
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        finish();
    }
}
