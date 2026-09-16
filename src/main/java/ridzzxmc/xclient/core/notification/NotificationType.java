package ridzzxmc.xclient.core.notification;

public enum NotificationType {
    INFO(0xFF3D8BFF),
    SUCCESS(0xFF43C463),
    WARNING(0xFFFFB020),
    ERROR(0xFFE53935);

    private final int color;

    NotificationType(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }
}
