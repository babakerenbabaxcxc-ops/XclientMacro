package ridzzxmc.xclient.core.settings;

/**
 * Free-text setting (module names, server passwords, webhook URLs, ...).
 * Edited inline in {@code ModuleSettingsPanel} using the same "click a row,
 * type, Enter to confirm" approach {@code ClickGUIScreen} already uses for
 * its search box, rather than spawning a separate vanilla
 * {@code TextFieldWidget} screen - keeps the whole settings panel one
 * self-contained widget instead of splitting text editing off into its own
 * focus/lifecycle to manage.
 */
public class TextSetting extends Setting<String> {

    private final int maxLength;
    private final boolean masked;

    public TextSetting(String name, String defaultValue, int maxLength) {
        this(name, defaultValue, maxLength, false);
    }

    /** @param masked if true, the panel renders the value as bullets (e.g. for a saved password) instead of plain text. */
    public TextSetting(String name, String defaultValue, int maxLength, boolean masked) {
        super(name, defaultValue == null ? "" : defaultValue);
        this.maxLength = maxLength;
        this.masked = masked;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public boolean isMasked() {
        return masked;
    }
}
