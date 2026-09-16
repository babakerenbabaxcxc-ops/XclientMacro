package ridzzxmc.xclient.gui.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ridzzxmc.xclient.XClientMod;
import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.gui.clickgui.ClickGUIScreen;
import ridzzxmc.xclient.gui.theme.XTheme;
import ridzzxmc.xclient.modules.hud.HudModule;
import ridzzxmc.xclient.util.RenderUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * "Customize HUD" editor for widget placement and size. Opened either from
 * the ClickGUI's HUD tab ("Customize HUD" link) or by right-clicking any
 * module card in the ClickGUI (which also pre-selects that widget here, if
 * it's a HUD one - e.g. right-clicking "Armor HUD" opens straight into
 * editing Armor HUD). Every enabled {@link HudModule} gets an outlined
 * bounding box:
 * <ul>
 *   <li>drag with the mouse to reposition</li>
 *   <li>scroll the mouse wheel over a widget (or a selected one) to resize it</li>
 *   <li>click a widget to select it, then use arrow keys to nudge it,
 *       {@code [ }/{@code ]} to shrink/grow it, or {@code R} to reset it</li>
 *   <li>right-click a widget for a small menu (reset / disable)</li>
 *   <li>{@code S} toggles grid snapping, {@code A} toggles alignment guides</li>
 * </ul>
 * Selection persists after releasing the mouse so keyboard adjustments work
 * without holding a drag. Escape or Right Shift both save and exit.
 */
public class HudEditorScreen extends Screen {

    private static final Identifier LOGO_ICON = Identifier.of(XClientMod.RESOURCE_NAMESPACE, "textures/icons/logo.png");

    private static final float SCROLL_STEP = 0.05f;
    private static final float BRACKET_STEP = 0.05f;
    private static final float NUDGE_STEP = 1f;
    private static final float NUDGE_STEP_FAST = 8f;
    private static final float GRID_SIZE = 8f;
    private static final float GUIDE_THRESHOLD = 4f;
    private static final int CTX_MENU_W = 92;
    private static final int CTX_ROW_H = 18;

    private HudModule dragging;
    private HudModule selected;
    private float dragOffsetX, dragOffsetY;

    private boolean snapEnabled = false;
    private boolean guidesEnabled = true;
    private Float guideLineX;
    private Float guideLineY;

    private HudModule contextMenuTarget;
    private int contextMenuRawX, contextMenuRawY;

    public HudEditorScreen() {
        this(null);
    }

    /** Opens the editor with a specific widget already selected (used by the ClickGUI's right-click-a-card shortcut). */
    public HudEditorScreen(HudModule preSelected) {
        super(Text.literal("Customize HUD"));
        this.selected = preSelected;
    }

    private List<HudModule> enabledHuds() {
        List<HudModule> list = new ArrayList<>();
        for (Module module : XClientMod.getModuleManager().getModules()) {
            if (module instanceof HudModule hud && hud.isEnabled()) {
                list.add(hud);
            }
        }
        return list;
    }

    /** Topmost enabled HUD widget whose bounding box contains this point, or null. */
    private HudModule hudAt(double mouseX, double mouseY) {
        for (HudModule hud : enabledHuds()) {
            int w = Math.round(hud.getWidth() * hud.getScale());
            int h = Math.round(hud.getHeight() * hud.getScale());
            if (mouseX >= hud.getX() && mouseX <= hud.getX() + w && mouseY >= hud.getY() && mouseY <= hud.getY() + h) {
                return hud;
            }
        }
        return null;
    }

    // -------------------------------------------------------------- layout

    /** Center toolbar layout (icon, settings button). Recomputed on every call - see {@link ClickGUIScreen}. */
    private record ToolbarLayout(int iconX, int iconY, int iconSize,
                                  int settingsX, int settingsY, int settingsW, int settingsH) {}

    private ToolbarLayout toolbarLayout() {
        int centerX = width / 2;
        int iconSize = 28;
        int iconY = 14;
        int iconX = centerX - iconSize / 2;

        String btnLabel = "\u2699 X SETTINGS";
        int btnW = textRenderer.getWidth(btnLabel) + 20;
        int btnH = 16;
        int btnY = iconY + iconSize + 6;
        int btnX = centerX - btnW / 2;

        return new ToolbarLayout(iconX, iconY, iconSize, btnX, btnY, btnW, btnH);
    }

    private record MenuBounds(int x, int y, int w, int h, int resetY, int disableY) {}

    private MenuBounds contextMenuBounds() {
        int w = CTX_MENU_W;
        int h = CTX_ROW_H * 2 + 6;
        int x = Math.max(4, Math.min(contextMenuRawX, width - w - 4));
        int y = Math.max(4, Math.min(contextMenuRawY, height - h - 4));
        return new MenuBounds(x, y, w, h, y + 4, y + 4 + CTX_ROW_H);
    }

    // ---------------------------------------------------------------- render

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x60000000);

        HudModule hovered = hudAt(mouseX, mouseY);

        for (HudModule hud : enabledHuds()) {
            int w = Math.round(hud.getWidth() * hud.getScale());
            int h = Math.round(hud.getHeight() * hud.getScale());

            hud.render(ctx, width, height);

            boolean active = hud == dragging || hud == selected || hud == hovered;
            int outlineColor = hud == dragging ? XTheme.ACCENT
                    : hud == selected ? XTheme.ACCENT
                    : hud == hovered ? XTheme.TEXT_SECONDARY
                    : 0x50FFFFFF;
            int boxX = Math.round(hud.getX()) - 2;
            int boxY = Math.round(hud.getY()) - 2;
            ctx.drawStrokedRectangle(boxX, boxY, w + 4, h + 4, outlineColor);

            if (active) {
                String label = hud.getName() + "  " + Math.round(hud.getScale() * 100) + "%";
                int labelY = Math.max(2, boxY - 10);
                ctx.drawText(textRenderer, label, boxX, labelY, XTheme.TEXT_PRIMARY, true);
            }
        }

        if (guideLineX != null) {
            int gx = Math.round(guideLineX);
            ctx.fill(gx, 0, gx + 1, height, RenderUtil.withAlpha(XTheme.ACCENT, 160));
        }
        if (guideLineY != null) {
            int gy = Math.round(guideLineY);
            ctx.fill(0, gy, width, gy + 1, RenderUtil.withAlpha(XTheme.ACCENT, 160));
        }

        renderToolbar(ctx, mouseX, mouseY);

        ctx.drawCenteredTextWithShadow(textRenderer,
                "Drag to reposition \u2022 Right-click for options \u2022 S snap \u2022 A align \u2022 Right Shift to close",
                width / 2, height - 14, XTheme.TEXT_SECONDARY);

        if (contextMenuTarget != null) {
            renderContextMenu(ctx, mouseX, mouseY);
        }
    }

    private void renderToolbar(DrawContext ctx, int mouseX, int mouseY) {
        ToolbarLayout t = toolbarLayout();

        RenderUtil.drawIcon(ctx, LOGO_ICON, t.iconX(), t.iconY(), t.iconSize());

        boolean settingsHovered = within(mouseX, mouseY, t.settingsX(), t.settingsY(), t.settingsW(), t.settingsH());
        RenderUtil.roundedRect(ctx, t.settingsX(), t.settingsY(), t.settingsW(), t.settingsH(), 8,
                settingsHovered ? XTheme.CARD_HOVER : XTheme.CARD_BG);
        String btnLabel = "\u2699 X SETTINGS";
        ctx.drawText(textRenderer, btnLabel,
                t.settingsX() + (t.settingsW() - textRenderer.getWidth(btnLabel)) / 2, t.settingsY() + 4,
                XTheme.TEXT_PRIMARY, false);
    }

    private void renderContextMenu(DrawContext ctx, int mouseX, int mouseY) {
        MenuBounds b = contextMenuBounds();
        RenderUtil.dropShadow(ctx, b.x(), b.y(), b.w(), b.h(), 8);
        RenderUtil.roundedRect(ctx, b.x(), b.y(), b.w(), b.h(), 8, XTheme.PANEL_BG);

        renderContextRow(ctx, "Reset", b.x(), b.resetY(), b.w(), mouseX, mouseY);
        renderContextRow(ctx, "Disable", b.x(), b.disableY(), b.w(), mouseX, mouseY);
    }

    private void renderContextRow(DrawContext ctx, String label, int x, int y, int w, int mouseX, int mouseY) {
        boolean hovered = within(mouseX, mouseY, x + 3, y, w - 6, CTX_ROW_H - 2);
        if (hovered) {
            RenderUtil.roundedRect(ctx, x + 3, y, w - 6, CTX_ROW_H - 2, 6, XTheme.CARD_HOVER);
        }
        ctx.drawText(textRenderer, label, x + 10, y + 5, XTheme.TEXT_PRIMARY, false);
    }

    private static boolean within(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    // ----------------------------------------------------------- alignment

    private float[] applyAlignmentGuides(HudModule dragged, float x, float y) {
        int w = Math.round(dragged.getWidth() * dragged.getScale());
        int h = Math.round(dragged.getHeight() * dragged.getScale());

        float left = x, right = x + w, centerX = x + w / 2f;
        float top = y, bottom = y + h, centerY = y + h / 2f;

        float bestX = x, bestDx = GUIDE_THRESHOLD;
        Float lineX = null;
        float bestY = y, bestDy = GUIDE_THRESHOLD;
        Float lineY = null;

        float screenCenterX = width / 2f;
        float screenCenterY = height / 2f;
        if (Math.abs(centerX - screenCenterX) < bestDx) {
            bestDx = Math.abs(centerX - screenCenterX);
            bestX = x + (screenCenterX - centerX);
            lineX = screenCenterX;
        }
        if (Math.abs(centerY - screenCenterY) < bestDy) {
            bestDy = Math.abs(centerY - screenCenterY);
            bestY = y + (screenCenterY - centerY);
            lineY = screenCenterY;
        }

        for (HudModule other : enabledHuds()) {
            if (other == dragged) continue;
            int ow = Math.round(other.getWidth() * other.getScale());
            int oh = Math.round(other.getHeight() * other.getScale());
            float oLeft = other.getX(), oRight = other.getX() + ow, oCenterX = other.getX() + ow / 2f;
            float oTop = other.getY(), oBottom = other.getY() + oh, oCenterY = other.getY() + oh / 2f;

            for (float candidate : new float[]{oLeft, oCenterX, oRight}) {
                for (float mine : new float[]{left, centerX, right}) {
                    float d = Math.abs(mine - candidate);
                    if (d < bestDx) {
                        bestDx = d;
                        bestX = x + (candidate - mine);
                        lineX = candidate;
                    }
                }
            }
            for (float candidate : new float[]{oTop, oCenterY, oBottom}) {
                for (float mine : new float[]{top, centerY, bottom}) {
                    float d = Math.abs(mine - candidate);
                    if (d < bestDy) {
                        bestDy = d;
                        bestY = y + (candidate - mine);
                        lineY = candidate;
                    }
                }
            }
        }

        guideLineX = lineX;
        guideLineY = lineY;
        return new float[]{bestX, bestY};
    }

    // ----------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) return super.mouseClicked(mouseX, mouseY, button);

        if (contextMenuTarget != null) {
            MenuBounds b = contextMenuBounds();
            if (button == 0 && within(mouseX, mouseY, b.x(), b.y(), b.w(), b.h())) {
                if (mouseY < b.disableY()) {
                    contextMenuTarget.resetLayout();
                } else {
                    contextMenuTarget.setEnabled(false);
                    if (selected == contextMenuTarget) selected = null;
                }
            }
            contextMenuTarget = null;
            return true;
        }

        if (button == 0) {
            ToolbarLayout t = toolbarLayout();
            if (within(mouseX, mouseY, t.settingsX(), t.settingsY(), t.settingsW(), t.settingsH())) {
                close();
                MinecraftClient.getInstance().setScreen(new ClickGUIScreen());
                return true;
            }
        }

        HudModule hit = hudAt(mouseX, mouseY);

        if (button == 1) {
            if (hit != null) {
                contextMenuTarget = hit;
                contextMenuRawX = (int) mouseX;
                contextMenuRawY = (int) mouseY;
                selected = hit;
            }
            return true;
        }

        if (hit != null) {
            dragging = hit;
            selected = hit;
            dragOffsetX = (float) (mouseX - hit.getX());
            dragOffsetY = (float) (mouseY - hit.getY());
            hit.setDragging(true);
            return true;
        }

        selected = null;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging != null) {
            float rawX = (float) (mouseX - dragOffsetX);
            float rawY = (float) (mouseY - dragOffsetY);

            float snappedX = snapEnabled ? Math.round(rawX / GRID_SIZE) * GRID_SIZE : rawX;
            float snappedY = snapEnabled ? Math.round(rawY / GRID_SIZE) * GRID_SIZE : rawY;

            guideLineX = null;
            guideLineY = null;
            if (guidesEnabled) {
                float[] aligned = applyAlignmentGuides(dragging, snappedX, snappedY);
                snappedX = aligned[0];
                snappedY = aligned[1];
            }

            dragging.setPosition(snappedX, snappedY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null) {
            dragging.setDragging(false);
            dragging = null;
            guideLineX = null;
            guideLineY = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        HudModule target = hudAt(mouseX, mouseY);
        if (target == null) target = selected;
        if (target != null) {
            target.adjustScale((float) (verticalAmount * SCROLL_STEP));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            XClientMod.suppressNextGuiKeyEdge();
            close();
            return true;
        }

        if (contextMenuTarget != null && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            contextMenuTarget = null;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_S) {
            snapEnabled = !snapEnabled;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_A) {
            guidesEnabled = !guidesEnabled;
            return true;
        }

        if (selected != null) {
            boolean fast = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
            float step = fast ? NUDGE_STEP_FAST : NUDGE_STEP;
            switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT -> {
                    selected.setPosition(selected.getX() - step, selected.getY());
                    return true;
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    selected.setPosition(selected.getX() + step, selected.getY());
                    return true;
                }
                case GLFW.GLFW_KEY_UP -> {
                    selected.setPosition(selected.getX(), selected.getY() - step);
                    return true;
                }
                case GLFW.GLFW_KEY_DOWN -> {
                    selected.setPosition(selected.getX(), selected.getY() + step);
                    return true;
                }
                case GLFW.GLFW_KEY_LEFT_BRACKET -> {
                    selected.adjustScale(-BRACKET_STEP);
                    return true;
                }
                case GLFW.GLFW_KEY_RIGHT_BRACKET -> {
                    selected.adjustScale(BRACKET_STEP);
                    return true;
                }
                case GLFW.GLFW_KEY_R -> {
                    selected.resetLayout();
                    return true;
                }
                default -> {
                    // fall through to super below
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        XClientMod.getConfigManager().save(XClientMod.getModuleManager());
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
