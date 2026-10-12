package com.omneria.fordix.debug;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangeMathTest {
    @Test
    void mergesOverlappingAndTouchingRanges() {
        assertArrayEquals(new int[] {-64, 72, 80, 320}, RangeMath.merge(new int[] {-24, 56, -64, 72, 80, 320}));
        assertArrayEquals(new int[] {0, 20}, RangeMath.merge(new int[] {0, 9, 10, 20}));
    }

    @Test
    void containsTestsEveryRange() {
        int[] ranges = {-64, 72, 80, 320};
        assertTrue(RangeMath.contains(ranges, 150));
        assertTrue(RangeMath.contains(ranges, 72));
        assertFalse(RangeMath.contains(ranges, 75));
        assertFalse(RangeMath.contains(ranges, 400));
    }

    @Test
    void describesRanges() {
        assertEquals("-64 à 72 et 80 à 320", RangeMath.describe(new int[] {-64, 72, 80, 320}, "et", "à"));
    }

    @Test
    void fractionRunsFromTopToBottomAndIsClamped() {
        assertEquals(0.0D, RangeMath.fraction(320, -64, 320), 1e-9);
        assertEquals(1.0D, RangeMath.fraction(-64, -64, 320), 1e-9);
        assertEquals(0.0D, RangeMath.fraction(900, -64, 320), 1e-9);
        assertEquals(1.0D, RangeMath.fraction(-900, -64, 320), 1e-9);
    }

    @Test
    void oreColoursAreStableAndKnownOnesKeepTheirLook() {
        assertEquals(0x4AD6E0, OreColors.of("minecraft:deepslate_diamond_ore"));
        assertEquals(0x9FB3C8, OreColors.of("create:zinc_ore"));
        assertEquals(OreColors.of("somemod:mystery_ore"), OreColors.of("somemod:mystery_ore"));
    }
}
