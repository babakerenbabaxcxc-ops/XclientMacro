package ridzzxmc.xclient.core.notification;

import java.util.LinkedList;
import java.util.List;

/** Toast-style notification queue, rendered top-right by {@code ToastRenderer} in the HUD layer. */
public class NotificationManager {

    public static final NotificationManager INSTANCE = new NotificationManager();

    private final LinkedList<Notification> active = new LinkedList<>();
    private static final int MAX_VISIBLE = 4;

    private NotificationManager() {}

    public void push(String title, String message, NotificationType type) {
        push(title, message, type, 3500);
    }

    public void push(String title, String message, NotificationType type, long durationMs) {
        active.addFirst(new Notification(title, message, type, durationMs));
        while (active.size() > MAX_VISIBLE) {
            active.removeLast();
        }
    }

    public void tick() {
        active.removeIf(Notification::isExpired);
    }

    public List<Notification> getActive() {
        return active;
    }
}
