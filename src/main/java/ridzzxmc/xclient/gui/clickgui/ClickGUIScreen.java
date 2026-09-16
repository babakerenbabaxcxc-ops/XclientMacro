package ridzzxmc.xclient.gui.clickgui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ridzzxmc.xclient.XClientMod;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.SliderSetting;
import ridzzxmc.xclient.gui.hud.HudEditorScreen;
import ridzzxmc.xclient.gui.theme.XTheme;
import ridzzxmc.xclient.modules.hud.ChatTimestamps;
import ridzzxmc.xclient.modules.hud.HudModule;
import ridzzxmc.xclient.modules.misc.MessageLogger;
import ridzzxmc.xclient.modules.render.BlockOverlayModule;
import ridzzxmc.xclient.modules.render.Crosshair;
import ridzzxmc.xclient.modules.render.DamageIndicator;
import ridzzxmc.xclient.modules.render.FullbrightModule;
import ridzzxmc.xclient.modules.render.HitboxModule;
import ridzzxmc.xclient.modules.render.KillEffect;
import ridzzxmc.xclient.modules.render.NoBossBar;
import ridzzxmc.xclient.modules.render.NoHurtCam;
import ridzzxmc.xclient.modules.render.Particles;
import ridzzxmc.xclient.modules.render.PingOverlay;
import ridzzxmc.xclient.modules.render.ScoreboardMod;
import ridzzxmc.xclient.modules.render.TimeChanger;
import ridzzxmc.xclient.modules.utility.AutoText;
import ridzzxmc.xclient.util.Easing;
import ridzzxmc.xclient.util.RenderUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * "Mod Menu" style ClickGUI: a sidebar on the left (X Client logo + page
 * icons/labels, plus a standalone "HUD" launcher below them) for switching
 * pages, a red header banner, filter tabs + search on the Mod Menu page, and
 * a scrollable 3-column grid of text cards with a toggle pill each. Clicking
 * a card toggles it; holding Left Shift while clicking opens its settings
 * panel instead (never the HUD editor - that's what the sidebar's "HUD" entry,
 * the tab row's "Customize HUD" link, and right-clicking a card are all for);
 * right-clicking any card jumps into the HUD layout editor (pre-selecting
 * that card if it's itself a HUD widget).
 * <p>
 * Layout is recomputed from {@link #width}/{@link #height} on every call
 * (render, mouseClicked, mouseScrolled) rather than cached, so there is no
 * risk of visuals and hit-testing ever drifting apart after a resize. Where a
 * companion screen/panel (settings panel, HUD editor, cosmetic panel) shares
 * this same layout math, it's kept in one place and reused rather than
 * duplicated.
 */
public class ClickGUIScreen extends Screen {

    // ---- sidebar pages ----
    private enum Page {
        MOD_MENU, CHAT, COSMETIC, GRAPHICS
    }

    // ---- filter tabs (Mod Menu page only) ----
    private enum Tab {
        ALL, NEW, HUD, RENDER
    }

    // Modules that don't cleanly belong to one ModuleCategory get an explicit
    // class list per sidebar page/tab instead (kept small and local to this
    // screen - see ModuleMeta for the tag side of the same "extra metadata"
    // idea). RENDER_TAB_MODULES must stay in sync with the "Render" section
    // ModuleManager.registerAll() registers.
    private static final Set<Class<? extends Module>> CHAT_MODULES = Set.of(
            MessageLogger.class, AutoText.class, ChatTimestamps.class);
    private static final Set<Class<? extends Module>> RENDER_TAB_MODULES = Set.of(
            BlockOverlayModule.class, Crosshair.class, DamageIndicator.class, FullbrightModule.class,
            HitboxModule.class, KillEffect.class, NoBossBar.class, NoHurtCam.class, Particles.class, PingOverlay.class,
            ScoreboardMod.class, TimeChanger.class);

    private static final Identifier LOGO_ICON = Identifier.of(XClientMod.RESOURCE_NAMESPACE, "textures/icons/logo.png");

    private static final int SIDEBAR_W = 44;
    private static final int SIDEBAR_BRAND_SIZE = 20;
    private static final int SIDEBAR_ITEM_H = 32;
    private static final int SIDEBAR_ITEM_GAP = 8;
    private static final int PANEL_GAP = 8;
    private static final int HEADER_H = 32;
    private static final int TAB_ROW_H = 26;
    private static final int GRID_PAD = 10;
    private static final int CARD_GAP = 8;
    private static final int CARD_H = 40;
    private static final int CARD_COLS = 3;
    private static final int TOGGLE_ANIM_MS = 180;

    private long openedAt;
    private Page page = Page.MOD_MENU;
    private Tab tab = Tab.ALL;
    private String searchQuery = "";
    private boolean searchFocused = false;
    private double scrollOffset = 0;
    private Module expandedSettingsModule;
    private int settingsPanelAnchorX;
    private int settingsPanelAnchorY;
    private SliderSetting draggingSetting;
    private boolean awaitingKeybind = false;
    private ridzzxmc.xclient.core.settings.TextSetting editingTextSetting;
    private String editingTextBuffer = "";
    private final Set<Module> favorites = new HashSet<>();

    public ClickGUIScreen() {
        super(Text.literal("X Client"));
    }

    @Override
    protected void init() {
        openedAt = System.currentTimeMillis();
    }

    /** Vertical slide-in offset for the opening animation - shared by render() and mouseClicked() so their layout math can never drift apart (see the startY comment in mouseClicked()). Settles to exactly 0 once the ~220ms opening animation finishes. */
    private int currentSlideOffset() {
        float openProgress = (float) Easing.easeOutBack(Math.min(1.0, (System.currentTimeMillis() - openedAt) / 220.0));
        return (int) (14 * (1 - openProgress));
    }

    // ---------------------------------------------------------------- render

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        RenderUtil.blurBackdrop(ctx, width, height);

        int totalW = Math.min(width - 40, 700);
        int totalH = Math.min(height - 40, 420);
        int startX = (width - totalW) / 2;
        int startY = (height - totalH) / 2 + currentSlideOffset();
        int mainX = startX + SIDEBAR_W + PANEL_GAP;
        int mainW = totalW - SIDEBAR_W - PANEL_GAP;

        renderSidebar(ctx, startX, startY, totalH, mouseX, mouseY);

        RenderUtil.dropShadow(ctx, mainX, startY, mainW, totalH, 10);
        RenderUtil.roundedRect(ctx, mainX, startY, mainW, totalH, 10, XTheme.PANEL_BG);

        // header banner (rounded top corners only, flush into the panel body below)
        RenderUtil.roundedRect(ctx, mainX, startY, mainW, HEADER_H, 10, XTheme.ACCENT);
        ctx.fill(mainX, startY + HEADER_H / 2, mainX + mainW, startY + HEADER_H, XTheme.ACCENT);
        ctx.drawText(textRenderer, pageTitle(), mainX + 14, startY + (HEADER_H - 8) / 2, 0xFFFFFFFF, true);

        int gridTop = startY + HEADER_H;

        if (page == Page.MOD_MENU) {
            renderTabRow(ctx, mainX, gridTop, mainW, mouseX, mouseY);
            gridTop += TAB_ROW_H;
        }

        int gridBottom = startY + totalH - GRID_PAD;

        if (page == Page.COSMETIC) {
            CosmeticPanel.render(ctx, mainX + GRID_PAD, gridTop + GRID_PAD / 2, mainW - GRID_PAD * 2,
                    gridBottom, mouseX, mouseY);
        } else {
            List<Module> visible = visibleModules();
            renderGrid(ctx, visible, mainX + GRID_PAD, gridTop + GRID_PAD / 2, mainW - GRID_PAD * 2,
                    gridBottom, mouseX, mouseY);
        }

        if (expandedSettingsModule != null) {
            ModuleSettingsPanel.Bounds bounds = ModuleSettingsPanel.computeBounds(
                    expandedSettingsModule, settingsPanelAnchorX, settingsPanelAnchorY);
            ModuleSettingsPanel.render(ctx, expandedSettingsModule, bounds, mouseX, mouseY, awaitingKeybind,
                    editingTextSetting, editingTextBuffer);
        }
    }

    private String pageTitle() {
        return switch (page) {
            case MOD_MENU -> "Mod Menu";
            case CHAT -> "Chat Options";
            case COSMETIC -> "Cosmetic";
            case GRAPHICS -> "Graphics";
        };
    }

    private void renderSidebar(DrawContext ctx, int x, int y, int h, int mouseX, int mouseY) {
        RenderUtil.dropShadow(ctx, x, y, SIDEBAR_W, h, 10);
        RenderUtil.roundedRect(ctx, x, y, SIDEBAR_W, h, 10, XTheme.PANEL_BG);

        int cx = x + SIDEBAR_W / 2;
        int cursorY = y + 12;

        RenderUtil.drawIcon(ctx, LOGO_ICON, cx - SIDEBAR_BRAND_SIZE / 2, cursorY, SIDEBAR_BRAND_SIZE);
        cursorY += SIDEBAR_BRAND_SIZE + 8;
        ctx.fill(x + 8, cursorY, x + SIDEBAR_W - 8, cursorY + 1, XTheme.TEXT_DISABLED);
        cursorY += 10;

        for (Page p : Page.values()) {
            boolean active = p == page;
            boolean hovered = mouseX >= x && mouseX <= x + SIDEBAR_W && mouseY >= cursorY && mouseY <= cursorY + SIDEBAR_ITEM_H;

            if (active) {
                RenderUtil.roundedRect(ctx, x + 6, cursorY, SIDEBAR_W - 12, SIDEBAR_ITEM_H, 8, XTheme.ACCENT);
            } else if (hovered) {
                RenderUtil.roundedRect(ctx, x + 6, cursorY, SIDEBAR_W - 12, SIDEBAR_ITEM_H, 8, XTheme.CARD_HOVER);
            }

            String label = sidebarLabel(p);
            int textColor = active ? 0xFFFFFFFF : XTheme.TEXT_SECONDARY;
            ctx.drawText(textRenderer, label, cx - textRenderer.getWidth(label) / 2, cursorY + 12, textColor, false);

            cursorY += SIDEBAR_ITEM_H + SIDEBAR_ITEM_GAP;
        }

        // Standalone "Customize HUD" launcher, directly below the page list
        // (below Gfx, the last page) - unlike the Page entries above, this
        // never shows as "active"/selected: clicking it immediately leaves
        // this screen for the HUD editor rather than switching content here.
        boolean hudHovered = mouseX >= x && mouseX <= x + SIDEBAR_W && mouseY >= cursorY && mouseY <= cursorY + SIDEBAR_ITEM_H;
        if (hudHovered) {
            RenderUtil.roundedRect(ctx, x + 6, cursorY, SIDEBAR_W - 12, SIDEBAR_ITEM_H, 8, XTheme.CARD_HOVER);
        }
        String hudLabel = "HUD";
        ctx.drawText(textRenderer, hudLabel, cx - textRenderer.getWidth(hudLabel) / 2, cursorY + 12, XTheme.TEXT_SECONDARY, false);
    }

    private String sidebarLabel(Page p) {
        return switch (p) {
            case MOD_MENU -> "Mods";
            case CHAT -> "Chat";
            case COSMETIC -> "Cos";
            case GRAPHICS -> "Gfx";
        };
    }

    private void renderTabRow(DrawContext ctx, int mainX, int y, int mainW, int mouseX, int mouseY) {
        int px = mainX + GRID_PAD;
        int py = y + (TAB_ROW_H - 16) / 2;

        for (Tab t : Tab.values()) {
            String label = tabLabel(t);
            int w = textRenderer.getWidth(label) + 14;
            boolean active = t == tab;
            boolean hovered = mouseX >= px && mouseX <= px + w && mouseY >= py && mouseY <= py + 16;

            if (active) {
                RenderUtil.roundedRect(ctx, px, py, w, 16, 8, XTheme.ACCENT);
            } else if (hovered) {
                RenderUtil.roundedRect(ctx, px, py, w, 16, 8, XTheme.CARD_HOVER);
            } else {
                RenderUtil.roundedRect(ctx, px, py, w, 16, 8, XTheme.CARD_BG);
            }
            int textColor = active ? 0xFFFFFFFF : XTheme.TEXT_SECONDARY;
            ctx.drawText(textRenderer, label, px + (w - textRenderer.getWidth(label)) / 2, py + 4, textColor, false);

            px += w + 6;
        }

        if (tab == Tab.HUD) {
            String editLabel = "Customize HUD";
            int editW = textRenderer.getWidth(editLabel);
            int editX = mainX + mainW - GRID_PAD - 100 - 8 - editW;
            ctx.drawText(textRenderer, editLabel, editX, py + 4, XTheme.ACCENT, false);
        }

        // search box
        int searchW = 100;
        int searchX = mainX + mainW - GRID_PAD - searchW;
        int searchColor = searchFocused ? XTheme.CARD_HOVER : XTheme.CARD_BG;
        RenderUtil.roundedRect(ctx, searchX, py, searchW, 16, 8, searchColor);
        String shown = searchQuery.isEmpty() ? "Search Mods" : searchQuery;
        int textColor2 = searchQuery.isEmpty() ? XTheme.TEXT_SECONDARY : XTheme.TEXT_PRIMARY;
        ctx.drawText(textRenderer, trimToWidth(shown, searchW - 12), searchX + 6, py + 4, textColor2, false);
    }

    private String trimToWidth(String s, int maxWidth) {
        while (s.length() > 1 && textRenderer.getWidth(s) > maxWidth) {
            s = s.substring(1);
        }
        return s;
    }

    private String tabLabel(Tab t) {
        return switch (t) {
            case ALL -> "All";
            case NEW -> "New";
            case HUD -> "HUD";
            case RENDER -> "Render";
        };
    }

    private void renderGrid(DrawContext ctx, List<Module> modules, int gx, int gy, int gw, int gBottom,
                             int mouseX, int mouseY) {
        int cardW = (gw - (CARD_COLS - 1) * CARD_GAP) / CARD_COLS;
        int rows = (modules.size() + CARD_COLS - 1) / CARD_COLS;
        int contentH = rows == 0 ? 0 : rows * CARD_H + (rows - 1) * CARD_GAP;
        int viewH = Math.max(0, gBottom - gy);
        double maxScroll = Math.max(0, contentH - viewH);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;

        ctx.enableScissor(gx, gy, gx + gw, gBottom);

        for (int i = 0; i < modules.size(); i++) {
            int col = i % CARD_COLS;
            int row = i / CARD_COLS;
            int cx = gx + col * (cardW + CARD_GAP);
            int cy = gy + row * (CARD_H + CARD_GAP) - (int) Math.round(scrollOffset);

            if (cy + CARD_H < gy || cy > gBottom) continue; // offscreen, skip drawing

            renderCard(ctx, modules.get(i), cx, cy, cardW, CARD_H, mouseX, mouseY);
        }

        ctx.disableScissor();

        if (modules.isEmpty()) {
            String msg = "No modules match";
            ctx.drawText(textRenderer, msg, gx + (gw - textRenderer.getWidth(msg)) / 2, gy + 20, XTheme.TEXT_SECONDARY, false);
        }
    }

    private void renderCard(DrawContext ctx, Module module, int x, int y, int w, int h, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;

        RenderUtil.card(ctx, x, y, w, h, hovered);
        if (hovered) {
            RenderUtil.rectOutline(ctx, x, y, w, h, 1, XTheme.ACCENT);
        }

        // favorite dot, top-right
        int favSize = 6;
        int favX = x + w - favSize - 6;
        int favY = y + 6;
        boolean favorited = favorites.contains(module);
        RenderUtil.roundedRect(ctx, favX, favY, favSize, favSize, 3, favorited ? XTheme.ACCENT : XTheme.TEXT_DISABLED);

        // name + toggle, vertically centered single row (no icon anymore)
        int pillW = 22, pillH = 11;
        int pillY = y + (h - pillH) / 2;
        int rowY = pillY + 1;
        float animT = (float) Easing.easeInOutCubic(module.getAnimationProgress(TOGGLE_ANIM_MS));

        int nameMaxWidth = w - 44;
        String name = trimToWidth(module.getName(), nameMaxWidth);
        int textColor = module.isEnabled() ? XTheme.TEXT_PRIMARY : XTheme.TEXT_SECONDARY;
        ctx.drawText(textRenderer, name, x + 8, rowY + 2, textColor, false);

        int pillX = x + w - pillW - 8;
        int pillColor = module.isEnabled()
                ? blendColor(XTheme.TOGGLE_OFF, XTheme.TOGGLE_ON, module.isAnimating() ? animT : 1f)
                : blendColor(XTheme.TOGGLE_ON, XTheme.TOGGLE_OFF, module.isAnimating() ? animT : 1f);
        RenderUtil.roundedRect(ctx, pillX, pillY, pillW, pillH, pillH / 2, pillColor);

        float knobT = module.isEnabled() ? (module.isAnimating() ? animT : 1f) : (module.isAnimating() ? 1f - animT : 0f);
        int knobX = pillX + 2 + Math.round((pillW - pillH) * knobT);
        RenderUtil.roundedRect(ctx, knobX, pillY + 2, pillH - 4, pillH - 4, (pillH - 4) / 2, 0xFFFFFFFF);
    }

    private static int blendColor(int from, int to, float t) {
        int a1 = (from >> 24) & 0xFF, r1 = (from >> 16) & 0xFF, g1 = (from >> 8) & 0xFF, b1 = from & 0xFF;
        int a2 = (to >> 24) & 0xFF, r2 = (to >> 16) & 0xFF, g2 = (to >> 8) & 0xFF, b2 = to & 0xFF;
        int a = Math.round(Easing.lerp(a1, a2, t));
        int r = Math.round(Easing.lerp(r1, r2, t));
        int g = Math.round(Easing.lerp(g1, g2, t));
        int b = Math.round(Easing.lerp(b1, b2, t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // ------------------------------------------------------------- modules

    private List<Module> visibleModules() {
        List<Module> base = switch (page) {
            case MOD_MENU -> XClientMod.getModuleManager().getModules();
            case CHAT -> filterByClass(CHAT_MODULES);
            case COSMETIC -> List.of(); // Cosmetic page renders CosmeticPanel instead of a module grid - see render()/mouseClicked()
            case GRAPHICS -> filterByCategoryOrClass(ModuleCategory.OPTIMIZATION, Set.of());
        };

        List<Module> result = new ArrayList<>(base);

        if (page == Page.MOD_MENU) {
            result.removeIf(m -> switch (tab) {
                case ALL -> false;
                case NEW -> !ModuleMeta.isNew(m);
                case HUD -> m.getCategory() != ModuleCategory.HUD;
                case RENDER -> !RENDER_TAB_MODULES.contains(m.getClass());
            });
        }

        if (!searchQuery.isEmpty()) {
            String q = searchQuery.toLowerCase();
            result.removeIf(m -> !m.getName().toLowerCase().contains(q)
                    && !m.getDescription().toLowerCase().contains(q));
        }

        return result;
    }

    private List<Module> filterByCategoryOrClass(ModuleCategory cat, Set<Class<? extends Module>> extra) {
        List<Module> out = new ArrayList<>();
        for (Module m : XClientMod.getModuleManager().getModules()) {
            if (m.getCategory() == cat || extra.contains(m.getClass())) {
                out.add(m);
            }
        }
        return out;
    }

    private List<Module> filterByClass(Set<Class<? extends Module>> classes) {
        List<Module> out = new ArrayList<>();
        for (Module m : XClientMod.getModuleManager().getModules()) {
            if (classes.contains(m.getClass())) {
                out.add(m);
            }
        }
        return out;
    }

    // ------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) return super.mouseClicked(mouseX, mouseY, button);

        int totalW = Math.min(width - 40, 700);
        int totalH = Math.min(height - 40, 420);
        int startX = (width - totalW) / 2;
        // Must match render()'s startY exactly, slide-in offset included - otherwise
        // every click made during the ~220ms opening animation lands up to 14px away
        // from what's actually on screen (easily a full card-row off), which reads as
        // "nothing I click works" even though the hit-testing below is otherwise correct.
        int startY = (height - totalH) / 2 + currentSlideOffset();
        int mainX = startX + SIDEBAR_W + PANEL_GAP;
        int mainW = totalW - SIDEBAR_W - PANEL_GAP;

        // sidebar, tab row, "Customize HUD" link and search box only respond to
        // a normal left click - right-click's only job on this screen is the
        // module-card shortcut further down.
        if (button == 0) {
            if (mouseX >= startX && mouseX <= startX + SIDEBAR_W && mouseY >= startY && mouseY <= startY + totalH) {
                // Must mirror renderSidebar()'s cursorY math exactly: 12 (top pad) + SIDEBAR_BRAND_SIZE+8 (brand icon row) + 10 (separator gap).
                int cursorY = startY + 12 + SIDEBAR_BRAND_SIZE + 8 + 10;
                for (Page p : Page.values()) {
                    if (mouseY >= cursorY && mouseY <= cursorY + SIDEBAR_ITEM_H) {
                        page = p;
                        tab = Tab.ALL;
                        scrollOffset = 0;
                        commitTextEdit();
                        expandedSettingsModule = null;
                        draggingSetting = null;
                        awaitingKeybind = false;
                        searchFocused = false;
                        return true;
                    }
                    cursorY += SIDEBAR_ITEM_H + SIDEBAR_ITEM_GAP;
                }
                // Standalone "Customize HUD" launcher, right below the page list.
                if (mouseY >= cursorY && mouseY <= cursorY + SIDEBAR_ITEM_H) {
                    close();
                    MinecraftClient.getInstance().setScreen(new HudEditorScreen());
                    return true;
                }
                return true;
            }
        }

        int gridTop = startY + HEADER_H;

        if (page == Page.MOD_MENU) {
            int py = gridTop + (TAB_ROW_H - 16) / 2;

            if (button == 0) {
                int px = mainX + GRID_PAD;
                for (Tab t : Tab.values()) {
                    String label = tabLabel(t);
                    int w = textRenderer.getWidth(label) + 14;
                    if (mouseX >= px && mouseX <= px + w && mouseY >= py && mouseY <= py + 16) {
                        tab = t;
                        scrollOffset = 0;
                        return true;
                    }
                    px += w + 6;
                }

                if (tab == Tab.HUD) {
                    String editLabel = "Customize HUD";
                    int editW = textRenderer.getWidth(editLabel);
                    int editX = mainX + mainW - GRID_PAD - 100 - 8 - editW;
                    if (mouseX >= editX && mouseX <= editX + editW && mouseY >= py && mouseY <= py + 16) {
                        close();
                        MinecraftClient.getInstance().setScreen(new HudEditorScreen());
                        return true;
                    }
                }

                int searchW = 100;
                int searchX = mainX + mainW - GRID_PAD - searchW;
                if (mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= py && mouseY <= py + 16) {
                    searchFocused = true;
                    return true;
                }
                searchFocused = false;
            }

            gridTop += TAB_ROW_H;
        }

        if (page == Page.COSMETIC) {
            if (button == 0 && CosmeticPanel.mouseClicked(mainX + GRID_PAD, gridTop + GRID_PAD / 2, mainW - GRID_PAD * 2,
                    startY + totalH - GRID_PAD, mouseX, mouseY)) {
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (button == 0 && expandedSettingsModule != null) {
            ModuleSettingsPanel.Bounds bounds = ModuleSettingsPanel.computeBounds(
                    expandedSettingsModule, settingsPanelAnchorX, settingsPanelAnchorY);
            if (ModuleSettingsPanel.contains(bounds, mouseX, mouseY)) {
                commitTextEdit(); // clicking anywhere else in the panel confirms whatever was being typed
                awaitingKeybind = false;
                if (ModuleSettingsPanel.isKeybindRowHit(bounds, mouseX, mouseY)) {
                    awaitingKeybind = true;
                } else {
                    ridzzxmc.xclient.core.settings.TextSetting textHit =
                            ModuleSettingsPanel.isTextSettingRowHit(expandedSettingsModule, bounds, mouseX, mouseY);
                    if (textHit != null) {
                        editingTextSetting = textHit;
                        editingTextBuffer = textHit.getValue() == null ? "" : textHit.getValue();
                    } else {
                        draggingSetting = ModuleSettingsPanel.handleClick(expandedSettingsModule, bounds, mouseX, mouseY);
                    }
                }
                return true;
            }
        }

        List<Module> visible = visibleModules();
        int gx = mainX + GRID_PAD;
        int gy = gridTop + GRID_PAD / 2;
        int gBottom = startY + totalH - GRID_PAD;
        int gw = mainW - GRID_PAD * 2;
        int cardW = (gw - (CARD_COLS - 1) * CARD_GAP) / CARD_COLS;

        if (mouseY >= gy && mouseY <= gBottom && mouseX >= gx && mouseX <= gx + gw) {
            for (int i = 0; i < visible.size(); i++) {
                int col = i % CARD_COLS;
                int row = i / CARD_COLS;
                int cx = gx + col * (cardW + CARD_GAP);
                int cy = gy + row * (CARD_H + CARD_GAP) - (int) Math.round(scrollOffset);

                if (mouseX >= cx && mouseX <= cx + cardW && mouseY >= cy && mouseY <= cy + CARD_H) {
                    Module module = visible.get(i);

                    if (button == 1) {
                        // Right-click any module card: jump straight into the HUD
                        // layout editor. If this specific card is itself a HUD
                        // widget, pre-select it there (e.g. right-clicking
                        // "Armor HUD" opens straight into customizing Armor HUD).
                        close();
                        HudEditorScreen editor = module instanceof HudModule hud
                                ? new HudEditorScreen(hud)
                                : new HudEditorScreen();
                        MinecraftClient.getInstance().setScreen(editor);
                        return true;
                    }

                    int favSize = 6;
                    int favX = cx + cardW - favSize - 6;
                    int favY = cy + 6;
                    if (mouseX >= favX - 3 && mouseX <= favX + favSize + 3 && mouseY >= favY - 3 && mouseY <= favY + favSize + 3) {
                        if (!favorites.remove(module)) favorites.add(module);
                        return true;
                    }

                    if (isLeftShiftDown()) {
                        // Shift+click opens/closes this module's settings panel.
                        // This NEVER opens the HUD editor - that's reached only via
                        // the sidebar's "HUD" entry, the tab row's "Customize HUD"
                        // link, or right-clicking a card (handled above).
                        commitTextEdit();
                        if (expandedSettingsModule == module) {
                            expandedSettingsModule = null;
                            draggingSetting = null;
                            awaitingKeybind = false;
                        } else {
                            expandedSettingsModule = module;
                            settingsPanelAnchorX = (int) mouseX;
                            settingsPanelAnchorY = (int) mouseY;
                            draggingSetting = null;
                            awaitingKeybind = false;
                        }
                    } else {
                        // A plain click anywhere on the card toggles it on/off.
                        module.toggle();
                        if (expandedSettingsModule == module) {
                            commitTextEdit();
                            expandedSettingsModule = null;
                            draggingSetting = null;
                            awaitingKeybind = false;
                        }
                    }
                    return true;
                }
            }
        }

        if (button == 0) {
            commitTextEdit();
            expandedSettingsModule = null;
            draggingSetting = null;
            awaitingKeybind = false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Mouse click events don't carry a modifiers mask, so Shift+click needs a direct GLFW poll (same approach the ClickGUI/HUD editor keybind uses). */
    private static boolean isLeftShiftDown() {
        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;
    }

    /** Saves whatever's currently in {@link #editingTextBuffer} back to {@link #editingTextSetting} and clears editing state - called from every path that would otherwise silently drop an in-progress edit (clicking away, closing the panel, closing the whole screen). */
    private void commitTextEdit() {
        if (editingTextSetting != null) {
            editingTextSetting.setValue(editingTextBuffer);
            editingTextSetting = null;
            editingTextBuffer = "";
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingSetting != null && expandedSettingsModule != null) {
            ModuleSettingsPanel.Bounds bounds = ModuleSettingsPanel.computeBounds(
                    expandedSettingsModule, settingsPanelAnchorX, settingsPanelAnchorY);
            ModuleSettingsPanel.handleDrag(draggingSetting, bounds, mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingSetting = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset -= verticalAmount * 18;
        if (scrollOffset < 0) scrollOffset = 0;
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (editingTextSetting != null) {
            if (editingTextBuffer.length() < editingTextSetting.getMaxLength() && chr >= 32) {
                editingTextBuffer += chr;
            }
            return true;
        }
        if (searchFocused && page == Page.MOD_MENU) {
            if (searchQuery.length() < 24 && chr >= 32) {
                searchQuery += chr;
            }
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Editing a TextSetting takes priority over everything else on this screen
        // (including the Right-Shift close shortcut) for the same reason keybind
        // capture does below - typing "shift" or any other special key into a
        // password/name field must not accidentally close the menu instead.
        if (editingTextSetting != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                commitTextEdit();
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                editingTextSetting = null;
                editingTextBuffer = "";
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !editingTextBuffer.isEmpty()) {
                editingTextBuffer = editingTextBuffer.substring(0, editingTextBuffer.length() - 1);
            }
            return true;
        }

        // Capturing a new keybind for the open settings panel takes priority
        // over everything else on this screen, including the Right-Shift
        // close shortcut below - so binding a module to Right Shift (or any
        // other key this screen would otherwise treat specially) works as
        // expected instead of silently closing the menu.
        if (awaitingKeybind && expandedSettingsModule != null) {
            awaitingKeybind = false;
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                expandedSettingsModule.getKeybind().setKey(keyCode);
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            XClientMod.suppressNextGuiKeyEdge();
            close();
            return true;
        }
        if (searchFocused && page == Page.MOD_MENU && keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
            searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        commitTextEdit();
        XClientMod.getConfigManager().save(XClientMod.getModuleManager());
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
