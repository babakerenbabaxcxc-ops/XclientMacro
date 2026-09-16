package ridzzxmc.xclient.core.keybind;

import net.minecraft.client.MinecraftClient;
import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.InputEvent;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleManager;

public class KeybindManager {

    private final ModuleManager moduleManager;

    public KeybindManager(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    @SubscribeEvent
    public void onInput(InputEvent event) {
        if (event.isMouse() || event.getAction() != InputEvent.Action.PRESS) return;

        // The mixin feeding this event observes every raw key press application-wide,
        // including while typing in chat, the ClickGUI search box, a sign, an anvil
        // rename field, etc. Without this guard, a module bound to a common letter key
        // would toggle itself on every matching keystroke while the player is just
        // typing. Vanilla keybinds don't fire while a screen is open either, so this
        // matches expected behavior: module keybinds only work during normal gameplay.
        if (MinecraftClient.getInstance().currentScreen != null) return;

        for (Module module : moduleManager.getModules()) {
            Keybind bind = module.getKeybind();
            if (bind.isBound() && bind.getKey() == event.getKey()) {
                module.toggle();
            }
        }
    }
}
