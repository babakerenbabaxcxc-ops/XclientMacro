package ridzzxmc.xclient.core.event.events;

import ridzzxmc.xclient.core.event.Event;

/** Fired on raw key presses, before keybind resolution. Used by the CPS counter, Keystrokes HUD, etc. */
public class InputEvent extends Event {

    public enum Action { PRESS, RELEASE, REPEAT }

    private final int key;
    private final Action action;
    private final boolean isMouse;

    public InputEvent(int key, Action action, boolean isMouse) {
        this.key = key;
        this.action = action;
        this.isMouse = isMouse;
    }

    public int getKey() {
        return key;
    }

    public Action getAction() {
        return action;
    }

    public boolean isMouse() {
        return isMouse;
    }
}
