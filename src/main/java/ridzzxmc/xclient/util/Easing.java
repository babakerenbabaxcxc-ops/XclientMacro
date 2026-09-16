package ridzzxmc.xclient.util;

/** Standard easing curves used for ClickGUI toggles, card hover, and toast fades. */
public final class Easing {

    private Easing() {}

    public static double easeInOutQuad(double t) {
        return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
    }

    public static double easeInOutCubic(double t) {
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    public static double easeOutBack(double t) {
        double c1 = 1.70158;
        double c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    }

    public static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }

    public static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }
}
