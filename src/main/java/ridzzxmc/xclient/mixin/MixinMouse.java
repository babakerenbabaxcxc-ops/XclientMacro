package ridzzxmc.xclient.mixin;

import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import ridzzxmc.xclient.core.event.EventBus;
import ridzzxmc.xclient.core.event.events.InputEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MixinMouse {

    // As of 1.21.11, Mouse#onMouseButton no longer takes separate (button, action, mods)
    // ints - button + mods were merged into a single MouseInput record, leaving
    // (window, MouseInput, action) as the three real parameters. This is why the mixin
    // was failing to apply with an "Invalid descriptor" error.
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void xclient$onMouseButton(long window, MouseInput input, int action, CallbackInfo ci) {
        InputEvent.Action mapped = action == GLFW.GLFW_PRESS ? InputEvent.Action.PRESS : InputEvent.Action.RELEASE;
        EventBus.INSTANCE.post(new InputEvent(input.button(), mapped, true));
    }
}
