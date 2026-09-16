package ridzzxmc.xclient.modules.misc;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.ConnectionEvent;
import ridzzxmc.xclient.core.event.events.TickEvent;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.notification.NotificationManager;
import ridzzxmc.xclient.core.notification.NotificationType;
import ridzzxmc.xclient.core.settings.ModeSetting;
import ridzzxmc.xclient.core.settings.SliderSetting;
import ridzzxmc.xclient.core.settings.TextSetting;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Submits your own saved password automatically a couple seconds after
 * joining a server, so you don't have to type {@code /login ...} by hand
 * every time on servers that gate movement/chat behind an auth plugin
 * (AuthMe and friends). This only ever acts on the account you're already
 * logged into with credentials you supply yourself - it doesn't touch
 * anyone else's account and gives no advantage over other players, so it's
 * a different category of thing entirely from an ESP/wallhack.
 * <p>
 * <b>Security note:</b> passwords are saved in plain text in
 * {@code config/xclient/autologin.json} on this machine, the same tradeoff
 * other clients with this feature (e.g. Feather, LabyMod) make. Don't use it
 * for a password you reuse anywhere sensitive, and don't share your config
 * folder.
 */
public class AutoLogin extends Module {

    private final ModeSetting command = register(new ModeSetting("Command", List.of("login", "l"), "login"));
    private final SliderSetting delaySeconds = register(new SliderSetting("Delay (s)", 2, 1, 10, 1, true));
    /**
     * Simple single-password path: type it straight into the ClickGUI settings
     * panel (no command needed) and it applies to whatever server you're on
     * when Auto Login fires. The {@code /autologin set} command below is for
     * the multi-server case this single field can't represent - it always
     * wins over this field for a server it has an entry for.
     */
    private final TextSetting password = register(new TextSetting("Password", "", 64, true));

    private final Path saveFile = FabricLoader.getInstance().getConfigDir().resolve("xclient").resolve("autologin.json");
    private final com.google.gson.Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, String> credentials = new HashMap<>();

    private String pendingAddress;
    private int ticksRemaining = -1;

    public AutoLogin() {
        super("Auto Login", "Sends your saved password after joining a server. Set one with /autologin set <password>.",
                ModuleCategory.MISC, true);
        load();
        registerCommands();
    }

    // ---- persistence ----

    private void load() {
        if (!Files.exists(saveFile)) return;
        try {
            String content = Files.readString(saveFile, StandardCharsets.UTF_8);
            Type mapType = new TypeToken<HashMap<String, String>>() {}.getType();
            Map<String, String> loaded = gson.fromJson(content, mapType);
            if (loaded != null) credentials.putAll(loaded);
        } catch (Exception e) {
            System.err.println("[X Client] Failed to load autologin.json, starting empty:");
            e.printStackTrace();
        }
    }

    private void save() {
        try {
            Files.createDirectories(saveFile.getParent());
            Files.writeString(saveFile, gson.toJson(credentials), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[X Client] Failed to save autologin.json:");
            e.printStackTrace();
        }
    }

    // ---- commands ----

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("autologin")
                        .executes(ctx -> {
                            ctx.getSource().sendFeedback(Text.literal(
                                    "§7Usage: /autologin set <password> | remove | list"));
                            return 1;
                        })
                        .then(ClientCommandManager.literal("set")
                                .then(ClientCommandManager.argument("password", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String address = currentAddress();
                                            if (address == null) {
                                                ctx.getSource().sendError(Text.literal("Not connected to a server."));
                                                return 0;
                                            }
                                            credentials.put(address, StringArgumentType.getString(ctx, "password"));
                                            save();
                                            ctx.getSource().sendFeedback(Text.literal(
                                                    "§aSaved a password for " + address + ". Stored in plain text locally."));
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("remove")
                                .executes(ctx -> {
                                    String address = currentAddress();
                                    if (address != null && credentials.remove(address) != null) {
                                        save();
                                        ctx.getSource().sendFeedback(Text.literal("§aRemoved saved password for " + address + "."));
                                    } else {
                                        ctx.getSource().sendError(Text.literal("No saved password for this server."));
                                    }
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("list")
                                .executes(ctx -> {
                                    if (credentials.isEmpty()) {
                                        ctx.getSource().sendFeedback(Text.literal("§7No saved passwords."));
                                        return 1;
                                    }
                                    ctx.getSource().sendFeedback(Text.literal("§7Servers with a saved password:"));
                                    for (String address : credentials.keySet()) {
                                        ctx.getSource().sendFeedback(Text.literal("§7- §f" + address));
                                    }
                                    return 1;
                                }))));
    }

    private String currentAddress() {
        return mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : null;
    }

    // ---- auto-send on join ----

    @SubscribeEvent
    public void onConnectionEvent(ConnectionEvent event) {
        if (!isEnabled() || event.getPhase() != ConnectionEvent.Phase.JOIN) return;
        if (event.getServerAddress() == null) return;

        boolean hasPerServer = credentials.containsKey(event.getServerAddress());
        boolean hasGlobal = !password.getValue().isEmpty();
        if (!hasPerServer && !hasGlobal) return;

        pendingAddress = event.getServerAddress();
        ticksRemaining = delaySeconds.getValue().intValue() * 20;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (event.getPhase() != TickEvent.Phase.END || ticksRemaining < 0) return;
        if (mc.player == null) return; // wait until we're actually placed in the world

        if (--ticksRemaining > 0) return;
        ticksRemaining = -1;

        // Per-server saved password (via /autologin set) wins if present, otherwise
        // fall back to the single GUI-entered password field - see its javadoc.
        String resolvedPassword = credentials.getOrDefault(pendingAddress, password.getValue());
        if (resolvedPassword == null || resolvedPassword.isEmpty()) return;

        mc.player.networkHandler.sendChatCommand(command.getValue() + " " + resolvedPassword);
        NotificationManager.INSTANCE.push("Auto Login", "Sent saved password.", NotificationType.INFO);
    }
}
