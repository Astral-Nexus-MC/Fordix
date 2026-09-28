package com.omneria.fordix.client.gui;

/**
 * The colors every Fordix screen draws with, as ARGB ints ready for
 * {@code GuiGraphics.fill}/{@code drawString}. Set from a {@link Theme} - Voidframe
 * by default - and reapplied when the client config changes, so they are plain
 * statics read at draw time rather than constants.
 */
public final class FordixPalette {

    private FordixPalette() {
    }

    public static int PANEL_FILL;
    public static int BEVEL_HIGHLIGHT;
    public static int BEVEL_SHADOW;

    public static int BUTTON_FILL;
    public static int BUTTON_FILL_HOVERED;

    public static int ACCENT;
    public static int TEXT;
    public static int TEXT_BRIGHT;

    /** Recessed wells: item slots, progress tracks, the player preview. */
    public static int SLOT_FILL;
    public static int PROGRESS_TRACK;
    public static int FLAME;
    public static int ERROR;

    /** Title strip across the top of container screens, with a faint accent rule under it. */
    public static int TITLE_BAND;
    public static int TITLE_RULE;

    /** Hovered slot fill, under the item - the accent outline is drawn over it. */
    public static int SLOT_HOVER;

    public static int TOOLTIP_FILL;
    public static int TOOLTIP_BORDER_TOP;
    public static int TOOLTIP_BORDER_BOTTOM;

    static {
        apply(Theme.byName(Theme.DEFAULT));
    }

    public static void apply(Theme theme) {
        PANEL_FILL = theme.panelFill();
        BEVEL_HIGHLIGHT = theme.bevelHighlight();
        BEVEL_SHADOW = theme.bevelShadow();
        BUTTON_FILL = theme.buttonFill();
        BUTTON_FILL_HOVERED = theme.buttonFillHovered();
        ACCENT = theme.accent();
        TEXT = theme.text();
        TEXT_BRIGHT = theme.textBright();
        SLOT_FILL = theme.slotFill();
        PROGRESS_TRACK = theme.progressTrack();
        FLAME = theme.flame();
        ERROR = theme.error();
        TITLE_BAND = theme.titleBand();
        TITLE_RULE = alpha(theme.accent(), 0x60);
        SLOT_HOVER = alpha(theme.accent(), 0x30);
        TOOLTIP_FILL = alpha(theme.panelFill(), 0xF0);
        TOOLTIP_BORDER_TOP = alpha(theme.accent(), 0xA0);
        TOOLTIP_BORDER_BOTTOM = alpha(theme.bevelHighlight(), 0xA0);
    }

    private static int alpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}
