package ridzzxmc.xclient.modules.hud;

import net.minecraft.client.gui.DrawContext;
import ridzzxmc.xclient.core.settings.BooleanSetting;
import ridzzxmc.xclient.gui.theme.XTheme;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ClockHUD extends HudModule {

    private final BooleanSetting militaryTime = register(new BooleanSetting("24-Hour", false));

    public ClockHUD() {
        super("Clock", "Displays your system's real-world time.", 6, 24);
    }

    @Override
    protected void renderContent(DrawContext ctx, int screenWidth, int screenHeight) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(militaryTime.getValue() ? "HH:mm:ss" : "hh:mm:ss a");
        String time = LocalTime.now().format(fmt);

        int width = mc.textRenderer.getWidth(time) + 12;
        ctx.fill(0, 0, width, 14, XTheme.PANEL_BG);
        ctx.fill(0, 0, 3, 14, XTheme.ACCENT);
        ctx.drawText(mc.textRenderer, time, 6, 3, XTheme.TEXT_PRIMARY, true);
    }

    @Override
    public int getWidth() {
        return 70;
    }

    @Override
    public int getHeight() {
        return 14;
    }
}
