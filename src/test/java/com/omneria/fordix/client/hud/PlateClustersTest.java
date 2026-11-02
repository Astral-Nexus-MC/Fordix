package com.omneria.fordix.client.hud;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PlateClustersTest {
    private static boolean[] grid(int columns, int rows, int[][] cells) {
        boolean[] used = new boolean[columns * rows];
        for (int[] cell : cells) {
            used[cell[1] * columns + cell[0]] = true;
        }
        return used;
    }

    @Test
    void nothingDrawnMakesNoPlate() {
        assertTrue(PlateClusters.find(new boolean[40], 8, 5).isEmpty());
    }

    @Test
    void neighbouringCellsFormOneGroupWithItsBox() {
        List<int[]> groups = PlateClusters.find(grid(10, 6, new int[][]{{2, 1}, {3, 1}, {3, 2}}), 10, 6);
        assertEquals(1, groups.size());
        assertArrayEquals(new int[]{2, 1, 3, 2}, groups.get(0));
    }

    @Test
    void oneEmptyCellBetweenStaysOneGroup() {
        assertEquals(1, PlateClusters.find(grid(12, 4, new int[][]{{1, 1}, {3, 1}}), 12, 4).size());
    }

    @Test
    void farApartThingsAreTwoPlates() {
        List<int[]> groups = PlateClusters.find(grid(20, 6, new int[][]{{1, 4}, {2, 4}, {16, 0}, {17, 1}}), 20, 6);
        assertEquals(2, groups.size());
    }
}
