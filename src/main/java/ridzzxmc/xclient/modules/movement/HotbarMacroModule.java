package com.example.xclientaddon.macro;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.settings.SliderSetting;

/** Base helper for one-shot hotbar macros. Keybind is the trigger; the module
 * executes once and disables itself again. */
public abstract class HotbarMacroModule extends Module {
    protected final SliderSetting primarySlot = register(new SliderSetting("Primary Slot", 1, 9, 1, 1, true));
    protected final SliderSetting secondarySlot = register(new SliderSetting("Secondary Slot", 1, 9, 2, 1, true));

    protected HotbarMacroModule(String name, String description, ridzzxmc.xclient.core.module.ModuleCategory category) {
        super(name, description, category);
    }

    protected int slot(SliderSetting setting) { return ((Number) setting.getValue()).intValue() - 1; }

    protected boolean ready() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null && mc.world != null && mc.interactionManager != null && mc.currentScreen == null;
    }

    protected boolean hasItem(int slot, Item item) {
        ItemStack stack = MinecraftClient.getInstance().player.getInventory().getStack(slot);
        return stack.isOf(item);
    }

    protected void select(int slot) { MinecraftClient.getInstance().player.getInventory().setSelectedSlot(slot); }

    protected void finish() { setEnabled(false); }
}
