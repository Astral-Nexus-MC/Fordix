package com.omneria.fordix.client.notification;

import java.util.List;
import java.util.Locale;

/**
 * Screen corner the notification stack hangs from, picked with the client option
 * "notificationPosition". "auto" is the top-right corner. Plain Java, unit tested.
 */
public enum NotificationCorner {
    TOP_RIGHT(true, true),
    TOP_LEFT(true, false),
    BOTTOM_RIGHT(false, true),
    BOTTOM_LEFT(false, false);

    public static final String AUTO = "auto";
    public static final List<String> OPTIONS = List.of(AUTO, "top_right", "top_left", "bottom_right", "bottom_left");

    private final boolean top;
    private final boolean right;

    NotificationCorner(boolean top, boolean right) {
        this.top = top;
        this.right = right;
    }

    public boolean isTop() {
        return top;
    }

    public boolean isRight() {
        return right;
    }

    /** The configured corner; "auto" (or anything unknown) is top-right. */
    public static NotificationCorner resolve(String option) {
        if (option != null && !option.equals(AUTO)) {
            for (NotificationCorner corner : values()) {
                if (corner.name().toLowerCase(Locale.ROOT).equals(option)) {
                    return corner;
                }
            }
        }
        return TOP_RIGHT;
    }
}
