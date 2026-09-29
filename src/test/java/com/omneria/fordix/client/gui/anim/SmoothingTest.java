package com.omneria.fordix.client.gui.anim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmoothingTest {

    @Test
    void movesTowardTheTargetWithoutOvershooting() {
        double next = Smoothing.step(0, 100, 0.016, 14);
        assertTrue(next > 0 && next < 100);
    }

    @Test
    void noTimePassedMeansNoMovement() {
        assertEquals(5.0, Smoothing.step(5, 100, 0, 14), 1e-9);
    }

    @Test
    void oneBigStepEqualsManySmallOnes() {
        double big = Smoothing.step(0, 100, 0.5, 10);
        double small = 0;
        for (int i = 0; i < 50; i++) {
            small = Smoothing.step(small, 100, 0.01, 10);
        }
        assertEquals(big, small, 1e-6);
    }

    @Test
    void scrollRangeKeepsLargeContentInView() {
        double[] range = Smoothing.scrollRange(0, 1000, 300, 20);
        assertEquals(-20, range[0], 1e-9);
        assertEquals(720, range[1], 1e-9);
    }

    @Test
    void smallContentIsCentered() {
        double[] range = Smoothing.scrollRange(0, 100, 300, 0);
        assertEquals(range[0], range[1], 1e-9);
        assertEquals(-100, range[0], 1e-9);
    }
}
