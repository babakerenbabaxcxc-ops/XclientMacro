package ridzzxmc.xclient.modules.utility;

import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lets the user define short text snippets they can send with one keypress
 * (e.g. "/spawn", "thanks!", a shop link) - a chat macro, not an
 * auto-spammer; each snippet sends exactly once per keypress.
 */
public class AutoText extends Module {

    private final Map<Integer, String> snippets = new LinkedHashMap<>();

    public AutoText() {
        super("Auto Text", "Bind short chat snippets to keys for quick sending.", ModuleCategory.UTILITY);
    }

    public void bindSnippet(int key, String text) {
        snippets.put(key, text);
    }

    public Map<Integer, String> getSnippets() {
        return snippets;
    }
}
