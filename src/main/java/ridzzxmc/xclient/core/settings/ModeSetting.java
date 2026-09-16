package ridzzxmc.xclient.core.settings;

import java.util.List;

public class ModeSetting extends Setting<String> {

    private final List<String> options;

    public ModeSetting(String name, List<String> options, String defaultValue) {
        super(name, defaultValue);
        this.options = options;
    }

    public List<String> getOptions() {
        return options;
    }

    public void cycleNext() {
        int index = options.indexOf(getValue());
        setValue(options.get((index + 1) % options.size()));
    }

    public void cyclePrevious() {
        int index = options.indexOf(getValue());
        setValue(options.get((index - 1 + options.size()) % options.size()));
    }
}
