package com.omneria.fordix.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AlertLatchTest {
    @Test
    void anAlertFiresOnceUntilItClears() {
        AlertLatch latch = new AlertLatch();
        assertFalse(latch.rise("a", false));
        assertTrue(latch.rise("a", true));
        assertFalse(latch.rise("a", true));
        assertFalse(latch.rise("a", false));
        assertTrue(latch.rise("a", true));
        assertTrue(latch.rise("b", true));
    }
}
