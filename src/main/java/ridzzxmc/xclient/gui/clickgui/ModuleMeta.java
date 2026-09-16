package ridzzxmc.xclient.gui.clickgui;

import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.modules.hud.*;
import ridzzxmc.xclient.modules.optimization.SmartAnimations;
import ridzzxmc.xclient.modules.render.XClientIcon;
import ridzzxmc.xclient.modules.utility.ToolWarning;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Presentation data for the ClickGUI mod-menu cards: which filter tabs
 * (currently just New) a module shows up under. The mod menu is text-only
 * (no per-module icon textures), so this only tracks tags now.
 * <p>
 * This is intentionally kept OUTSIDE {@link Module} and keyed by class
 * (not by display name) so that a typo here can never silently point at the
 * wrong module (a wrong class reference fails to compile; a wrong string
 * would not).
 */
public final class ModuleMeta {

    public enum Tag {
        NEW
    }

    private static final Map<Class<? extends Module>, Set<Tag>> TAGS = new HashMap<>();

    private ModuleMeta() {
    }

    private static void tags(Class<? extends Module> cls, Tag... tagList) {
        if (tagList.length == 0) return;
        TAGS.put(cls, EnumSet.copyOf(Arrays.asList(tagList)));
    }

    static {
        // ---------------- HUD ----------------
        tags(DayCounter.class, Tag.NEW);

        // ---------------- RENDER ----------------
        tags(XClientIcon.class, Tag.NEW);

        // ---------------- OPTIMIZATION ----------------
        tags(SmartAnimations.class, Tag.NEW);

        // ---------------- UTILITY ----------------
        tags(ToolWarning.class, Tag.NEW);
    }

    public static Set<Tag> tagsFor(Module module) {
        return TAGS.getOrDefault(module.getClass(), Set.of());
    }

    public static boolean isNew(Module module) {
        return tagsFor(module).contains(Tag.NEW);
    }
}
