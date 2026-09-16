package ridzzxmc.xclient.core.settings;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Base class for a configurable module option, rendered as a widget in the ClickGUI. */
public abstract class Setting<T> {

    private final String name;
    protected T value;
    private Supplier<Boolean> visiblePredicate = () -> true;
    private Consumer<T> onChange;

    protected Setting(String name, T defaultValue) {
        this.name = name;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
        if (onChange != null) onChange.accept(value);
    }

    /**
     * Runs {@code callback} every time this setting's value changes (from the GUI,
     * a command, or config load alike) - e.g. {@code Waypoints} uses this on a
     * {@link TextSetting} to add a waypoint the moment its name field is confirmed,
     * without needing a dedicated "action" widget the GUI doesn't otherwise support.
     */
    public Setting<T> onChange(Consumer<T> callback) {
        this.onChange = callback;
        return this;
    }

    /** Hide this setting in the GUI unless the predicate is true (e.g. a slider only shown when a mode is active). */
    public Setting<T> visibleIf(Supplier<Boolean> predicate) {
        this.visiblePredicate = predicate;
        return this;
    }

    public boolean isVisible() {
        return visiblePredicate.get();
    }
}
