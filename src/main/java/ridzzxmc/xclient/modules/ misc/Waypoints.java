package ridzzxmc.xclient.modules.misc;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import ridzzxmc.xclient.core.event.SubscribeEvent;
import ridzzxmc.xclient.core.event.events.RenderEvent;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.notification.NotificationManager;
import ridzzxmc.xclient.core.notification.NotificationType;
import ridzzxmc.xclient.core.settings.BooleanSetting;
import ridzzxmc.xclient.core.settings.SliderSetting;
import ridzzxmc.xclient.core.settings.TextSetting;
import ridzzxmc.xclient.gui.theme.XTheme;
import ridzzxmc.xclient.util.RenderUtil;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Named, persistent waypoints: {@code /waypoint add <name>} saves your
 * current position, and a small panel in the top-right (while this module
 * is enabled) shows the nearest ones as an 8-direction arrow + distance,
 * e.g. "↗ Base  128m".
 * <p>
 * Deliberately HUD-only rather than a true in-world beam/ESP: the old
 * {@code WORLD_3D} render bridge in {@code XClientMod} was removed (see the
 * comment in {@code registerRenderBridge()}) because the Fabric API class it
 * depended on moved packages for 1.21.10+, and no replacement is currently
 * wired up. Everything here only ever needs {@code RenderEvent.Type.HUD_2D},
 * which is still bridged, so it works today without depending on that gap
 * being closed.
 * <p>
 * Waypoints live in their own {@code config/xclient/waypoints.json}, separate
 * from {@code ConfigManager}'s {@code config.json} - that file only knows how
 * to (de)serialize {@link ridzzxmc.xclient.core.settings.Setting} values, and
 * teaching it a one-off list-of-records shape for a single module isn't worth
 * the coupling when this module can just own its own small file instead.
 */
public class Waypoints extends Module {

    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    private final BooleanSetting currentDimensionOnly = register(new BooleanSetting("Current Dimension Only", true));
    private final BooleanSetting showDistance = register(new BooleanSetting("Show Distance", true));
    private final SliderSetting maxShown = register(new SliderSetting("Max Shown", 5, 1, 10, 1, true));
    private final SliderSetting maxRange = register(new SliderSetting("Max Range", 5000, 100, 20000, 100, true));
    /**
     * GUI alternative to {@code /waypoint add <name>}: type a name and hit
     * Enter in the settings panel, and {@link #onChange} fires immediately
     * with the confirmed text, saving a waypoint at your current position and
     * then clearing the field back to empty (that second {@code setValue("")}
     * re-enters this same callback, which the blank-check below simply no-ops
     * on rather than adding an empty-named waypoint).
     */
    private final TextSetting newWaypointName = register(new TextSetting("New Waypoint (Enter)", "", 32));

    private final Path saveFile = FabricLoader.getInstance().getConfigDir().resolve("xclient").resolve("waypoints.json");
    private final com.google.gson.Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final List<Entry> waypoints = new ArrayList<>();

    public Waypoints() {
        super("Waypoints", "Save named locations with /waypoint add <name> (or the settings panel field) and see a direction + distance panel for them.",
                ModuleCategory.MISC, true);
        load();
        registerCommands();
        newWaypointName.onChange(name -> {
            if (name == null || name.isBlank()) return;
            addWaypoint(name.trim());
            newWaypointName.setValue("");
        });
    }

    // ---- persistence ----

    private void load() {
        if (!Files.exists(saveFile)) return;
        try {
            String content = Files.readString(saveFile, StandardCharsets.UTF_8);
            Type listType = new TypeToken<ArrayList<Entry>>() {}.getType();
            List<Entry> loaded = gson.fromJson(content, listType);
            if (loaded != null) waypoints.addAll(loaded);
        } catch (Exception e) {
            System.err.println("[X Client] Failed to load waypoints.json, starting empty:");
            e.printStackTrace();
        }
    }

    private void save() {
        try {
            Files.createDirectories(saveFile.getParent());
            Files.writeString(saveFile, gson.toJson(waypoints), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[X Client] Failed to save waypoints.json:");
            e.printStackTrace();
        }
    }

    // ---- commands ----

    /**
     * Registers under both {@code /waypoint} and its {@code /wp} shorthand.
     * Brigadier command nodes are one-shot once built, so this builds two
     * independent trees from the same lambdas rather than trying to reuse a
     * single {@link LiteralArgumentBuilder} instance for both names.
     */
    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(buildCommandTree("waypoint"));
            dispatcher.register(buildCommandTree("wp"));
        });
    }

    private LiteralArgumentBuilder<FabricClientCommandSource> buildCommandTree(String rootLiteral) {
        return ClientCommandManager.literal(rootLiteral)
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Text.literal(
                            "§7Usage: /" + rootLiteral + " add <name> | remove <name> | list | clear"));
                    return 1;
                })
                .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(this::executeAdd)))
                .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(this::executeRemove)))
                .then(ClientCommandManager.literal("list").executes(this::executeList))
                .then(ClientCommandManager.literal("clear").executes(this::executeClear));
    }

    /**
     * Shared by both entry points - {@code /waypoint add} and the "New Waypoint"
     * GUI field - so behavior (duplicate-name rejection, dimension recording,
     * persistence, feedback) can't drift between the two.
     *
     * @return true if it was added, false if a waypoint with that name already exists.
     */
    private boolean addWaypoint(String name) {
        if (mc.player == null || mc.world == null) {
            NotificationManager.INSTANCE.push("Waypoints", "You're not in a world.", NotificationType.WARNING);
            return false;
        }
        if (waypoints.stream().anyMatch(w -> w.name.equalsIgnoreCase(name))) {
            NotificationManager.INSTANCE.push("Waypoints", "A waypoint named '" + name + "' already exists.", NotificationType.WARNING);
            return false;
        }

        Entry entry = new Entry();
        entry.name = name;
        entry.x = mc.player.getX();
        entry.y = mc.player.getY();
        entry.z = mc.player.getZ();
        entry.dimension = mc.world.getRegistryKey().getValue().toString();
        waypoints.add(entry);
        save();

        NotificationManager.INSTANCE.push("Waypoints", "Added '" + name + "'.", NotificationType.SUCCESS);
        return true;
    }

    private int executeAdd(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        boolean added = addWaypoint(name);
        if (added) {
            Entry entry = waypoints.get(waypoints.size() - 1);
            ctx.getSource().sendFeedback(Text.literal(String.format(Locale.ROOT,
                    "§aAdded waypoint '%s' at %.0f, %.0f, %.0f", name, entry.x, entry.y, entry.z)));
            return 1;
        }
        ctx.getSource().sendError(Text.literal("Couldn't add '" + name + "' - see the notification for why."));
        return 0;
    }

    private int executeRemove(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        boolean removed = waypoints.removeIf(w -> w.name.equalsIgnoreCase(name));
        if (removed) {
            save();
            ctx.getSource().sendFeedback(Text.literal("§aRemoved waypoint '" + name + "'."));
            return 1;
        }
        ctx.getSource().sendError(Text.literal("No waypoint named '" + name + "'."));
        return 0;
    }

    private int executeList(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> ctx) {
        if (waypoints.isEmpty()) {
            ctx.getSource().sendFeedback(Text.literal("§7No waypoints saved yet."));
            return 1;
        }
        ctx.getSource().sendFeedback(Text.literal("§7Waypoints (" + waypoints.size() + "):"));
        for (Entry w : waypoints) {
            ctx.getSource().sendFeedback(Text.literal(String.format(Locale.ROOT,
                    "§7- §f%s §7(%.0f, %.0f, %.0f in %s)", w.name, w.x, w.y, w.z, w.dimension)));
        }
        return 1;
    }

    private int executeClear(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> ctx) {
        int count = waypoints.size();
        waypoints.clear();
        save();
        ctx.getSource().sendFeedback(Text.literal("§aCleared " + count + " waypoint(s)."));
        return 1;
    }

    // ---- HUD panel ----

    @SubscribeEvent
    private void xclient$onRender(RenderEvent event) {
        if (event.getType() != RenderEvent.Type.HUD_2D || mc.options.hudHidden) return;
        if (mc.player == null || mc.world == null || waypoints.isEmpty()) return;

        String currentDimension = mc.world.getRegistryKey().getValue().toString();
        double playerX = mc.player.getX();
        double playerY = mc.player.getY();
        double playerZ = mc.player.getZ();
        double range = maxRange.getValue();

        List<Row> rows = new ArrayList<>();
        for (Entry w : waypoints) {
            if (currentDimensionOnly.getValue() && !currentDimension.equals(w.dimension)) continue;

            double dx = w.x - playerX;
            double dy = w.y - playerY;
            double dz = w.z - playerZ;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > range) continue;

            rows.add(new Row(w.name, distance, arrowFor(dx, dz)));
        }
        if (rows.isEmpty()) return;

        rows.sort(Comparator.comparingDouble(Row::distance));
        if (rows.size() > maxShown.getValue().intValue()) {
            rows = rows.subList(0, maxShown.getValue().intValue());
        }

        DrawContext ctx = event.getDrawContext();
        var text = mc.textRenderer;
        int rowHeight = 12;
        int panelWidth = 150;
        int panelHeight = rowHeight * (rows.size() + 1) + 6;
        int screenWidth = mc.getWindow().getScaledWidth();
        int x = screenWidth - panelWidth - 6;
        int y = 6;

        RenderUtil.card(ctx, x, y, panelWidth, panelHeight, false);
        ctx.drawText(text, "Waypoints", x + 8, y + 5, XTheme.TEXT_SECONDARY, true);

        int rowY = y + rowHeight + 3;
        for (Row row : rows) {
            String suffix = showDistance.getValue() ? "  " + Math.round(row.distance()) + "m" : "";
            String label = row.arrow() + " " + row.name() + suffix;
            ctx.drawText(text, label, x + 8, rowY, XTheme.TEXT_PRIMARY, true);
            rowY += rowHeight;
        }
    }

    /**
     * Bucketed 8-direction arrow relative to where the player is currently
     * facing (not compass-absolute) - "↑" always means "walk forward".
     * {@code atan2(dz, dx) - 90} is the standard formula for the yaw a player
     * would need in order to face the point {@code (dx, dz)} away from them,
     * matching Minecraft's yaw convention; wrapping the difference against
     * the player's own yaw then gives how far left/right of centre it is.
     */
    private String arrowFor(double dx, double dz) {
        double targetYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        double relative = MathHelper.wrapDegrees((float) (targetYaw - mc.player.getYaw()));
        int index = Math.floorMod((int) Math.round(relative / 45.0), 8);
        return ARROWS[index];
    }

    private record Row(String name, double distance, String arrow) {}

    /** Plain data holder - field names double as the JSON keys, kept simple on purpose for Gson. */
    private static class Entry {
        String name;
        double x, y, z;
        String dimension;
    }
}
