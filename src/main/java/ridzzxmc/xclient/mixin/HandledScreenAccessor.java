package ridzzxmc.xclient.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * {@code HandledScreen}'s {@code x}/{@code y}/{@code backgroundWidth}/
 * {@code backgroundHeight} fields (the GUI texture's on-screen position and
 * size) are protected, so {@code InventoryQuickActions} needs this accessor
 * to position its buttons relative to a container screen it doesn't own.
 * These four field names have been unchanged since the inventory GUI was
 * last restructured many versions ago - about as low-risk as a mixin target gets.
 */
@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {

    @Accessor("x")
    int xclient$getGuiX();

    @Accessor("y")
    int xclient$getGuiY();

    @Accessor("backgroundWidth")
    int xclient$getBackgroundWidth();

    @Accessor("backgroundHeight")
    int xclient$getBackgroundHeight();
}
