package ridzzxmc.xclient.feature;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.Generic3x3ContainerScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import ridzzxmc.xclient.mixin.HandledScreenAccessor;

/**
 * Adds "Take All" / "Store All" / "Sort" buttons directly to chest-like
 * container screens, and a standalone "Sort" button to your own inventory
 * screen when no container is open.
 * <p>
 * Deliberately NOT a toggleable {@link ridzzxmc.xclient.core.module.Module} -
 * these read as the kind of always-there quality-of-life buttons vanilla
 * arguably should ship with, not something anyone would want a keybind to
 * disable. So this is wired up once from {@code XClientMod.onInitializeClient()}
 * via {@link #register()} instead of going through the ClickGUI/Module system.
 * <p>
 * Every button works purely by replaying the same slot-click packets a
 * player's mouse already sends - shift-click for "move the whole stack to the
 * other side", plain left-click pick-up/place for merging and compacting.
 * {@code ClientPlayerInteractionManager.clickSlot(...)} is the exact call
 * vanilla's own mouse handling uses, so the server-side {@code ScreenHandler}
 * validates every step exactly like a real click - there's nothing here a
 * fast player couldn't already do by hand, just done instantly.
 */
public final class InventoryQuickActions {

    private static final int BUTTON_WIDTH = 50;
    private static final int BUTTON_HEIGHT = 14;
    private static final int GAP = 2;
    private static final int MARGIN = 4;

    private InventoryQuickActions() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof HandledScreen<?> handledScreen)) return;

            var accessor = (HandledScreenAccessor) handledScreen;
            ScreenHandler handler = handledScreen.getScreenHandler();

            if (isSupportedContainer(handler)) {
                addContainerButtons(client, handledScreen, accessor, handler);
            } else if (screen instanceof InventoryScreen) {
                addInventoryOnlyButton(client, handledScreen, accessor, handler);
            }
        });
    }

    private static boolean isSupportedContainer(ScreenHandler handler) {
        return handler instanceof GenericContainerScreenHandler   // chest, double chest, ender chest, minecart chest
                || handler instanceof ShulkerBoxScreenHandler
                || handler instanceof HopperScreenHandler
                || handler instanceof Generic3x3ContainerScreenHandler; // dispenser, dropper
    }

    private static void addContainerButtons(MinecraftClient client, HandledScreen<?> screen,
                                             HandledScreenAccessor accessor, ScreenHandler handler) {
        // The player's own inventory (27 main + 9 hotbar) is always the last 36 slots
        // ScreenHandler adds, regardless of which container type comes before it.
        int containerSize = handler.slots.size() - 36;
        if (containerSize <= 0) return;

        int[] pos = layout(accessor, 3);

        Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("Take All"),
                        btn -> quickMoveRange(client, handler, 0, containerSize))
                .dimensions(pos[0], pos[1], BUTTON_WIDTH, BUTTON_HEIGHT).build());

        Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("Store All"),
                        btn -> quickMoveRange(client, handler, containerSize, handler.slots.size()))
                .dimensions(pos[0], pos[1] + (BUTTON_HEIGHT + GAP), BUTTON_WIDTH, BUTTON_HEIGHT).build());

        Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("Sort"),
                        btn -> {
                            sortRange(client, handler, 0, containerSize);
                            sortRange(client, handler, containerSize, handler.slots.size());
                        })
                .dimensions(pos[0], pos[1] + (BUTTON_HEIGHT + GAP) * 2, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private static void addInventoryOnlyButton(MinecraftClient client, HandledScreen<?> screen,
                                                HandledScreenAccessor accessor, ScreenHandler handler) {
        int[] pos = layout(accessor, 1);
        Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("Sort"),
                        btn -> sortRange(client, handler, 0, handler.slots.size()))
                .dimensions(pos[0], pos[1], BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    /**
     * @return {x, topY} for a right-aligned, top-anchored stack of {@code buttonCount}
     * buttons - placed just above the GUI background if there's room there, otherwise
     * to its right, so the buttons never end up covering the actual item slots.
     */
    private static int[] layout(HandledScreenAccessor accessor, int buttonCount) {
        int guiX = accessor.xclient$getGuiX();
        int guiY = accessor.xclient$getGuiY();
        int guiWidth = accessor.xclient$getBackgroundWidth();
        int stackHeight = buttonCount * BUTTON_HEIGHT + (buttonCount - 1) * GAP;

        if (guiY - stackHeight - MARGIN >= 0) {
            return new int[]{guiX + guiWidth - BUTTON_WIDTH, guiY - stackHeight - MARGIN};
        }
        return new int[]{guiX + guiWidth + MARGIN, guiY};
    }

    /**
     * Shift-clicks every occupied slot in [start, end) - the same one-click
     * "move the whole stack to the other side" a player gets from
     * shift-clicking each slot by hand.
     */
    private static void quickMoveRange(MinecraftClient client, ScreenHandler handler, int start, int end) {
        if (client.player == null) return;
        for (int i = start; i < end; i++) {
            if (handler.getSlot(i).hasStack()) {
                client.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, client.player);
            }
        }
    }

    /** Merges same-item stacks together, then pushes everything toward the front of the range to close up any gaps left behind. */
    private static void sortRange(MinecraftClient client, ScreenHandler handler, int start, int end) {
        if (client.player == null) return;
        mergeDuplicates(client, handler, start, end);
        compact(client, handler, start, end);
    }

    /**
     * For each pair of same-item slots, replays "pick up the later stack, left-click
     * the earlier one to merge into it, and if there's overflow left on the cursor,
     * left-click the (now empty) later slot again to drop it back" - exactly what
     * merging two stacks by hand looks like, just automated.
     */
    private static void mergeDuplicates(MinecraftClient client, ScreenHandler handler, int start, int end) {
        for (int i = start; i < end; i++) {
            Slot slotI = handler.getSlot(i);
            if (!slotI.hasStack() || slotI.getStack().getCount() >= slotI.getStack().getMaxCount()) continue;

            for (int j = i + 1; j < end; j++) {
                if (!slotI.hasStack() || slotI.getStack().getCount() >= slotI.getStack().getMaxCount()) break;
                Slot slotJ = handler.getSlot(j);
                if (!slotJ.hasStack() || !ItemStack.areItemsAndComponentsEqual(slotI.getStack(), slotJ.getStack())) continue;

                click(client, handler, j, SlotActionType.PICKUP);
                click(client, handler, i, SlotActionType.PICKUP);
                if (!handler.getCursorStack().isEmpty()) {
                    click(client, handler, j, SlotActionType.PICKUP); // leftover from an overfull merge goes back where it came from
                }
            }
        }
    }

    /** Standard "shift non-null entries to the front" compaction, translated into pick-up/place clicks on empty slots. */
    private static void compact(MinecraftClient client, ScreenHandler handler, int start, int end) {
        int writeIndex = start;
        for (int readIndex = start; readIndex < end; readIndex++) {
            if (!handler.getSlot(readIndex).hasStack()) continue;
            if (readIndex != writeIndex) {
                click(client, handler, readIndex, SlotActionType.PICKUP);
                click(client, handler, writeIndex, SlotActionType.PICKUP);
            }
            writeIndex++;
        }
    }

    private static void click(MinecraftClient client, ScreenHandler handler, int slotIndex, SlotActionType type) {
        client.interactionManager.clickSlot(handler.syncId, slotIndex, 0, type, client.player);
    }
}
