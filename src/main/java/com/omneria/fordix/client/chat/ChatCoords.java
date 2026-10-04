package com.omneria.fordix.client.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Finds block coordinates written in a chat line ("120 64 -300", "X: 10, Y: 70, Z: 20"). Pure Java, unit tested. */
public final class ChatCoords {
    /** One match: where it sits in the text and the three numbers. */
    public record Found(int start, int end, int x, int y, int z) {
    }

    private static final Pattern COORDS = Pattern.compile(
            "(?<![\\w.-])(x[:=]?\\s*)?(-?\\d{1,7})(?:\\.\\d+)?[\\s,;]+(y[:=]?\\s*)?(-?\\d{1,4})(?:\\.\\d+)?[\\s,;]+(z[:=]?\\s*)?(-?\\d{1,7})(?:\\.\\d+)?(?![\\w.])",
            Pattern.CASE_INSENSITIVE);

    /** Prefix of the click value that carries a place; the chat catches it instead of copying it. */
    public static final String CLICK_PREFIX = "fordix-marker:";

    private ChatCoords() {
    }

    public static String clickValue(Found found) {
        return CLICK_PREFIX + found.x() + "," + found.y() + "," + found.z();
    }

    /** {x, y, z} of a click value made by {@link #clickValue}, or null. */
    public static int[] parseClick(String value) {
        if (value == null || !value.startsWith(CLICK_PREFIX)) {
            return null;
        }
        String[] parts = value.substring(CLICK_PREFIX.length()).split(",");
        try {
            return parts.length == 3 ? new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])} : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static List<Found> find(String text) {
        List<Found> found = new ArrayList<>();
        Matcher matcher = COORDS.matcher(text == null ? "" : text);
        while (matcher.find()) {
            int x = Integer.parseInt(matcher.group(2));
            int y = Integer.parseInt(matcher.group(4));
            int z = Integer.parseInt(matcher.group(6));
            boolean labeled = matcher.group(1) != null || matcher.group(3) != null || matcher.group(5) != null;
            // Without labels, three small numbers are far more likely a count than a place.
            if (y >= -64 && y <= 400 && (labeled || Math.abs(x) >= 16 || Math.abs(z) >= 16)) {
                found.add(new Found(matcher.start(), matcher.end(), x, y, z));
            }
        }
        return found;
    }
}
