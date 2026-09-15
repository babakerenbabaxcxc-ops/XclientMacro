package ridzzxmc.xclient.core.event.events;

import ridzzxmc.xclient.core.event.Event;

/**
 * Fired when the client joins or leaves a server, mirrors
 * {@code ClientPlayConnectionEvents.JOIN}/{@code DISCONNECT}. Kept as its own
 * event (rather than reusing {@link TickEvent}'s phase style) since modules
 * that care about connection lifecycle (Auto Reconnect, session stats, ...)
 * need the server address, which a tick event has no room for.
 */
public class ConnectionEvent extends Event {

    public enum Phase { JOIN, DISCONNECT }

    private final Phase phase;
    private final String serverAddress;

    public ConnectionEvent(Phase phase, String serverAddress) {
        this.phase = phase;
        this.serverAddress = serverAddress;
    }

    public Phase getPhase() {
        return phase;
    }

    /** May be null - e.g. singleplayer, or a disconnect that happened before any server info was recorded. */
    public String getServerAddress() {
        return serverAddress;
    }
}
