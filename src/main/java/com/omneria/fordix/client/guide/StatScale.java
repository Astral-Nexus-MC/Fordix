package com.omneria.fordix.client.guide;

/**
 * The scale of one stat bar of an item sheet: how far the best item of the game goes, where the item in the player's hand
 * stands, and what a full bar means. Pure Java, unit tested.
 */
public final class StatScale {
    /** A bar leaves some room above the highest mark so that the best item does not sit at the very end. */
    private static final double HEADROOM = 1.2;

    private StatScale() {
    }

    /** What a full bar means: a little above the highest of the item, the best of the game and the held item. */
    public static double full(double value, double best, double held) {
        double top = Math.max(value, Math.max(best, held));
        return top <= 0.0 ? 1.0 : top * HEADROOM;
    }

    /** {@code value} as a share of {@code full}, kept in 0..1; negative when there is no mark to draw. */
    public static float fraction(double value, double full) {
        if (value < 0.0 || full <= 0.0) {
            return -1.0F;
        }
        return (float) Math.min(1.0, value / full);
    }
}
