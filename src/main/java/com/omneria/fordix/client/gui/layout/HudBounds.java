package com.omneria.fordix.client.gui.layout;

import java.util.List;

/**
 * Where each movable Voidframe HUD element sits by default (no offset), in scaled GUI pixels,
 * for a given screen size. Mirrors the positions VoidframeHud and VoidframeGauges draw at, so
 * the layout screen can outline them even when the element isn't showing (no armor, dry land).
 * Elements are in the order of FordixConfig.HUD_ELEMENTS.
 */
public final class HudBounds {
    public static final List<String> ELEMENTS = List.of("health", "hunger", "armor", "air", "experience", "boss");
    public static final List<String> LABELS = List.of("Vie", "Faim", "Armure", "Air", "Experience", "Boss");

    private HudBounds() {
    }

    public static HudSnap.Rect defaults(String element, int screenWidth, int screenHeight) {
        int center = screenWidth / 2;
        return switch (element) {
            case "health" -> new HudSnap.Rect(center - 91, screenHeight - 39, 81, 9);
            case "hunger" -> new HudSnap.Rect(center + 10, screenHeight - 39, 81, 9);
            case "armor" -> new HudSnap.Rect(center - 91, screenHeight - 48, 52, 7);
            case "air" -> new HudSnap.Rect(center + 39, screenHeight - 48, 52, 7);
            case "experience" -> new HudSnap.Rect(center - 91, screenHeight - 40, 182, 18);
            case "boss" -> new HudSnap.Rect(center - 91, 4, 182, 19);
            default -> throw new IllegalArgumentException(element);
        };
    }

    /** The rectangle moved by an offset and kept fully on screen. */
    public static HudSnap.Rect place(HudSnap.Rect base, int offsetX, int offsetY, int screenWidth, int screenHeight) {
        int x = clamp(base.x() + offsetX, 0, Math.max(0, screenWidth - base.w()));
        int y = clamp(base.y() + offsetY, 0, Math.max(0, screenHeight - base.h()));
        return new HudSnap.Rect(x, y, base.w(), base.h());
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
