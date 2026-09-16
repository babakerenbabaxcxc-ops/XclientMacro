package ridzzxmc.xclient.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.input.KeyInput;
import ridzzxmc.xclient.core.event.EventBus;
import ridzzxmc.xclient.core.event.events.InputEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bridges GLFW key callbacks into {@link InputEvent}, so modules like
 * {@code KeybindManager} never touch GLFW directly. Fires alongside vanilla
 * key handling - it observes only, never cancels or rewrites input.
 *
 * As of 1.21.11, Keyboard#onKey no longer takes separate (key, scancode, mods)
 * ints - key + scancode + modifiers were merged into a single KeyInput record,
 * leaving (window, action, KeyInput) as the real parameters. This is why the
 * mixin was failing to apply with an "Invalid descriptor" error (same change
 * that MixinMouse already accounts for on the mouse side).
 */
@Mixin(Keyboard.class)
public class MixinKeyboard {

    @Inject(method = "onKey", at = @At("HEAD"))
    private void xclient$onKey(long window, int action, KeyInput input, CallbackInfo ci) {
        InputEvent.Action mapped = switch (action) {
            case GLFW.GLFW_PRESS -> InputEvent.Action.PRESS;
            case GLFW.GLFW_RELEASE -> InputEvent.Action.RELEASE;
            default -> InputEvent.Action.REPEAT;
        };
        EventBus.INSTANCE.post(new InputEvent(input.key(), mapped, false));
    }
}
