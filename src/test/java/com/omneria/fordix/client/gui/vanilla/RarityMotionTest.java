package com.omneria.fordix.client.gui.vanilla;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RarityMotionTest {
    @Test
    void onlyEpicAndLegendaryHaveAShine() {
        assertFalse(RarityMotion.hasSweep(2));
        assertTrue(RarityMotion.hasSweep(3));
        assertEquals(-1.0D, RarityMotion.sweep(2, 4.0D, 0));
    }

    @Test
    void theShineIsStillMostOfTheCycleAndPassesAtTheEnd() {
        // Epic: a 5 s cycle, passing in its last 36 %.
        assertEquals(-1.0D, RarityMotion.sweep(3, 0.5D, 0));
        assertEquals(-1.0D, RarityMotion.sweep(3, 3.0D, 0));
        assertEquals(0.0D, RarityMotion.sweep(3, 3.2001D, 0), 1e-3);
        assertEquals(0.5D, RarityMotion.sweep(3, 4.1D, 0), 1e-9);
        // Legendary passes more often.
        assertTrue(RarityMotion.sweep(4, 2.4D, 0) > 0);
    }

    @Test
    void theDelayShiftsTheCycleAndNeverGoesNegative() {
        assertEquals(RarityMotion.sweep(3, 4.1D, 0), RarityMotion.sweep(3, 4.1D - 1.0D, 1.0D), 1e-9);
        assertTrue(RarityMotion.sweep(3, 0.0D, -1.0D) >= -1.0D);
    }

    @Test
    void slotsDoNotShareTheirDelay() {
        assertEquals(0.0D, RarityMotion.delayOf(0));
        assertTrue(RarityMotion.delayOf(5) != RarityMotion.delayOf(6));
        assertTrue(RarityMotion.delayOf(100) < 3.0D);
    }

    @Test
    void theBreathStaysBetweenZeroAndItsPeak() {
        for (double t = 0; t < 5; t += 0.1) {
            double v = RarityMotion.breath(t, 0);
            assertTrue(v >= -1e-9 && v <= 0.95D + 1e-9);
        }
        assertEquals(0.0D, RarityMotion.breath(0, 0), 1e-9);
        assertEquals(0.95D, RarityMotion.breath(1.2D, 0), 1e-9);
    }

    @Test
    void theSparklesTwinkleWithinZeroAndOne() {
        for (double t = 0; t < 4; t += 0.05) {
            double v = RarityMotion.twinkle(t, 0.3D, 2);
            assertTrue(v >= 0.0D && v <= 1.0D + 1e-9, "at " + t + " : " + v);
        }
        assertEquals(1.0D, RarityMotion.twinkle(0.36D, 0, 0), 1e-9);
    }

    @Test
    void theArrivalFlashFadesOut() {
        assertEquals(0.55D, RarityMotion.flash(0.0D), 1e-9);
        assertEquals(0.0D, RarityMotion.flash(0.6D));
        assertEquals(0.0D, RarityMotion.flash(-1.0D));
        assertTrue(RarityMotion.flash(0.2D) > RarityMotion.flash(0.4D));
    }

    @Test
    void theTintRisesWithTheRankAndTheCommonHasNone() {
        int blue = 0xFF4EA3FF;
        assertEquals(0, RarityMotion.tint(0, blue)[0]);
        int rare = RarityMotion.tint(2, blue)[1] & 0xFF;
        int epic = RarityMotion.tint(3, blue)[1] & 0xFF;
        assertTrue(epic > rare);
        // The legendary is warmer and brighter than the epic at the bottom.
        int legendaryRed = RarityMotion.tint(4, 0xFFFF9A2E)[1] >> 16 & 255;
        int epicRed = RarityMotion.tint(3, 0xFFC58BFF)[1] >> 16 & 255;
        assertTrue(legendaryRed > 0xB0);
        assertTrue(legendaryRed >= epicRed - 10);
    }

    @Test
    void theHoverSweepRunsOnceThenStops() {
        assertEquals(-1.0D, RarityMotion.hoverSweep(-0.1D));
        assertEquals(0.5D, RarityMotion.hoverSweep(0.35D), 1e-9);
        assertEquals(-1.0D, RarityMotion.hoverSweep(0.8D));
    }
}
