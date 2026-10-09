package com.omneria.fordix.client.gui.layout;

import java.util.ArrayList;
import java.util.List;

/** The editor's verifier: which visible elements sit on top of each other. Pure Java, unit tested. */
public final class HudChecks {
    /** Share of the smaller rectangle that must be covered before two elements count as overlapping. */
    public static final float OVERLAP = 0.30F;

    private HudChecks() {
    }

    /** Pairs {i, j} (i < j) of rectangles where the shared area is at least {@link #OVERLAP} of the smaller one. */
    public static List<int[]> overlaps(List<HudSnap.Rect> rects) {
        List<int[]> pairs = new ArrayList<>();
        for (int i = 0; i < rects.size(); i++) {
            for (int j = i + 1; j < rects.size(); j++) {
                if (shared(rects.get(i), rects.get(j)) >= OVERLAP) {
                    pairs.add(new int[]{i, j});
                }
            }
        }
        return pairs;
    }

    /** Fraction (0..1) of the smaller rectangle covered by the other. */
    static float shared(HudSnap.Rect a, HudSnap.Rect b) {
        int w = Math.min(a.right(), b.right()) - Math.max(a.x(), b.x());
        int h = Math.min(a.bottom(), b.bottom()) - Math.max(a.y(), b.y());
        if (w <= 0 || h <= 0) {
            return 0.0F;
        }
        int smaller = Math.min(a.w() * a.h(), b.w() * b.h());
        return smaller <= 0 ? 0.0F : (w * h) / (float) smaller;
    }
}
