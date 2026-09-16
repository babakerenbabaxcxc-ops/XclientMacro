package com.example.xclientaddon.macro;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.TickEvent;
import ridzzxmc.xclient.core.module.ModuleCategory;

/** One-click spear use followed by an optional switch to a configured food slot. */
public class SpearOneClickModule extends HotbarMacroModule {
    public SpearOneClickModule() {
        super("Spear One-Click", "Use spear once, then swap to configured food slot", ModuleCategory.WORLD);
        ridzzxmc.xclient.core.event.EventBus.INSTANCE.subscribe(this);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (!isEnabled() || event.getPhase() != TickEvent.Phase.END || !ready()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        int spear = slot(primarySlot);
        int food = slot(secondarySlot);
        if (!hasItem(spear, Items.SPEAR)) { finish(); return; }
        select(spear);
        mc.doItemUse();
        select(food);
        finish();
    }
}
