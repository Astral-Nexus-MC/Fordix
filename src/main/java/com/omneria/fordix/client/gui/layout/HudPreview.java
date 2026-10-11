package com.omneria.fordix.client.gui.layout;

import java.util.ArrayList;
import java.util.List;

/** The HUD editor's preview at other interface sizes: the sizes offered and the miniature's geometry. Pure Java, unit tested. */
public final class HudPreview {
    /** Interface sizes (in scaled pixels) the game produces from common windows: 320 x 240 is the game's minimum. */
    public static final int[][] SIZES = {{320, 240}, {427, 240}, {480, 270}, {640, 360}, {854, 480}, {960, 540}, {1280, 720}, {1920, 1080}};

    private HudPreview() {
    }

    public static String label(int index) {
        return SIZES[index][0] + " x " + SIZES[index][1];
    }

    /** The next size, wrapping around. */
    public static int step(int index, int direction) {
        return Math.floorMod(index + direction, SIZES.length);
    }

    /** How much the miniature of a {@code width} x {@code height} interface is scaled to fill an area. */
    public static float scale(int areaWidth, int areaHeight, int width, int height) {
        if (width <= 0 || height <= 0 || areaWidth <= 0 || areaHeight <= 0) {
            return 0.0F;
        }
        return Math.min(areaWidth / (float) width, areaHeight / (float) height);
    }

    /** Whether any part of the rectangle falls outside an interface of this size. */
    public static boolean outside(HudSnap.Rect rect, int width, int height) {
        return rect.x() < 0 || rect.y() < 0 || rect.right() > width || rect.bottom() > height;
    }

    /** Indices of the rectangles that fall outside. */
    public static List<Integer> outsideAll(List<HudSnap.Rect> rects, int width, int height) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < rects.size(); i++) {
            if (outside(rects.get(i), width, height)) {
                out.add(i);
            }
        }
        return out;
    }
}
