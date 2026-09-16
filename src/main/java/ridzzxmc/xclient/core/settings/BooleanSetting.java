package ridzzxmc.xclient.core.settings;

public class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String name, boolean defaultValue) {
        super(name, defaultValue);
    }

    public void toggle() {
        setValue(!getValue());
    }
}
