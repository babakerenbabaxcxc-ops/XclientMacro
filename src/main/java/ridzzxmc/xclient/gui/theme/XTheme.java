package ridzzxmc.xclient.gui.theme;

/**
 * Single source of truth for the "X Client" red aesthetic.
 * Change values here to re-theme the whole client.
 */
public final class XTheme {

    private XTheme() {}

    public static final int ACCENT          = 0xFFE53935; // primary red
    public static final int ACCENT_DIM      = 0xFF8F2422;
    public static final int ACCENT_GLOW     = 0x55E53935;

    public static final int BACKGROUND      = 0xE6141014; // near-black w/ red tint, translucent
    public static final int PANEL_BG        = 0xF01A1418;
    public static final int CARD_BG         = 0xEE1F181B;
    public static final int CARD_HOVER      = 0xEE2A1D20;

    public static final int TEXT_PRIMARY    = 0xFFF5F0F0;
    public static final int TEXT_SECONDARY  = 0xFFAFA0A2;
    public static final int TEXT_DISABLED   = 0xFF6E6265;

    public static final int TOGGLE_ON       = ACCENT;
    public static final int TOGGLE_OFF      = 0xFF3A2E31;

    public static final int SUCCESS         = 0xFF43C463;
    public static final int WARNING         = 0xFFFFB020;
    public static final int ERROR           = 0xFFE53935;

    public static final String FONT_REGULAR = "xclient:inter_regular";
    public static final String FONT_MEDIUM  = "xclient:inter_medium";
    public static final String FONT_BOLD    = "xclient:inter_bold";
}
