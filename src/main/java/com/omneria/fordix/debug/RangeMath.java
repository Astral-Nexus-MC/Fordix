package com.omneria.fordix.debug;

import java.util.ArrayList;
import java.util.List;

/** Height ranges of an ore: merged when they overlap or touch, and tested against a height. Plain Java, no Minecraft. */
public final class RangeMath {
    private RangeMath() {
    }

    /** {@code flat} holds pairs [low, high, low, high...]; the result is sorted and merged in the same layout. */
    public static int[] merge(int[] flat) {
        List<int[]> pairs = new ArrayList<>();
        for (int i = 0; i + 1 < flat.length; i += 2) {
            pairs.add(new int[] {Math.min(flat[i], flat[i + 1]), Math.max(flat[i], flat[i + 1])});
        }
        pairs.sort((a, b) -> Integer.compare(a[0], b[0]));
        List<int[]> merged = new ArrayList<>();
        for (int[] pair : pairs) {
            if (!merged.isEmpty() && pair[0] <= merged.get(merged.size() - 1)[1] + 1) {
                int[] last = merged.get(merged.size() - 1);
                last[1] = Math.max(last[1], pair[1]);
            } else {
                merged.add(pair);
            }
        }
        int[] out = new int[merged.size() * 2];
        for (int i = 0; i < merged.size(); i++) {
            out[i * 2] = merged.get(i)[0];
            out[i * 2 + 1] = merged.get(i)[1];
        }
        return out;
    }

    public static boolean contains(int[] flat, int y) {
        for (int i = 0; i + 1 < flat.length; i += 2) {
            if (y >= flat[i] && y <= flat[i + 1]) {
                return true;
            }
        }
        return false;
    }

    /** "-64 à 72 et 80 à 320". */
    public static String describe(int[] flat, String and, String to) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i + 1 < flat.length; i += 2) {
            if (i > 0) {
                text.append(' ').append(and).append(' ');
            }
            text.append(flat[i]).append(' ').append(to).append(' ').append(flat[i + 1]);
        }
        return text.toString();
    }

    /** Position of a height on a vertical scale, 0 at the top (maxY) and 1 at the bottom (minY), clamped. */
    public static double fraction(int y, int minY, int maxY) {
        if (maxY <= minY) {
            return 0.0D;
        }
        double value = (double) (maxY - y) / (double) (maxY - minY);
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
