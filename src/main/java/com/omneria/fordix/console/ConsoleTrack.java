package com.omneria.fordix.console;

/** The track of a levelled setting, from -max to +max: which cells are lit for a level. Plain Java, no Minecraft. */
public final class ConsoleTrack {
    public static final int EMPTY = 0;
    public static final int UP = 1;
    public static final int DOWN = 2;

    private ConsoleTrack() {
    }

    /** One entry per cell, from -max to +max: lit {@link #UP} between 0 and a positive level, {@link #DOWN} between a negative level and 0. */
    public static int[] cells(int level, int max) {
        int[] cells = new int[max * 2 + 1];
        int clamped = Math.max(-max, Math.min(max, level));
        for (int i = -max; i <= max; i++) {
            if (clamped > 0 && i > 0 && i <= clamped) {
                cells[i + max] = UP;
            } else if (clamped < 0 && i < 0 && i >= clamped) {
                cells[i + max] = DOWN;
            }
        }
        return cells;
    }

    /** Index of the cell of a level (the one drawn with a frame). */
    public static int index(int level, int max) {
        return Math.max(-max, Math.min(max, level)) + max;
    }

    /** "+2", "-1" or "0". */
    public static String signed(int level) {
        return level > 0 ? "+" + level : Integer.toString(level);
    }
}
