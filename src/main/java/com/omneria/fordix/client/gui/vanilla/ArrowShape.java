package com.omneria.fordix.client.gui.vanilla;

import java.util.ArrayList;
import java.util.List;

/**
 * Pixel spans of a right-pointing progress arrow, drawn with plain fills instead of
 * vanilla's arrow texture. A thin shaft runs to a triangular head whose tip sits on
 * the middle row. No Minecraft dependency, so the shape is unit tested on its own.
 */
public final class ArrowShape {

    private ArrowShape() {
    }

    /** One horizontal run of pixels, relative to the arrow's top-left corner. */
    public record Span(int x, int y, int width) {
    }

    /**
     * The spans of an arrow of the given size, clipped to the first {@code filled}
     * pixels from the left - pass {@code width} for the full outline, or a progress
     * value to draw only the filled part.
     */
    public static List<Span> spans(int width, int height, int filled) {
        List<Span> spans = new ArrayList<>();
        int headLength = (height + 1) / 2;
        int headStart = width - headLength;
        int shaftHalfHeight = height / 6;
        int limit = Math.min(filled, width);
        for (int row = 0; row < height; row++) {
            // Distance from the middle row, in half-rows, so odd and even heights both taper evenly.
            int distance = Math.abs(2 * row - (height - 1));
            int start = distance <= 2 * shaftHalfHeight ? 0 : headStart;
            int end = Math.min(width - (distance + 1) / 2, limit);
            if (end > start) {
                spans.add(new Span(start, row, end - start));
            }
        }
        return spans;
    }
}
