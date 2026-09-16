package ridzzxmc.xclient.modules.misc;

import ridzzxmc.xclient.core.module.Module;
import ridzzxmc.xclient.core.module.ModuleCategory;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps every chat line you've received this session in a local list you
 * can scroll back through or search - purely a convenience log, doesn't
 * read anything the client wasn't already shown in chat.
 */
public class MessageLogger extends Module {

    private final List<String> log = new ArrayList<>();

    public MessageLogger() {
        super("Message Logger", "Keeps a scrollable local log of chat messages.", ModuleCategory.MISC, true);
    }

    public void log(String message) {
        log.add(message);
    }

    public List<String> getLog() {
        return log;
    }
}
