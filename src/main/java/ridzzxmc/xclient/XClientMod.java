package ridzzxmc.xclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import ridzzxmc.xclient.core.config.ConfigManager;
import ridzzxmc.xclient.core.event.EventBus;
import ridzzxmc.xclient.core.event.events.ConnectionEvent;
import ridzzxmc.xclient.core.event.events.RenderEvent;
import ridzzxmc.xclient.core.event.events.TickEvent;
import ridzzxmc.xclient.core.keybind.KeybindManager;
import ridzzxmc.xclient.core.module.ModuleManager;
import ridzzxmc.xclient.core.notification.NotificationManager;
import ridzzxmc.xclient.gui.clickgui.ClickGUIScreen;
import org.lwjgl.glfw.GLFW;

/**
 * Client entrypoint. Responsible only for:
 *  1. Building the module registry and config manager.
 *  2. Bridging Fabric API's native callbacks into our own {@link EventBus},
 *     so every module only ever has to know about {@code TickEvent} /
 *     {@code RenderEvent} / etc, not Fabric API directly.
 *  3. Registering the ClickGUI open keybind.
 */
public class XClientMod implements ClientModInitializer {

    public static final String MOD_ID = "x-client";

    /**
     * Resource/asset namespace - deliberately NOT the same as {@link #MOD_ID}.
     * Every texture in this project lives under {@code assets/xclient/...}
     * (see {@code fabric.mod.json}'s {@code "icon"} entry), not
     * {@code assets/x-client/...}, so any {@code Identifier.of(...)} call for
     * a texture must use this constant rather than {@link #MOD_ID}.
     */
    public static final String RESOURCE_NAMESPACE = "xclient";

    private static ModuleManager moduleManager;
    private static ConfigManager configManager;
    private static KeybindManager keybindManager;

    /** GLFW key that opens the ClickGUI - Right Shift by default, matches Feather Client's convention. */
    private static int clickGuiKey = GLFW.GLFW_KEY_RIGHT_SHIFT;

    private static boolean clickGuiKeyWasDown = false;

    /** Last server address we successfully joined, for {@code AutoReconnect}. See {@link #registerLifecycleBridge()}. */
    private static String lastServerAddress = null;

    @Override
    public void onInitializeClient() {
        moduleManager = new ModuleManager();
        configManager = new ConfigManager();
        keybindManager = new KeybindManager(moduleManager);

        moduleManager.registerAll();
        EventBus.INSTANCE.subscribe(keybindManager);
        EventBus.INSTANCE.subscribe(new ridzzxmc.xclient.gui.hud.ToastRenderer());
        configManager.load(moduleManager);

        registerTickBridge();
        registerRenderBridge();
        registerLifecycleBridge();
        registerGuiKeybind();
        ridzzxmc.xclient.feature.InventoryQuickActions.register();

        NotificationManager.INSTANCE.push("X Client", "Loaded successfully.",
                ridzzxmc.xclient.core.notification.NotificationType.SUCCESS);
    }

    private void registerTickBridge() {
        ClientTickEvents.START_CLIENT_TICK.register(client ->
                EventBus.INSTANCE.post(new TickEvent(TickEvent.Phase.START)));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            EventBus.INSTANCE.post(new TickEvent(TickEvent.Phase.END));
            NotificationManager.INSTANCE.tick();
        });
    }

    private void registerRenderBridge() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) ->
                EventBus.INSTANCE.post(new RenderEvent(RenderEvent.Type.HUD_2D, drawContext, tickDelta.getTickDelta(true))));

        // NOTE: A WORLD_3D bridge previously lived here, wired to
        // net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents#AFTER_TRANSLUCENT.
        // That flat package/class no longer exists in current Fabric API - it was removed
        // during Minecraft's rendering-pipeline rewrite and only reintroduced starting with
        // Fabric API for Minecraft 1.21.10, under a NEW package:
        //   net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
        //   net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
        // Referencing the old (nonexistent) class is exactly what threw the
        // NoClassDefFoundError/ClassNotFoundException that crashed the client on init.
        // No module currently subscribes to RenderEvent.Type.WORLD_3D, so the bridge was
        // simply dead code and has been removed rather than ported. If you add a module that
        // needs to draw in the world later, register against the new WorldRenderEvents class
        // above (see https://docs.fabricmc.net/1.21.11/develop/rendering/world) and post a
        // RenderEvent(RenderEvent.Type.WORLD_3D, ...) from there.
    }

    private void registerLifecycleBridge() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Recorded here (rather than read lazily off mc.getCurrentServerEntry() at
            // disconnect time) because that entry is already cleared by the time
            // DISCONNECT fires - Auto Reconnect needs the address to still be around then.
            lastServerAddress = client.getCurrentServerEntry() != null ? client.getCurrentServerEntry().address : null;

            NotificationManager.INSTANCE.push("Connected", "Joined the server.",
                    ridzzxmc.xclient.core.notification.NotificationType.INFO);
            EventBus.INSTANCE.post(new ConnectionEvent(ConnectionEvent.Phase.JOIN, lastServerAddress));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            configManager.save(moduleManager);
            EventBus.INSTANCE.post(new ConnectionEvent(ConnectionEvent.Phase.DISCONNECT, lastServerAddress));
        });
    }

    private void registerGuiKeybind() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long handle = client.getWindow().getHandle();
            boolean down = GLFW.glfwGetKey(handle, clickGuiKey) == GLFW.GLFW_PRESS;
            // Only act on the rising edge (just pressed), not on "currently held" - polling
            // held-state every tick meant closing the menu while still physically holding
            // the key (e.g. pressing Escape without releasing Right Shift) would see the
            // key as still down and immediately reopen the menu the very next tick.
            if (down && !clickGuiKeyWasDown && client.currentScreen == null) {
                MinecraftClient.getInstance().setScreen(new ClickGUIScreen());
            }
            clickGuiKeyWasDown = down;
        });
    }

    public static ModuleManager getModuleManager() {
        return moduleManager;
    }

    public static ConfigManager getConfigManager() {
        return configManager;
    }

    public static void setClickGuiKey(int key) {
        clickGuiKey = key;
    }

    /**
     * Call this when a different screen (e.g. the HUD editor) closes itself on
     * a Right Shift press. Without it, the very next tick would see that same
     * physical key still down, treat it as a fresh press (since this class's
     * own edge-tracker never saw it), and immediately reopen the ClickGUI
     * right after the other screen closed.
     */
    public static void suppressNextGuiKeyEdge() {
        clickGuiKeyWasDown = true;
    }
}
