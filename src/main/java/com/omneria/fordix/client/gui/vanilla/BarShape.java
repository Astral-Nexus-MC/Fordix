package com.omneria.fordix.client.gui.vanilla;

/**
 * The angular ends of the Voidframe status bars: one end is cut on a slant, so each row of
 * the bar is a little shorter than the one above. Plain Java, unit tested; {@link VoidframeHud}
 * draws the rows.
 */
public final class BarShape {
    private BarShape() {
    }

    /**
     * Horizontal extent {start, end} (end exclusive, relative to the bar's left edge) of a row.
     * With {@code cutRight} the right end recedes going down (health); otherwise the left end
     * advances going up (hunger), so the two bars face each other across the hotbar.
     */
    public static int[] rowSpan(int row, int height, int width, int slant, boolean cutRight) {
        int steps = Math.max(1, height - 1);
        if (cutRight) {
            return new int[]{0, width - Math.round(slant * row / (float) steps)};
        }
        return new int[]{Math.round(slant * (steps - row) / (float) steps), width};
    }
}
