package com.omneria.fordix.client.gui.vanilla;

/**
 * How many rows of a chest-like container a screen shows at once. When the whole container does not fit the screen, the rows are
 * cut into pages (a strip with "<" and ">" under the grid), the way the ships of Small Ships show their inventory. Pure, so it is tested.
 */
public final class ChestPaging {
    /** The least rows a page of the wide layout holds: fewer than that, the compact layout is the better use of the room. */
    public static final int MIN_WIDE_ROWS = 3;

    private ChestPaging() {
    }

    /**
     * Rows of the wide layout that fit a screen: the room left after the margins, the part above the grid, the strip of the page
     * buttons (when paged) and the part under it. Returns {@code rows} when everything fits, 0 when even a page of
     * {@link #MIN_WIDE_ROWS} does not (the compact layout is used then).
     */
    public static int wideRows(int screenHeight, int margin, int rows, int pitch, int above, int below, int strip) {
        int room = screenHeight - 2 * margin - above - below;
        if (room / pitch >= rows) {
            return rows;
        }
        int paged = (room - strip) / pitch;
        return paged >= MIN_WIDE_ROWS ? paged : 0;
    }

    /**
     * Rows of the compact layout (the mod's own panel, {@code panelHeight} tall for {@code rows} rows of {@code pitch} px) that fit a
     * screen. Returns {@code rows} when the panel fits, else the rows that fit with the strip of the page buttons, at least 1.
     */
    public static int compactRows(int screenHeight, int margin, int panelHeight, int rows, int pitch, int strip) {
        int room = screenHeight - 2 * margin;
        if (panelHeight <= room) {
            return rows;
        }
        int missing = panelHeight + strip - room;
        int cut = (missing + pitch - 1) / pitch;
        return Math.max(1, Math.min(rows - 1, rows - cut));
    }

    public static int pages(int rows, int shownRows) {
        return shownRows <= 0 ? 1 : (rows + shownRows - 1) / shownRows;
    }
}
