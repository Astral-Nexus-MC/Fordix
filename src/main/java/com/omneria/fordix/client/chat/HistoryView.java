package com.omneria.fordix.client.chat;

/** Which lines of the sent-messages history the command line shows while the player walks through it with the arrows. Pure Java, unit tested. */
public final class HistoryView {
    /** Lines {@code from} (included) to {@code to} (excluded) are shown, {@code selected} is the one in the field. */
    public record Window(int from, int to, int selected) {
        public boolean empty() {
            return to <= from;
        }
    }

    private HistoryView() {
    }

    /**
     * @param position index of the history line in the field; equal to {@code size} (or more) when the player is not browsing
     * @param size     how many lines the history holds
     * @param shown    most lines to show
     */
    public static Window window(int position, int size, int shown) {
        if (size <= 0 || position < 0 || position >= size || shown <= 0) {
            return new Window(0, 0, -1);
        }
        int to = position + 1;
        int from = Math.max(0, to - shown);
        return new Window(from, to, position);
    }

    /** The "3 / 14" counter: the line's rank counted from the oldest. */
    public static String counter(int position, int size) {
        return (position + 1) + " / " + size;
    }
}
