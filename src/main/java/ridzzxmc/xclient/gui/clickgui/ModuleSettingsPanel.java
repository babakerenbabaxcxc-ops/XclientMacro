package ridzzxmc.xclient.gui.clickgui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.settings.*;
import ridzzxmc.xclient.gui.theme.XTheme;
import ridzzxmc.xclient.util.RenderUtil;

/**
 * Floating panel listing a module's keybind plus every {@link Setting} it
 * exposes. Rendered beside the clicked card; each setting type gets its own
 * compact widget so the whole panel stays readable even with 5-6 settings
 * stacked.
 * <p>
 * The keybind row at the top is always present (every {@link Module} has one,
 * even if unbound) - clicking it starts "capture" mode, where the very next
 * key the player presses becomes the new bind (Escape cancels without
 * changing it). Capture state itself lives on the owning {@link ClickGUIScreen},
 * not here, since this class stays a stateless set of static helpers like the
 * rest of the ClickGUI - {@link #isKeybindRowHit} just tells the screen
 * whether to enter that mode.
 */
public final class ModuleSettingsPanel {

    private static final int PANEL_WIDTH = 180;
    private static final int ROW_HEIGHT = 22;

    /** A small preset palette for ColorSetting - clicking a swatch cycles to the next entry. */
    private static final int[] COLOR_PRESETS = {
            0xFFFFFFFF, 0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55,
            0xFF55FF55, 0xFF55FFFF, 0xFF5599FF, 0xFFFF55FF
    };

    private ModuleSettingsPanel() {}

    /** Fixed panel bounds anchored at (anchorX, anchorY) - the point the panel was opened at, NOT the live mouse position. */
    public record Bounds(int x, int y, int width, int height) {}

    public static Bounds computeBounds(Module module, int anchorX, int anchorY) {
        var mc = MinecraftClient.getInstance();
        int settingsCount = (int) module.getSettings().stream().filter(Setting::isVisible).count();
        int panelHeight = 20 + ROW_HEIGHT + settingsCount * ROW_HEIGHT; // header + keybind row + settings
        int x = Math.min(mc.getWindow().getScaledWidth() - PANEL_WIDTH - 8, anchorX + 12);
        int y = Math.min(mc.getWindow().getScaledHeight() - panelHeight - 8, anchorY);
        return new Bounds(x, y, PANEL_WIDTH, panelHeight);
    }

    public static boolean contains(Bounds b, double mouseX, double mouseY) {
        return mouseX >= b.x() && mouseX <= b.x() + b.width() && mouseY >= b.y() && mouseY <= b.y() + b.height();
    }

    /**
     * Renders the panel at a fixed anchor position (does not track the live mouse -
     * doing so previously made it impossible to reach and click widgets lower in
     * the panel, since the panel kept sliding to follow the cursor).
     *
     * @param editingSetting the {@link TextSetting} currently being edited (row shows
     *                       {@code editingBuffer} with a cursor instead of its stored value), or null
     * @param editingBuffer  live in-progress text for {@code editingSetting}; ignored if that's null
     */
    public static void render(DrawContext ctx, Module module, Bounds b, int mouseX, int mouseY, boolean awaitingKeybind,
                               TextSetting editingSetting, String editingBuffer) {
        var mc = MinecraftClient.getInstance();
        var settings = module.getSettings().stream().filter(Setting::isVisible).toList();

        RenderUtil.card(ctx, b.x(), b.y(), b.width(), b.height(), false);
        ctx.drawText(mc.textRenderer, module.getName(), b.x() + 8, b.y() + 6, XTheme.ACCENT, true);

        int rowX = b.x() + 8;
        int rowW = b.width() - 16;
        int rowY = b.y() + 20;

        boolean keybindHovered = mouseX >= rowX - 4 && mouseX <= rowX + rowW + 4
                && mouseY >= rowY - 3 && mouseY <= rowY + ROW_HEIGHT - 6;
        if (keybindHovered || awaitingKeybind) {
            ctx.fill(b.x() + 2, rowY - 3, b.x() + b.width() - 2, rowY + ROW_HEIGHT - 6, 0x22FFFFFF);
        }
        ctx.drawText(mc.textRenderer, "Keybind", rowX, rowY, XTheme.TEXT_SECONDARY, false);
        String keyLabel = awaitingKeybind ? "Press a key\u2026" : module.getKeybind().getDisplayName();
        int keyColor = awaitingKeybind ? XTheme.ACCENT : (module.getKeybind().isBound() ? XTheme.TEXT_PRIMARY : XTheme.TEXT_DISABLED);
        ctx.drawText(mc.textRenderer, keyLabel, rowX + rowW - mc.textRenderer.getWidth(keyLabel), rowY, keyColor, false);
        rowY += ROW_HEIGHT;

        for (Setting<?> setting : settings) {
            boolean hovered = mouseX >= rowX - 4 && mouseX <= rowX + rowW + 4
                    && mouseY >= rowY - 3 && mouseY <= rowY + ROW_HEIGHT - 6;
            if (hovered) {
                ctx.fill(b.x() + 2, rowY - 3, b.x() + b.width() - 2, rowY + ROW_HEIGHT - 6, 0x22FFFFFF);
            }
            if (setting == editingSetting) {
                renderTextEditing(ctx, (TextSetting) setting, editingBuffer, rowX, rowY, rowW);
            } else {
                renderSetting(ctx, setting, rowX, rowY, rowW);
            }
            rowY += ROW_HEIGHT;
        }
    }

    /** @return true if this click landed on the keybind row - the caller should enter "awaiting a key press" mode rather than call {@link #handleClick}. */
    public static boolean isKeybindRowHit(Bounds b, double mouseX, double mouseY) {
        int rowX = b.x() + 8;
        int rowW = b.width() - 16;
        int rowY = b.y() + 20;
        return mouseY >= rowY - 3 && mouseY <= rowY + ROW_HEIGHT - 6 && mouseX >= rowX && mouseX <= rowX + rowW;
    }

    /**
     * @return the {@link TextSetting} under the cursor, or null - checked by the caller
     * before {@link #handleClick} so a click can start text editing instead of the
     * click-to-cycle/toggle behavior {@link #handleClick} applies to every other setting type.
     */
    public static TextSetting isTextSettingRowHit(Module module, Bounds b, double mouseX, double mouseY) {
        var settings = module.getSettings().stream().filter(Setting::isVisible).toList();
        int rowY = b.y() + 20 + ROW_HEIGHT; // skip the keybind row
        int rowX = b.x() + 8;
        int rowW = b.width() - 16;
        for (Setting<?> setting : settings) {
            boolean inRow = mouseY >= rowY - 3 && mouseY <= rowY + ROW_HEIGHT - 6 && mouseX >= rowX && mouseX <= rowX + rowW;
            if (inRow) {
                return setting instanceof TextSetting t ? t : null;
            }
            rowY += ROW_HEIGHT;
        }
        return null;
    }

    /**
     * Routes a click within the panel to the setting it landed on (assumes
     * {@link #isKeybindRowHit} was already checked and returned false).
     * Returns the {@link SliderSetting} that should keep tracking the mouse
     * for a drag, or null if the click didn't start a drag (toggle/mode/color
     * settings apply immediately and don't need drag tracking).
     */
    public static SliderSetting handleClick(Module module, Bounds b, double mouseX, double mouseY) {
        var settings = module.getSettings().stream().filter(Setting::isVisible).toList();
        int rowY = b.y() + 20 + ROW_HEIGHT; // skip the keybind row
        int rowX = b.x() + 8;
        int rowW = b.width() - 16;
        for (Setting<?> setting : settings) {
            boolean inRow = mouseY >= rowY - 3 && mouseY <= rowY + ROW_HEIGHT - 6 && mouseX >= rowX && mouseX <= rowX + rowW;
            if (inRow) {
                if (setting instanceof BooleanSetting bs) {
                    bs.toggle();
                } else if (setting instanceof SliderSetting s) {
                    s.setFromRatio((mouseX - rowX) / rowW);
                    return s;
                } else if (setting instanceof ModeSetting m) {
                    m.cycleNext();
                } else if (setting instanceof ColorSetting c) {
                    cycleColorPreset(c);
                } else if (setting instanceof TextSetting) {
                    // Handled by the caller via isTextSettingRowHit before handleClick is
                    // ever reached - this branch only exists so falling through here
                    // (e.g. a future caller that skips that check) is a silent no-op
                    // instead of an unhandled setting type.
                }
                return null;
            }
            rowY += ROW_HEIGHT;
        }
        return null;
    }

    /** Continues a slider drag started by {@link #handleClick}; call while the mouse button is still held. */
    public static void handleDrag(SliderSetting setting, Bounds b, double mouseX) {
        int rowX = b.x() + 8;
        int rowW = b.width() - 16;
        setting.setFromRatio((mouseX - rowX) / rowW);
    }

    private static void cycleColorPreset(ColorSetting c) {
        int current = c.getValue();
        int index = -1;
        for (int i = 0; i < COLOR_PRESETS.length; i++) {
            if (COLOR_PRESETS[i] == current) {
                index = i;
                break;
            }
        }
        c.setValue(COLOR_PRESETS[(index + 1) % COLOR_PRESETS.length]);
    }

    private static void renderSetting(DrawContext ctx, Setting<?> setting, int x, int y, int width) {
        var mc = MinecraftClient.getInstance();

        ctx.drawText(mc.textRenderer, setting.getName(), x, y, XTheme.TEXT_SECONDARY, false);

        if (setting instanceof BooleanSetting b) {
            int pillW = 26, pillH = 12;
            int pillX = x + width - pillW;
            int color = b.getValue() ? XTheme.TOGGLE_ON : XTheme.TOGGLE_OFF;
            RenderUtil.roundedRect(ctx, pillX, y - 1, pillW, pillH, pillH / 2, color);
            int knobX = pillX + (b.getValue() ? pillW - pillH + 2 : 2);
            RenderUtil.roundedRect(ctx, knobX, y, pillH - 4, pillH - 4, (pillH - 4) / 2, 0xFFFFFFFF);

        } else if (setting instanceof SliderSetting s) {
            int barY = y + 11;
            RenderUtil.roundedRect(ctx, x, barY, width, 3, 1, XTheme.TOGGLE_OFF);
            int filled = (int) (width * s.getRatio());
            RenderUtil.roundedRect(ctx, x, barY, Math.max(3, filled), 3, 1, XTheme.ACCENT);
            String valueText = s.isInteger() ? String.valueOf(s.getValue().intValue()) : String.format("%.2f", s.getValue());
            ctx.drawText(mc.textRenderer, valueText, x + width - mc.textRenderer.getWidth(valueText), y, XTheme.TEXT_PRIMARY, false);

        } else if (setting instanceof ModeSetting m) {
            String value = m.getValue();
            ctx.drawText(mc.textRenderer, value, x + width - mc.textRenderer.getWidth(value), y, XTheme.ACCENT, false);

        } else if (setting instanceof ColorSetting c) {
            int swatchSize = 12;
            ctx.fill(x + width - swatchSize, y - 1, x + width, y - 1 + swatchSize, c.getValue());

        } else if (setting instanceof TextSetting t) {
            String display = displayValue(t, t.getValue());
            ctx.drawText(mc.textRenderer, display, x + width - mc.textRenderer.getWidth(display), y, XTheme.TEXT_PRIMARY, false);
        }
    }

    /** Live version of the {@link TextSetting} row while it's being typed into: shows the in-progress buffer with a blinking cursor. */
    private static void renderTextEditing(DrawContext ctx, TextSetting setting, String buffer, int x, int y, int width) {
        var mc = MinecraftClient.getInstance();
        ctx.drawText(mc.textRenderer, setting.getName(), x, y, XTheme.TEXT_SECONDARY, false);

        boolean cursorOn = (System.currentTimeMillis() / 500) % 2 == 0;
        String shown = displayValue(setting, buffer) + (cursorOn ? "_" : "");
        ctx.drawText(mc.textRenderer, shown, x + width - mc.textRenderer.getWidth(shown), y, XTheme.ACCENT, false);
    }

    private static String displayValue(TextSetting setting, String value) {
        if (value == null || value.isEmpty()) return setting.isMasked() ? "" : "(none)";
        return setting.isMasked() ? "\u2022".repeat(value.length()) : value;
    }
}
