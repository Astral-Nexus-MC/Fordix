package com.omneria.fordix.wiki;

import java.util.ArrayList;
import java.util.List;

/** What the server answers to the client's wiki requests, read back from the text lines. Pure Java, unit tested. */
public final class WikiAnswers {
    public record Info(String id, String title, int pages, String source, String license) {
    }

    public record Entry(String slug, String title) {
    }

    public record Hit(String wiki, String slug, String title, String snippet) {
    }

    /** A page: its title, where it comes from, the wiki's licence and its text in the guide markup. */
    public record Page(String title, String source, String license, String body) {
    }

    private WikiAnswers() {
    }

    public static List<Info> parseList(String text) {
        List<Info> list = new ArrayList<>();
        for (String line : text.split("\n")) {
            String[] parts = line.split("\t", -1);
            if (parts.length >= 5) {
                list.add(new Info(parts[0], parts[1], parseInt(parts[2]), parts[3], parts[4]));
            }
        }
        return list;
    }

    public static List<Entry> parseIndex(String text) {
        List<Entry> list = new ArrayList<>();
        for (String line : text.split("\n")) {
            String[] parts = line.split("\t", -1);
            if (parts.length >= 2 && !parts[0].isEmpty()) {
                list.add(new Entry(parts[0], parts[1]));
            }
        }
        return list;
    }

    public static List<Hit> parseHits(String text) {
        List<Hit> list = new ArrayList<>();
        for (String line : text.split("\n")) {
            String[] parts = line.split("\t", -1);
            if (parts.length >= 4) {
                list.add(new Hit(parts[0], parts[1], parts[2], parts[3]));
            }
        }
        return list;
    }

    public static Page parsePage(String text) {
        String[] parts = text.split("\n", 5);
        if (parts.length < 5 || !parts[3].equals("---")) {
            return new Page(parts.length > 0 ? parts[0] : "", "", "", text);
        }
        return new Page(parts[0], parts[1], parts[2], parts[4]);
    }

    private static int parseInt(String text) {
        try {
            return Integer.parseInt(text.strip());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
