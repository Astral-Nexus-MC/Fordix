package com.omneria.fordix.client.gui.vanilla;

/**
 * Positions of the wide layouts of other mods' containers (Botania's flower pouch and bauble box) and of the Curios column of
 * the inventory. Plain Java, no Minecraft: the numbers are those of the mockup and are tested on their own.
 */
public final class ModLayouts {
    /** The 20 px box of a slot, and the 22 px pitch of the wide layouts (the same as the chests). */
    public static final int PITCH = 22;

    // ---- Botania's flower pouch: 32 slots in two groups of 16 (small flowers, then tall ones), 8 columns
    public static final int POUCH_W = 218;
    public static final int POUCH_SMALL_Y = 48;
    public static final int POUCH_TALL_Y = 113;
    public static final int POUCH_COLUMN_X = 22;
    public static final int POUCH_DIVIDER = 165;
    public static final int POUCH_H = POUCH_DIVIDER + 168;
    public static final int POUCH_SLOTS = 32;

    // ---- Botania's bauble box: 24 slots, 6 columns, the player on the left
    public static final int BOX_W = 226;
    public static final int BOX_GRID_X = 88;
    public static final int BOX_GRID_Y = 34;
    public static final int BOX_DIVIDER = BOX_GRID_Y + PITCH * 4 + 10;
    public static final int BOX_H = BOX_DIVIDER + 168;
    public static final int BOX_SLOTS = 24;
    /** The well the player is drawn in: {x, y, width, height}. */
    public static final int[] BOX_PLAYER = {10, BOX_GRID_Y, 68, 86};

    // ---- the Curios column of the survival inventory
    /** How far the equipment, the player and the attribute card move to the right to make room for the column. */
    public static final int CURIOS_SHIFT = 32;
    public static final int CURIOS_X = 18;
    public static final int CURIOS_Y = 50;
    public static final int CURIOS_ROWS = 8;

    private ModLayouts() {
    }

    /** The top-left of the box of pouch slot {@code index} (0 to 31). */
    public static int[] pouchBox(int index) {
        int group = index / 16;
        int inGroup = index % 16;
        return new int[]{POUCH_COLUMN_X + inGroup % 8 * PITCH, (group == 0 ? POUCH_SMALL_Y : POUCH_TALL_Y) + inGroup / 8 * PITCH};
    }

    /** The top-left of the box of bauble box slot {@code index} (0 to 23). */
    public static int[] boxBox(int index) {
        return new int[]{BOX_GRID_X + index % 6 * PITCH, BOX_GRID_Y + index / 6 * PITCH};
    }

    /** How many Curios slots the column draws out of {@code total}: it stops at the room there is. */
    public static int curiosShown(int total) {
        return Math.max(0, Math.min(total, CURIOS_ROWS));
    }

    /** How many Curios slots do not fit in the column (the "+3" under it). */
    public static int curiosHidden(int total) {
        return Math.max(0, total - CURIOS_ROWS);
    }

    /** The box of Curios cell {@code row}: {x, y}. */
    public static int[] curiosBox(int row) {
        return new int[]{CURIOS_X, CURIOS_Y + row * PITCH};
    }

    /** The row of the Curios column at a point relative to the panel, or -1. */
    public static int curiosRowAt(int x, int y, int shown) {
        if (x < CURIOS_X || x >= CURIOS_X + 20) {
            return -1;
        }
        int row = (y - CURIOS_Y) / PITCH;
        if (y < CURIOS_Y || row >= shown || (y - CURIOS_Y) % PITCH >= 20) {
            return -1;
        }
        return row;
    }
}
