package com.omneria.fordix.client.gui;

/**
 * Colors for the Voidframe direction - the visual identity picked for Fordix.
 * ARGB ints, ready for {@code GuiGraphics.fill}/{@code drawString}.
 */
public final class FordixPalette {

    private FordixPalette() {
    }

    public static final int PANEL_FILL = 0xFF131A22;
    public static final int BEVEL_HIGHLIGHT = 0xFF24313D;
    public static final int BEVEL_SHADOW = 0xFF05070A;

    public static final int BUTTON_FILL = 0xFF16202A;
    public static final int BUTTON_FILL_HOVERED = 0xFF1C2830;

    public static final int ACCENT = 0xFF3FD1C7;
    public static final int TEXT = 0xFF9FB3C0;
    public static final int TEXT_BRIGHT = 0xFFC9E9E5;

    /** Recessed wells: item slots, progress tracks, the player preview. */
    public static final int SLOT_FILL = 0xFF0B1016;
    public static final int PROGRESS_TRACK = 0xFF1E2A35;
    public static final int FLAME = 0xFFE39B45;
}
