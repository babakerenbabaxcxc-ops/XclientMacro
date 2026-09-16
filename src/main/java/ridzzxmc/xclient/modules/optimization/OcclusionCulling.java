package ridzzxmc.xclient.modules.optimization;

import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;

/**
 * Complements {@link ChunkCuller}'s CPU-side heuristic with GPU occlusion
 * queries for the remaining borderline sections, skipping the draw call
 * entirely when the query reports zero visible samples last frame.
 */
public class OcclusionCulling extends Module {

    public OcclusionCulling() {
        super("Occlusion Culling", "Uses GPU occlusion queries to skip drawing hidden chunk sections.",
                ModuleCategory.OPTIMIZATION, true);
    }
}
