package ridzzxmc.xclient.core.settings;

public class SliderSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double step;
    private final boolean isInteger;

    public SliderSetting(String name, double defaultValue, double min, double max, double step) {
        this(name, defaultValue, min, max, step, false);
    }

    public SliderSetting(String name, double defaultValue, double min, double max, double step, boolean isInteger) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
        this.isInteger = isInteger;
    }

    /** Clamps (and, for integer sliders, rounds) any incoming value to the valid [min,max] range. */
    @Override
    public void setValue(Double value) {
        if (value == null || value.isNaN()) {
            super.setValue(min);
            return;
        }
        double clamped = Math.max(min, Math.min(max, value));
        super.setValue(isInteger ? Math.round(clamped) * 1.0 : clamped);
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    public boolean isInteger() {
        return isInteger;
    }

    /** Clamp + snap-to-step, called by the GUI slider widget while dragging. */
    public void setFromRatio(double ratio) {
        double raw = min + (max - min) * Math.max(0, Math.min(1, ratio));
        double snapped = Math.round(raw / step) * step;
        setValue(isInteger ? Math.round(snapped) * 1.0 : snapped);
    }

    public double getRatio() {
        return (getValue() - min) / (max - min);
    }
}
