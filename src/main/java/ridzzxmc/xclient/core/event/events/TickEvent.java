package ridzzxmc.xclient.core.event.events;

import ridzzxmc.xclient.core.event.Event;

/** Fired once per client tick (20/s), mirrors {@code ClientTickEvents.END_CLIENT_TICK}. */
public class TickEvent extends Event {

    public enum Phase { START, END }

    private final Phase phase;

    public TickEvent(Phase phase) {
        this.phase = phase;
    }

    public Phase getPhase() {
        return phase;
    }
}
