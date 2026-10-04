package com.omneria.fordix.client.gui.layout;

import java.util.List;
import java.util.Locale;

/**
 * Anchored placement of the HUD elements. An element is pinned to one of nine points of the
 * screen (corners, edge middles, center): its matching point sits at the screen's, then the
 * offset moves it. Unlike a plain offset from a default spot, it keeps its place when the window
 * or the GUI scale changes. {@link Anchor#DEFAULT} keeps vanilla-like default spot plus offset.
 * Pure Java, unit tested.
 */
public final class HudAnchors {
    public static final double MIN_SCALE = 0.5D;
    public static final double MAX_SCALE = 3.0D;

    private HudAnchors() {
    }

    /** A size factor kept in the range the layout screen allows (and 1 for a missing value). */
    public static float clampScale(float scale) {
        if (!(scale > 0.0F)) {
            return 1.0F;
        }
        return (float) Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }

    /** A rectangle with width and height multiplied separately, its top-left corner staying where it is. */
    public static HudSnap.Rect scaled(HudSnap.Rect rect, float scaleX, float scaleY) {
        return new HudSnap.Rect(rect.x(), rect.y(), Math.max(1, Math.round(rect.w() * scaleX)), Math.max(1, Math.round(rect.h() * scaleY)));
    }

    /** A rectangle with its size multiplied, its top-left corner staying where it is. */
    public static HudSnap.Rect scaled(HudSnap.Rect rect, float scale) {
        return new HudSnap.Rect(rect.x(), rect.y(), Math.max(1, Math.round(rect.w() * scale)), Math.max(1, Math.round(rect.h() * scale)));
    }

    public enum Anchor {
        DEFAULT(0, 0),
        TOP_LEFT(0, 0),
        TOP_CENTER(0.5F, 0),
        TOP_RIGHT(1, 0),
        MIDDLE_LEFT(0, 0.5F),
        CENTER(0.5F, 0.5F),
        MIDDLE_RIGHT(1, 0.5F),
        BOTTOM_LEFT(0, 1),
        BOTTOM_CENTER(0.5F, 1),
        BOTTOM_RIGHT(1, 1);

        private final float fx;
        private final float fy;

        Anchor(float fx, float fy) {
            this.fx = fx;
            this.fy = fy;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Anchor byId(String id) {
            for (Anchor anchor : values()) {
                if (anchor.id().equals(id)) {
                    return anchor;
                }
            }
            return null;
        }
    }

    /** Where an element with this anchor and offset ends up, kept fully on screen. */
    public static HudSnap.Rect place(Anchor anchor, int offsetX, int offsetY, HudSnap.Rect defaultRect, int screenWidth, int screenHeight) {
        if (anchor == Anchor.DEFAULT) {
            return HudBounds.place(defaultRect, offsetX, offsetY, screenWidth, screenHeight);
        }
        int x = Math.round(anchor.fx * (screenWidth - defaultRect.w())) + offsetX;
        int y = Math.round(anchor.fy * (screenHeight - defaultRect.h())) + offsetY;
        int clampedX = Math.max(0, Math.min(Math.max(0, screenWidth - defaultRect.w()), x));
        int clampedY = Math.max(0, Math.min(Math.max(0, screenHeight - defaultRect.h()), y));
        return new HudSnap.Rect(clampedX, clampedY, defaultRect.w(), defaultRect.h());
    }

    /** The anchor whose third of the screen holds the rectangle's center. */
    public static Anchor nearest(HudSnap.Rect rect, int screenWidth, int screenHeight) {
        int column = third(rect.x() + rect.w() / 2, screenWidth);
        int row = third(rect.y() + rect.h() / 2, screenHeight);
        Anchor[][] grid = {
                {Anchor.TOP_LEFT, Anchor.TOP_CENTER, Anchor.TOP_RIGHT},
                {Anchor.MIDDLE_LEFT, Anchor.CENTER, Anchor.MIDDLE_RIGHT},
                {Anchor.BOTTOM_LEFT, Anchor.BOTTOM_CENTER, Anchor.BOTTOM_RIGHT}};
        return grid[row][column];
    }

    private static int third(int value, int size) {
        if (value < size / 3) {
            return 0;
        }
        return value < size * 2 / 3 ? 1 : 2;
    }

    /** {offsetX, offsetY} that put a rectangle at its current spot with this anchor. */
    public static int[] offsetFor(Anchor anchor, HudSnap.Rect rect, HudSnap.Rect defaultRect, int screenWidth, int screenHeight) {
        if (anchor == Anchor.DEFAULT) {
            return new int[]{rect.x() - defaultRect.x(), rect.y() - defaultRect.y()};
        }
        return new int[]{rect.x() - Math.round(anchor.fx * (screenWidth - rect.w())),
                rect.y() - Math.round(anchor.fy * (screenHeight - rect.h()))};
    }

    /** One element's placement in a preset layout. */
    public record Entry(Anchor anchor, int offsetX, int offsetY, boolean hidden) {
        public static Entry standard() {
            return new Entry(Anchor.DEFAULT, 0, 0, false);
        }
    }

    /** A named layout for all elements, in the order of {@link HudBounds#ELEMENTS}. */
    public record Preset(String name, List<Entry> entries) {
    }

    public static final List<Preset> PRESETS = List.of(
            new Preset("Classique", List.of(Entry.standard(), Entry.standard(), Entry.standard(),
                    Entry.standard(), Entry.standard(), Entry.standard())),
            new Preset("Coins", List.of(
                    new Entry(Anchor.BOTTOM_LEFT, 6, -6, false),
                    new Entry(Anchor.BOTTOM_RIGHT, -6, -6, false),
                    new Entry(Anchor.BOTTOM_LEFT, 6, -17, false),
                    new Entry(Anchor.BOTTOM_RIGHT, -6, -17, false),
                    Entry.standard(),
                    Entry.standard())),
            new Preset("Haut", List.of(
                    new Entry(Anchor.TOP_LEFT, 6, 6, false),
                    new Entry(Anchor.TOP_LEFT, 6, 17, false),
                    new Entry(Anchor.TOP_LEFT, 6, 28, false),
                    new Entry(Anchor.TOP_LEFT, 6, 39, false),
                    Entry.standard(),
                    new Entry(Anchor.TOP_CENTER, 0, 4, false))),
            new Preset("Minimal", List.of(
                    Entry.standard(), Entry.standard(),
                    new Entry(Anchor.DEFAULT, 0, 0, true),
                    new Entry(Anchor.DEFAULT, 0, 0, false),
                    Entry.standard(),
                    Entry.standard())));
}
