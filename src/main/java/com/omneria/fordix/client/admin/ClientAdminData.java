package com.omneria.fordix.client.admin;

import java.util.List;

/**
 * The latest answers the server gave the admin screen. Each change bumps {@link #version()}, which
 * the screen polls to know when to refresh.
 */
public final class ClientAdminData {
    private static volatile List<String> panels = List.of();
    private static volatile List<String> panel = List.of();
    private static volatile List<String> history = List.of();
    private static volatile List<String> schedules = List.of();
    private static volatile List<String> templates = List.of();
    private static volatile List<String> pages = List.of();
    private static volatile List<String> page = List.of();
    private static volatile List<String> book = List.of();
    private static volatile String status = "";
    private static volatile boolean denied;
    private static volatile int version;

    private ClientAdminData() {
    }

    public static void receive(String kind, List<String> lines) {
        switch (kind) {
            case "panels" -> panels = List.copyOf(lines);
            case "panel" -> panel = List.copyOf(lines);
            case "history" -> history = List.copyOf(lines);
            case "schedules" -> schedules = List.copyOf(lines);
            case "pages" -> pages = List.copyOf(lines);
            case "page" -> page = List.copyOf(lines);
            case "book" -> book = List.copyOf(lines);
            case "templates" -> templates = List.copyOf(lines);
            case "status" -> status = lines.isEmpty() ? "" : lines.get(0);
            case "denied" -> {
                denied = true;
                status = lines.isEmpty() ? "" : lines.get(0);
            }
            default -> {
            }
        }
        version++;
    }

    public static void reset() {
        panels = List.of();
        panel = List.of();
        history = List.of();
        schedules = List.of();
        templates = List.of();
        pages = List.of();
        page = List.of();
        book = List.of();
        status = "";
        denied = false;
        version++;
    }

    public static List<String> panels() {
        return panels;
    }

    /** The panel last asked for: id, title, description, then "label TAB command" lines. */
    public static List<String> panel() {
        return panel;
    }

    /** "id TAB description" per scheduled notification. */
    public static List<String> schedules() {
        return schedules;
    }

    /** "name TAB style TAB seconds TAB title TAB message" per template. */
    public static List<String> templates() {
        return templates;
    }

    /** "id TAB title" per guide page. */
    public static List<String> pages() {
        return pages;
    }

    /** The guide page last asked for: id, title, body. */
    public static List<String> page() {
        return page;
    }

    /** The welcome book: the reward command, then one config-format line per step. */
    public static List<String> book() {
        return book;
    }

    public static List<String> history() {
        return history;
    }

    public static String status() {
        return status;
    }

    public static boolean denied() {
        return denied;
    }

    public static int version() {
        return version;
    }
}
