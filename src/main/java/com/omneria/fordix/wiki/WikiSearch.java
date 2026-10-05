package com.omneria.fordix.wiki;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Searches wiki pages: every word must be in the title or the text; titles weigh much more. Pure Java, unit tested. */
public final class WikiSearch {
    public record Hit(String wiki, String slug, String title, String snippet, int score) {
    }

    private WikiSearch() {
    }

    /** The score of a page for a query, 0 when a word is missing. */
    public static int score(String title, String body, String query) {
        String[] words = words(query);
        if (words.length == 0) {
            return 0;
        }
        String lowerTitle = title.toLowerCase(Locale.ROOT);
        String lowerBody = body.toLowerCase(Locale.ROOT);
        int score = 0;
        for (String word : words) {
            boolean inTitle = lowerTitle.contains(word);
            int count = count(lowerBody, word);
            if (!inTitle && count == 0) {
                return 0;
            }
            score += (inTitle ? 20 : 0) + (lowerTitle.equals(word) ? 30 : 0) + Math.min(count, 10);
        }
        return score;
    }

    /** About 90 characters of the text around the first word found, "..." marking what was cut. */
    public static String snippet(String body, String query) {
        String[] words = words(query);
        String text = body.replace('\n', ' ').replaceAll("[#@-]\\s?", "").replaceAll("\\s+", " ");
        String lower = text.toLowerCase(Locale.ROOT);
        int at = -1;
        for (String word : words) {
            int found = lower.indexOf(word);
            if (found >= 0 && (at < 0 || found < at)) {
                at = found;
            }
        }
        int start = Math.max(0, at - 30);
        int end = Math.min(text.length(), start + 90);
        return (start > 0 ? "…" : "") + text.substring(start, end).strip() + (end < text.length() ? "…" : "");
    }

    public static List<Hit> sorted(List<Hit> hits, int limit) {
        List<Hit> copy = new ArrayList<>(hits);
        copy.sort(Comparator.comparingInt(Hit::score).reversed().thenComparing(Hit::title));
        return copy.size() > limit ? copy.subList(0, limit) : copy;
    }

    private static String[] words(String query) {
        String trimmed = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        return trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
    }

    private static int count(String text, String word) {
        int count = 0;
        for (int at = text.indexOf(word); at >= 0 && count < 50; at = text.indexOf(word, at + word.length())) {
            count++;
        }
        return count;
    }
}
