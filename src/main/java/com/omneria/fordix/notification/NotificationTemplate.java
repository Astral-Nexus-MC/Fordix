package com.omneria.fordix.notification;

import java.util.Map;

/**
 * A notification written in the config as {@code "style | Title | message"}, with
 * {placeholders} filled in when it's sent. Style and message are optional: a bare
 * "Title" is an info notification with no message. Plain Java, unit tested.
 */
public record NotificationTemplate(NotificationStyle style, String title, String message) {

    /** Parses a config line; null when it has no title or names an unknown style. */
    public static NotificationTemplate parse(String line) {
        if (line == null) {
            return null;
        }
        String[] parts = line.split("\\|", 3);
        String first = parts[0].trim();
        NotificationStyle style = NotificationStyle.byId(first);
        if (parts.length == 1) {
            return first.isEmpty() ? null : new NotificationTemplate(NotificationStyle.INFO, first, "");
        }
        if (style == null) {
            // "Title | message" without a style.
            String title = first;
            String message = parts.length == 3 ? parts[1].trim() + " | " + parts[2].trim() : parts[1].trim();
            return title.isEmpty() ? null : new NotificationTemplate(NotificationStyle.INFO, title, message);
        }
        String title = parts[1].trim();
        String message = parts.length == 3 ? parts[2].trim() : "";
        return title.isEmpty() ? null : new NotificationTemplate(style, title, message);
    }

    /** Replaces each {key} with its value; unknown placeholders are left as written. */
    public static String fill(String text, Map<String, String> values) {
        String result = text;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    public NotificationTemplate filled(Map<String, String> values) {
        return new NotificationTemplate(style, fill(title, values), fill(message, values));
    }
}
