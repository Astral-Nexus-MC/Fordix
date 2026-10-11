package com.omneria.fordix.client.guide;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatScaleTest {

    @Test
    void theFullBarLeavesRoomAboveTheHighestMark() {
        assertEquals(9.6, StatScale.full(7, 8, 6), 1e-9);
        assertEquals(12.0, StatScale.full(10, 8, 6), 1e-9);
        assertEquals(10.8, StatScale.full(1, 8, 9), 1e-9);
    }

    @Test
    void anItemBetterThanTheBestStillFitsItsBar() {
        double full = StatScale.full(12, 8, 6);
        assertTrue(StatScale.fraction(12, full) < 1.0F);
        assertTrue(StatScale.fraction(12, full) > StatScale.fraction(8, full));
    }

    @Test
    void aMissingMarkIsNegative() {
        assertEquals(-1.0F, StatScale.fraction(-1, 10), 0.0F);
        assertEquals(1.0F, StatScale.full(0, 0, 0), 0.0F);
    }

    @Test
    void theFractionIsCappedAtOne() {
        assertEquals(1.0F, StatScale.fraction(20, 10), 0.0F);
    }
}
