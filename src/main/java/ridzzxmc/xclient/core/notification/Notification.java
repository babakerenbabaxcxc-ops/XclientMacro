package ridzzxmc.xclient.core.notification;

public class Notification {

    private final String title;
    private final String message;
    private final NotificationType type;
    private final long createdAt = System.currentTimeMillis();
    private final long durationMs;

    public Notification(String title, String message, NotificationType type, long durationMs) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.durationMs = durationMs;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public NotificationType getType() {
        return type;
    }

    public long getAge() {
        return System.currentTimeMillis() - createdAt;
    }

    public boolean isExpired() {
        return getAge() > durationMs;
    }

    /** 0..1, eased fade in over 200ms, hold, fade out over the last 300ms. */
    public float getOpacity() {
        long age = getAge();
        if (age < 200) return age / 200f;
        long remaining = durationMs - age;
        if (remaining < 300) return Math.max(0f, remaining / 300f);
        return 1f;
    }
}
