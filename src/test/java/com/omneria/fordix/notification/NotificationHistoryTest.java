package com.omneria.fordix.notification;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationHistoryTest {

    private static NotificationHistory.Entry entry(int n) {
        return new NotificationHistory.Entry(1_000_000L * n, "Alex", "info", "Titre " + n, "msg", "tous", n);
    }

    @Test
    void latestReturnsNewestFirst() {
        NotificationHistory history = new NotificationHistory();
        history.add(entry(1));
        history.add(entry(2));
        history.add(entry(3));
        List<NotificationHistory.Entry> latest = history.latest(2);
        assertEquals(2, latest.size());
        assertEquals("Titre 3", latest.get(0).title());
        assertEquals("Titre 2", latest.get(1).title());
    }

    @Test
    void oldestEntriesAreDroppedPastTheCapacity() {
        NotificationHistory history = new NotificationHistory();
        for (int i = 1; i <= NotificationHistory.CAPACITY + 5; i++) {
            history.add(entry(i));
        }
        assertEquals(NotificationHistory.CAPACITY, history.size());
        assertEquals("Titre " + (NotificationHistory.CAPACITY + 5), history.latest(1).get(0).title());
    }

    @Test
    void formatNamesSenderRecipientsAndTitle() {
        String line = NotificationHistory.format(entry(7));
        assertTrue(line.contains("Alex -> tous (7) [info] Titre 7"), line);
    }
}
