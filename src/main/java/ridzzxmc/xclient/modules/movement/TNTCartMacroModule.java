package com.example.xclientaddon.macro;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.TickEvent;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.SliderSetting;

/** Places a rail on the selected surface, then places a TNT minecart on it. */
public class TNTCartMacroModule extends HotbarMacroModule {
    private final SliderSetting cartSlot = register(new SliderSetting("TNT Cart Slot", 1, 9, 3, 1, true));

    public TNTCartMacroModule() {
        super("TNT Cart Macro", "One-key rail + TNT minecart placement", ModuleCategory.WORLD);
        ridzzxmc.xclient.core.event.EventBus.INSTANCE.subscribe(this);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (!isEnabled() || event.getPhase() != TickEvent.Phase.END || !ready()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) { finish(); return; }

        int rail = slot(primarySlot);
        int cart = ((Number) cartSlot.getValue()).intValue() - 1;
        if (!hasItem(rail, Items.RAIL) || !hasItem(cart, Items.TNT_MINECART)) { finish(); return; }

        BlockPos railPos = hit.getBlockPos().offset(hit.getSide());
        select(rail);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);

        // Target the top face of the newly placed rail for the minecart.
        BlockHitResult railHit = new BlockHitResult(
                Vec3d.ofCenter(railPos).add(0.0, 0.5, 0.0), Direction.UP, railPos, false);
        select(cart);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, railHit);
        finish();
    }
}
