package com.omneria.fordix.notification;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The notifications the server sent recently - who sent them, to whom, what - kept in memory,
 * newest last, up to a cap. Plain Java, unit tested; the server writes each entry to a log file too.
 */
public final class NotificationHistory {
    public static final int CAPACITY = 200;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault());

    /** {@code recipients} is a label ("tous", "joueur Alex", "equipe rouge"...) and {@code count} how many got it. */
    public record Entry(long time, String sender, String style, String title, String message, String recipients, int count) {
    }

    private final Deque<Entry> entries = new ArrayDeque<>();

    public synchronized void add(Entry entry) {
        entries.addLast(entry);
        while (entries.size() > CAPACITY) {
            entries.removeFirst();
        }
    }

    /** The last {@code count} entries, newest first. */
    public synchronized List<Entry> latest(int count) {
        List<Entry> result = new ArrayList<>();
        var iterator = entries.descendingIterator();
        while (iterator.hasNext() && result.size() < count) {
            result.add(iterator.next());
        }
        return result;
    }

    public synchronized int size() {
        return entries.size();
    }

    /** One line for chat or the admin screen: "30/09 14:05 Alex -> tous (12) [info] Titre". */
    public static String format(Entry entry) {
        return TIME.format(Instant.ofEpochMilli(entry.time())) + " " + entry.sender() + " -> " + entry.recipients()
                + " (" + entry.count() + ") [" + entry.style() + "] " + entry.title();
    }
}
