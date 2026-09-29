package com.omneria.fordix.client.guide;

import java.util.List;

/**
 * The guide tab lines the connected server pushed, if any. While a server list is set
 * it replaces the local config; it's cleared on disconnect so other worlds and servers
 * use the player's own guides again.
 */
public final class ServerGuides {
    private static volatile List<String> lines = List.of();

    private ServerGuides() {
    }

    public static List<String> lines() {
        return lines;
    }

    public static void set(List<String> received) {
        lines = List.copyOf(received);
    }

    public static void clear() {
        lines = List.of();
    }
}
