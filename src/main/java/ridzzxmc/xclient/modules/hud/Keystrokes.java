package ridzzxmc.xclient.modules.hud;

import net.minecraft.client.gui.DrawContext;
import ridzzxmc.xclient.gui.theme.XTheme;
import org.lwjgl.glfw.GLFW;

public class Keystrokes extends HudModule {

    private static final int KEY_SIZE = 20;

    public Keystrokes() {
        super("Keystrokes", "Shows WASD, space and mouse button presses.", 6, 96);
    }

    @Override
    protected void renderContent(DrawContext ctx, int screenWidth, int screenHeight) {
        long handle = mc.getWindow().getHandle();

        drawKey(ctx, KEY_SIZE, 0, "W", GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS);
        drawKey(ctx, 0, KEY_SIZE, "A", GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS);
        drawKey(ctx, KEY_SIZE, KEY_SIZE, "S", GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS);
        drawKey(ctx, KEY_SIZE * 2, KEY_SIZE, "D", GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS);

        boolean leftDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rightDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        drawWideKey(ctx, 0, KEY_SIZE * 2, KEY_SIZE * 3, "LMB", leftDown);
        drawWideKey(ctx, 0, KEY_SIZE * 2 + KEY_SIZE + 2, KEY_SIZE * 3, "RMB", rightDown);
    }

    private void drawKey(DrawContext ctx, int x, int y, String label, boolean pressed) {
        int bg = pressed ? XTheme.ACCENT : XTheme.CARD_BG;
        ctx.fill(x, y, x + KEY_SIZE - 2, y + KEY_SIZE - 2, bg);
        int textWidth = mc.textRenderer.getWidth(label);
        ctx.drawText(mc.textRenderer, label, x + (KEY_SIZE - 2 - textWidth) / 2, y + 6,
                pressed ? 0xFFFFFFFF : XTheme.TEXT_SECONDARY, false);
    }

    private void drawWideKey(DrawContext ctx, int x, int y, int width, String label, boolean pressed) {
        int bg = pressed ? XTheme.ACCENT : XTheme.CARD_BG;
        ctx.fill(x, y, x + width - 2, y + KEY_SIZE - 2, bg);
        int textWidth = mc.textRenderer.getWidth(label);
        ctx.drawText(mc.textRenderer, label, x + (width - 2 - textWidth) / 2, y + 6,
                pressed ? 0xFFFFFFFF : XTheme.TEXT_SECONDARY, false);
    }

    @Override
    public int getWidth() {
        return KEY_SIZE * 3;
    }

    @Override
    public int getHeight() {
        return KEY_SIZE * 2 + KEY_SIZE * 2 + 4;
    }
}
