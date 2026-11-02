package com.omneria.fordix.client.hud;

import java.util.ArrayList;
import java.util.List;

/**
 * Groups what an overlay drew into the separate things it is made of (a mana bar at the bottom and the info of a block in the
 * middle are two plates, not one that covers the screen). The overlay is cut into a grid of cells; two used cells with at most
 * one empty cell between them belong to the same group. Plain Java, unit tested.
 */
public final class PlateClusters {
    private PlateClusters() {
    }

    /** The groups of used cells, each as {firstColumn, firstRow, lastColumn, lastRow}. */
    public static List<int[]> find(boolean[] used, int columns, int rows) {
        List<int[]> groups = new ArrayList<>();
        boolean[] seen = new boolean[used.length];
        int[] queue = new int[used.length];
        for (int start = 0; start < used.length; start++) {
            if (!used[start] || seen[start]) {
                continue;
            }
            int head = 0;
            int tail = 0;
            queue[tail++] = start;
            seen[start] = true;
            int[] box = {start % columns, start / columns, start % columns, start / columns};
            while (head < tail) {
                int cell = queue[head++];
                int column = cell % columns;
                int row = cell / columns;
                box[0] = Math.min(box[0], column);
                box[1] = Math.min(box[1], row);
                box[2] = Math.max(box[2], column);
                box[3] = Math.max(box[3], row);
                for (int dr = -2; dr <= 2; dr++) {
                    for (int dc = -2; dc <= 2; dc++) {
                        int c = column + dc;
                        int r = row + dr;
                        if (c < 0 || r < 0 || c >= columns || r >= rows) {
                            continue;
                        }
                        int next = r * columns + c;
                        if (used[next] && !seen[next]) {
                            seen[next] = true;
                            queue[tail++] = next;
                        }
                    }
                }
            }
            groups.add(box);
        }
        return groups;
    }
}
