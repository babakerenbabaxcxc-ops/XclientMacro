package ridzzxmc.xclient.core.module;

public enum ModuleCategory {
    HUD("HUD", 0xFFE53935),
    RENDER("Render", 0xFFE53935),
    COSMETIC("Cosmetic", 0xFFE53935),
    OPTIMIZATION("Optimization", 0xFFE53935),
    MOVEMENT("Movement", 0xFFE53935),
    WORLD("World", 0xFFE53935),
    UTILITY("Utility", 0xFFE53935),
    MISC("Misc", 0xFFE53935);

    private final String displayName;
    private final int accentColor;

    ModuleCategory(String displayName, int accentColor) {
        this.displayName = displayName;
        this.accentColor = accentColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getAccentColor() {
        return accentColor;
    }
}
