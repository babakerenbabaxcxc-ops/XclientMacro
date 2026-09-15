package ridzzxmc.xclient.core.event;

/**
 * Base class for every event dispatched on the {@link EventBus}.
 * Events are cancellable by default; modules that only observe
 * (most HUD/render modules) simply never call {@link #cancel()}.
 */
public abstract class Event {

    private boolean cancelled = false;

    public void cancel() {
        this.cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
