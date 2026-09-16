package ridzzxmc.xclient.modules.optimization;

import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.SliderSetting;

/**
 * Reduces animation update frequency (texture/model animations, armor stand
 * limb swing interpolation, etc.) for entities far from the camera. Nearby
 * entities keep full-rate animation; the visual difference at range is
 * imperceptible but the CPU savings compound in crowded servers.
 */
public class SmartAnimations extends Module {

    private final SliderSetting throttleDistance = register(new SliderSetting("Throttle Distance", 32, 8, 96, 4, true));

    public SmartAnimations() {
        super("Smart Animations", "Throttles animation updates for distant entities.",
                ModuleCategory.OPTIMIZATION, true);
    }

    public double getThrottleDistance() {
        return throttleDistance.getValue();
    }
}
