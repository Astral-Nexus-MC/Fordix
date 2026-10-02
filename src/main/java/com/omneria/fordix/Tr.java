package com.omneria.fordix;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Fordix's own texts, looked up in the mod's language files (assets/fordix/lang) under the
 * {@code fordix.} prefix. {@link #c} gives a component that the receiving client translates, so a
 * server can send feedback without knowing the player's language; {@link #s} gives the text now and
 * is for client code only.
 */
public final class Tr {
    private static final String PREFIX = "fordix.";

    private Tr() {
    }

    public static MutableComponent c(String key, Object... args) {
        return Component.translatable(PREFIX + key, args);
    }

    /** The text in the current game language (client side). */
    public static String s(String key, Object... args) {
        return c(key, args).getString();
    }

    /** The key a status line travels under between server and client: "key" then its arguments, \u001F-separated. */
    public static String encode(String key, Object... args) {
        StringBuilder text = new StringBuilder(key);
        for (Object arg : args) {
            text.append('\u001F').append(arg);
        }
        return text.toString();
    }

    /** The text of a line made by {@link #encode}, in the current game language (client side). */
    public static String decode(String line) {
        String[] parts = line.split("\u001F", -1);
        Object[] args = new Object[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);
        return s(parts[0], args);
    }
}
