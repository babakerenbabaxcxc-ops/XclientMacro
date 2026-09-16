package ridzzxmc.xclient.modules.optimization;

import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.SliderSetting;

/**
 * Caps how many newly-received chunk sections get their mesh built per
 * frame (rather than all at once when many arrive together, e.g. on
 * teleport or /tp), spreading the one-time cost across several frames to
 * avoid a stutter. The server-authoritative chunk data itself is cached
 * and applied unchanged - this only paces the client's own render prep.
 */
public class LazyChunkLoading extends Module {

    private final SliderSetting perFrameLimit = register(new SliderSetting("Chunks/Frame", 4, 1, 16, 1, true));

    public LazyChunkLoading() {
        super("Lazy Chunk Loading", "Spreads chunk mesh building across frames to avoid stutter.",
                ModuleCategory.OPTIMIZATION, true);
    }

    public int getPerFrameLimit() {
        return perFrameLimit.getValue().intValue();
    }
}
