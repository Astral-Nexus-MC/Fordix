package com.omneria.fordix.client.chat;

import java.util.Locale;

/** The chat search: every word typed must appear in the line, in any order, ignoring case. Pure Java, unit tested. */
public final class ChatSearch {
    private ChatSearch() {
    }

    public static boolean matches(String text, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String haystack = text == null ? "" : text.toLowerCase(Locale.ROOT);
        for (String word : query.toLowerCase(Locale.ROOT).trim().split("\\s+")) {
            if (!haystack.contains(word)) {
                return false;
            }
        }
        return true;
    }
}
