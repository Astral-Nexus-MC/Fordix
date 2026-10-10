package com.omneria.fordix.client.gui.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FitTest {
    @Test
    void nothingToShrinkWhenTheInterfaceIsBigEnough() {
        assertEquals(1.0F, Fit.factor(854, 480, 400, 240));
        assertEquals(1.0F, Fit.factor(400, 240, 400, 240));
    }

    @Test
    void noMinimumMeansNoShrinking() {
        assertEquals(1.0F, Fit.factor(320, 240, 0, 0));
    }

    @Test
    void shrinksByTheTighterSide() {
        assertEquals(0.8F, Fit.factor(320, 240, 400, 240), 1e-6);
        assertEquals(0.5F, Fit.factor(400, 120, 400, 240), 1e-6);
    }

    @Test
    void theSmallestInterfaceTheGameAllowsKeepsEveryScreenReadable() {
        // 320 x 240 is the floor of the game; the widest declared minimum is 420 x 250.
        assertEquals(true, Fit.factor(320, 240, 420, 250) > 0.7F);
    }
}
