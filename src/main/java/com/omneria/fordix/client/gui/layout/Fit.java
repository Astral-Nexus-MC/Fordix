package com.omneria.fordix.client.gui.layout;

/** How much the interface must shrink for a screen's minimum size to fit: pure, so it is tested without the game. */
public final class Fit {
    private Fit() {
    }

    /** 1 when {@code width} x {@code height} holds the minimum (0 = no minimum), else the factor below 1 that makes it hold. */
    public static float factor(int width, int height, int minWidth, int minHeight) {
        float fit = 1.0F;
        if (minWidth > 0 && width < minWidth) {
            fit = Math.min(fit, width / (float) minWidth);
        }
        if (minHeight > 0 && height < minHeight) {
            fit = Math.min(fit, height / (float) minHeight);
        }
        return fit;
    }
}
