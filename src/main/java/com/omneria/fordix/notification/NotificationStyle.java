package com.omneria.fordix.notification;

import java.util.Locale;

/** Kind of notification; the client picks the stripe color and icon from it. */
public enum NotificationStyle {
    INFO,
    SUCCESS,
    WARNING,
    ERROR;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The style with this id ("info", "success"...), or null if there is none. */
    public static NotificationStyle byId(String id) {
        for (NotificationStyle style : values()) {
            if (style.id().equals(id)) {
                return style;
            }
        }
        return null;
    }
}
