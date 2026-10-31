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

    // ---- the accessory bar (Curios) at the left of the survival inventory
    /** The bar starts at the left edge of the panel, below the title row, and holds eight slots per column. */
    public static final int ACCESS_X = 18;
    public static final int ACCESS_Y = 50;
    public static final int ACCESS_ROWS = 8;
    /** Room kept between the bar and the equipment column. */
    public static final int ACCESS_GAP = 10;

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

    /** How many columns {@code count} accessory slots take, eight to a column. */
    public static int accessoryColumns(int count) {
        return count <= 0 ? 0 : (count + ACCESS_ROWS - 1) / ACCESS_ROWS;
    }

    /** How far the equipment, the player and the attribute card move right to make room for the bar (0 without slots). */
    public static int accessoryShift(int normal, int cosmetic) {
        int columns = accessoryColumns(normal) + accessoryColumns(cosmetic);
        return columns == 0 ? 0 : columns * PITCH + ACCESS_GAP;
    }

    /** The top-left of the box of accessory slot {@code index} of a block whose first column is {@code firstColumn}. */
    public static int[] accessoryBox(int index, int firstColumn) {
        return new int[]{ACCESS_X + (firstColumn + index / ACCESS_ROWS) * PITCH, ACCESS_Y + index % ACCESS_ROWS * PITCH};
    }
}
