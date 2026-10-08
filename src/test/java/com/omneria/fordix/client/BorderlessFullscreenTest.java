package com.omneria.fordix.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BorderlessFullscreenTest {
    @Test
    void findsTheMonitorThatHoldsAPoint() {
        int[] left = {0, 0, 1920, 1080};
        int[] right = {1920, 0, 2560, 1440};
        assertTrue(BorderlessFullscreen.contains(left, 960, 540));
        assertFalse(BorderlessFullscreen.contains(left, 1920, 540));
        assertTrue(BorderlessFullscreen.contains(right, 1920, 0));
        assertFalse(BorderlessFullscreen.contains(right, 1919, 0));
    }
}
