package ridzzxmc.xclient.modules.optimization;

import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;
import ridzzxmc.xclient.core.settings.SliderSetting;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runs chunk section mesh-building on a dedicated worker pool instead of the
 * client render thread, then uploads the finished buffers on the next frame.
 * Building a mesh only reads already-generated chunk data (never writes
 * blocks or triggers generation), so world seed/output is bit-for-bit
 * identical with or without this module - it only changes *when* the mesh
 * for a chunk becomes ready, not what it contains.
 */
public class AsyncChunkBuilder extends Module {

    private final SliderSetting workerThreads = register(new SliderSetting("Worker Threads", 2, 1, 8, 1, true));
    private ExecutorService executor;

    public AsyncChunkBuilder() {
        super("Async Chunk Builder", "Builds chunk meshes on background threads instead of blocking render.",
                ModuleCategory.OPTIMIZATION, true);
    }

    @Override
    protected void onEnable() {
        int threads = Math.max(1, workerThreads.getValue().intValue());
        executor = Executors.newFixedThreadPool(threads, r -> new Thread(r, "xclient-chunk-builder"));
    }

    @Override
    protected void onDisable() {
        if (executor != null) executor.shutdown();
    }

    public ExecutorService getExecutor() {
        return executor;
    }
}
