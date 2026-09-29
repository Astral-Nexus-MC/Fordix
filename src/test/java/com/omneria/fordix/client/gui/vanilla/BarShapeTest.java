package com.omneria.fordix.client.gui.vanilla;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class BarShapeTest {

    @Test
    void healthTopRowIsFullAndBottomRowIsShortenedBySlant() {
        assertArrayEquals(new int[]{0, 81}, BarShape.rowSpan(0, 9, 81, 4, true));
        assertArrayEquals(new int[]{0, 77}, BarShape.rowSpan(8, 9, 81, 4, true));
    }

    @Test
    void hungerBottomRowIsFullAndTopRowIsIndented() {
        assertArrayEquals(new int[]{0, 81}, BarShape.rowSpan(8, 9, 81, 4, false));
        assertArrayEquals(new int[]{4, 81}, BarShape.rowSpan(0, 9, 81, 4, false));
    }

    @Test
    void rowsChangeMonotonically() {
        int previous = 81;
        for (int row = 0; row < 9; row++) {
            int end = BarShape.rowSpan(row, 9, 81, 4, true)[1];
            org.junit.jupiter.api.Assertions.assertTrue(end <= previous);
            previous = end;
        }
    }
}
