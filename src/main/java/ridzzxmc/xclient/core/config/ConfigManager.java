package ridzzxmc.xclient.core.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleManager;
import ridzzxmc.xclient.core.settings.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persists module enabled-state, settings and keybinds to
 * {@code .minecraft/config/xclient/config.json}.
 */
public class ConfigManager {

    private final Path configDir = FabricLoader.getInstance().getConfigDir().resolve("xclient");
    private final Path configFile = configDir.resolve("config.json");
    private final com.google.gson.Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void save(ModuleManager moduleManager) {
        try {
            Files.createDirectories(configDir);

            JsonObject root = new JsonObject();
            JsonArray moduleArray = new JsonArray();

            for (Module module : moduleManager.getModules()) {
                JsonObject moduleJson = new JsonObject();
                moduleJson.addProperty("name", module.getName());
                moduleJson.addProperty("enabled", module.isEnabled());
                moduleJson.addProperty("keybind", module.getKeybind().getKey());

                if (module instanceof ridzzxmc.xclient.modules.hud.HudModule hud) {
                    moduleJson.addProperty("x", hud.getX());
                    moduleJson.addProperty("y", hud.getY());
                }

                JsonObject settingsJson = new JsonObject();
                for (Setting<?> setting : module.getSettings()) {
                    if (setting instanceof BooleanSetting b) {
                        settingsJson.addProperty(b.getName(), b.getValue());
                    } else if (setting instanceof SliderSetting s) {
                        settingsJson.addProperty(s.getName(), s.getValue());
                    } else if (setting instanceof ModeSetting m) {
                        settingsJson.addProperty(m.getName(), m.getValue());
                    } else if (setting instanceof ColorSetting c) {
                        settingsJson.addProperty(c.getName(), c.getValue());
                    } else if (setting instanceof TextSetting t) {
                        settingsJson.addProperty(t.getName(), t.getValue());
                    }
                }
                moduleJson.add("settings", settingsJson);
                moduleArray.add(moduleJson);
            }

            root.add("modules", moduleArray);
            Files.writeString(configFile, gson.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[X Client] Failed to save config:");
            e.printStackTrace();
        }
    }

    public void load(ModuleManager moduleManager) {
        if (!Files.exists(configFile)) return;

        try {
            String content = Files.readString(configFile, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            JsonArray moduleArray = root.getAsJsonArray("modules");

            for (var element : moduleArray) {
                JsonObject moduleJson = element.getAsJsonObject();
                String name = moduleJson.get("name").getAsString();

                moduleManager.getByName(name).ifPresent(module -> {
                    // Explicitly set both true and false: several modules default to
                    // enabled=true, so only ever turning modules ON here meant a saved
                    // "disabled" state for one of those was silently ignored and the
                    // module came back on every restart.
                    module.setEnabled(moduleJson.get("enabled").getAsBoolean());
                    if (moduleJson.has("keybind")) {
                        module.getKeybind().setKey(moduleJson.get("keybind").getAsInt());
                    }

                    if (module instanceof ridzzxmc.xclient.modules.hud.HudModule hud
                            && moduleJson.has("x") && moduleJson.has("y")) {
                        hud.setPosition(moduleJson.get("x").getAsFloat(), moduleJson.get("y").getAsFloat());
                    }

                    JsonObject settingsJson = moduleJson.getAsJsonObject("settings");
                    for (Setting<?> setting : module.getSettings()) {
                        if (!settingsJson.has(setting.getName())) continue;
                        var jsonValue = settingsJson.get(setting.getName());

                        if (setting instanceof BooleanSetting b) {
                            b.setValue(jsonValue.getAsBoolean());
                        } else if (setting instanceof SliderSetting s) {
                            s.setValue(jsonValue.getAsDouble());
                        } else if (setting instanceof ModeSetting m) {
                            m.setValue(jsonValue.getAsString());
                        } else if (setting instanceof ColorSetting c) {
                            c.setValue(jsonValue.getAsInt());
                        } else if (setting instanceof TextSetting t) {
                            t.setValue(jsonValue.getAsString());
                        }
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("[X Client] Failed to load config, using defaults:");
            e.printStackTrace();
        }
    }
}
