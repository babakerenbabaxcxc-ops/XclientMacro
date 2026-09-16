package ridzzxmc.xclient.core.settings;

public class ColorSetting extends Setting<Integer> {

    /** @param defaultArgb packed 0xAARRGGBB */
    public ColorSetting(String name, int defaultArgb) {
        super(name, defaultArgb);
    }

    public int getAlpha() {
        return (getValue() >> 24) & 0xFF;
    }

    public int getRed() {
        return (getValue() >> 16) & 0xFF;
    }

    public int getGreen() {
        return (getValue() >> 8) & 0xFF;
    }

    public int getBlue() {
        return getValue() & 0xFF;
    }

    public void setRgba(int a, int r, int g, int b) {
        setValue((a << 24) | (r << 16) | (g << 8) | b);
    }
}
