package ridzzxmc.xclient.modules.world;

import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;

/**
 * Hides rain/snow particle and screen-darkening rendering client-side only -
 * server-side weather state, crop growth, and lightning mechanics are
 * completely unaffected, this only changes what gets drawn on your screen.
 */
public class NoWeather extends Module {

    public NoWeather() {
        super("No Weather", "Hides rain and snow rendering client-side.", ModuleCategory.WORLD);
    }
}
