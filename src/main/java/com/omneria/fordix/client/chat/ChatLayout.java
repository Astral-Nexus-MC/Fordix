package com.omneria.fordix.client.chat;

import com.omneria.fordix.client.gui.layout.HudSnap;

/**
 * Where each part of the chat sits, in scaled GUI pixels, for a screen size and a width: the tabs, the history, and the
 * text field under it. The whole block is one HUD element ({@link #block}). Pure Java, unit tested.
 */
public final class ChatLayout {
    public static final int MARGIN = 2;
    public static final int LOG_HEIGHT = 110;
    public static final int TABS_HEIGHT = 13;
    public static final int INPUT_HEIGHT = 12;
    /** Gap between the history and the text field. */
    public static final int GAP = 2;
    public static final int MIN_WIDTH = 240;
    public static final int MAX_WIDTH = 420;

    private ChatLayout() {
    }

    /** The width the chat gets from the player's chat width setting, kept in a readable range. */
    public static int width(int configuredWidth, int screenWidth) {
        return Math.max(1, Math.min(screenWidth - 2 * MARGIN, Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, configuredWidth))));
    }

    public static HudSnap.Rect input(int screenWidth, int screenHeight, int width) {
        return new HudSnap.Rect(MARGIN, screenHeight - MARGIN - INPUT_HEIGHT, width, INPUT_HEIGHT);
    }

    public static HudSnap.Rect log(int screenWidth, int screenHeight, int width) {
        int bottom = input(screenWidth, screenHeight, width).y() - GAP;
        return new HudSnap.Rect(MARGIN, bottom - LOG_HEIGHT, width, LOG_HEIGHT);
    }

    public static HudSnap.Rect tabs(int screenWidth, int screenHeight, int width) {
        HudSnap.Rect log = log(screenWidth, screenHeight, width);
        return new HudSnap.Rect(MARGIN, log.y() - 1 - TABS_HEIGHT, width, TABS_HEIGHT);
    }

    /** Tabs, history and text field: the element the HUD layout screen moves and sizes. */
    public static HudSnap.Rect block(int screenWidth, int screenHeight, int width) {
        HudSnap.Rect tabs = tabs(screenWidth, screenHeight, width);
        HudSnap.Rect input = input(screenWidth, screenHeight, width);
        return new HudSnap.Rect(MARGIN, tabs.y(), width, input.bottom() - tabs.y());
    }
}
