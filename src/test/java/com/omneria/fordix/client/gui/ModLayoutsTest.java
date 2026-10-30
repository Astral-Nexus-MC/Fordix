package com.omneria.fordix.client.gui;

import com.omneria.fordix.client.gui.vanilla.ModLayouts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ModLayoutsTest {
    @Test
    void pouchHasTwoGroupsOfTwoRowsOfEight() {
        assertArrayEquals(new int[]{22, 48}, ModLayouts.pouchBox(0));
        assertArrayEquals(new int[]{22 + 7 * 22, 48}, ModLayouts.pouchBox(7));
        assertArrayEquals(new int[]{22, 70}, ModLayouts.pouchBox(8));
        assertArrayEquals(new int[]{22, 113}, ModLayouts.pouchBox(16));
        assertArrayEquals(new int[]{22 + 7 * 22, 135}, ModLayouts.pouchBox(31));
        assertEquals(333, ModLayouts.POUCH_H);
    }

    @Test
    void pouchGroupsAreSeparatedByMoreThanOnePitchRow() {
        int smallEnd = ModLayouts.pouchBox(15)[1] + 20;
        int tallStart = ModLayouts.pouchBox(16)[1];
        assertEquals(true, tallStart - smallEnd >= 8);
    }

    @Test
    void boxIsSixColumnsByFourRows() {
        assertArrayEquals(new int[]{88, 34}, ModLayouts.boxBox(0));
        assertArrayEquals(new int[]{88 + 5 * 22, 34}, ModLayouts.boxBox(5));
        assertArrayEquals(new int[]{88, 34 + 22}, ModLayouts.boxBox(6));
        assertArrayEquals(new int[]{88 + 5 * 22, 34 + 3 * 22}, ModLayouts.boxBox(23));
        assertEquals(300, ModLayouts.BOX_H);
    }

    @Test
    void curiosColumnStopsAtEightAndCountsTheRest() {
        assertEquals(0, ModLayouts.curiosShown(0));
        assertEquals(5, ModLayouts.curiosShown(5));
        assertEquals(8, ModLayouts.curiosShown(8));
        assertEquals(8, ModLayouts.curiosShown(11));
        assertEquals(3, ModLayouts.curiosHidden(11));
        assertEquals(0, ModLayouts.curiosHidden(8));
    }

    @Test
    void curiosRowAtFindsOnlyTheBoxes() {
        assertEquals(0, ModLayouts.curiosRowAt(20, 52, 8));
        assertEquals(1, ModLayouts.curiosRowAt(30, 50 + 22 + 3, 8));
        assertEquals(-1, ModLayouts.curiosRowAt(20, 50 + 21, 8));
        assertEquals(-1, ModLayouts.curiosRowAt(17, 60, 8));
        assertEquals(-1, ModLayouts.curiosRowAt(20, 49, 8));
        assertEquals(-1, ModLayouts.curiosRowAt(20, 50 + 4 * 22, 4));
    }
}
