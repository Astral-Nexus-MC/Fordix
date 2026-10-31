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
    void accessoryColumnsHoldEightSlots() {
        assertEquals(0, ModLayouts.accessoryColumns(0));
        assertEquals(1, ModLayouts.accessoryColumns(1));
        assertEquals(1, ModLayouts.accessoryColumns(8));
        assertEquals(2, ModLayouts.accessoryColumns(9));
        assertEquals(6, ModLayouts.accessoryColumns(48));
    }

    @Test
    void accessoryShiftIsZeroWithoutSlotsAndGrowsByAColumn() {
        assertEquals(0, ModLayouts.accessoryShift(0, 0));
        assertEquals(32, ModLayouts.accessoryShift(5, 0));
        assertEquals(54, ModLayouts.accessoryShift(12, 0));
        assertEquals(54, ModLayouts.accessoryShift(8, 8));
        assertEquals(142, ModLayouts.accessoryShift(48, 0));
    }

    @Test
    void accessoryBoxFillsAColumnThenTheNext() {
        assertArrayEquals(new int[]{18, 50}, ModLayouts.accessoryBox(0, 0));
        assertArrayEquals(new int[]{18, 50 + 7 * 22}, ModLayouts.accessoryBox(7, 0));
        assertArrayEquals(new int[]{40, 50}, ModLayouts.accessoryBox(8, 0));
        assertArrayEquals(new int[]{62, 72}, ModLayouts.accessoryBox(1, 2));
    }
}
