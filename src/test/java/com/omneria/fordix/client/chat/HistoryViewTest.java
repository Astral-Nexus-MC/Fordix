package com.omneria.fordix.client.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoryViewTest {

    @Test
    void notBrowsingShowsNothing() {
        assertTrue(HistoryView.window(14, 14, 3).empty());
        assertTrue(HistoryView.window(-1, 14, 3).empty());
        assertTrue(HistoryView.window(0, 0, 3).empty());
    }

    @Test
    void theWindowEndsOnTheSelectedLine() {
        HistoryView.Window window = HistoryView.window(9, 14, 3);
        assertEquals(7, window.from());
        assertEquals(10, window.to());
        assertEquals(9, window.selected());
    }

    @Test
    void nearTheOldestLineTheWindowIsShorter() {
        HistoryView.Window window = HistoryView.window(1, 14, 3);
        assertEquals(0, window.from());
        assertEquals(2, window.to());
    }

    @Test
    void theCounterCountsFromTheOldest() {
        assertEquals("3 / 14", HistoryView.counter(2, 14));
    }
}
