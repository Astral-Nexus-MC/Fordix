package com.omneria.fordix.client.hud;

import java.util.Collection;
import java.util.Set;

/**
 * What the server this player is on requires of the HUD: elements that can never be hidden (life, hotbar), elements that are
 * always hidden, and a layout it suggests. Pure Java, unit tested; {@link #hidden} is what every "is it hidden" test goes through.
 */
public final class HudRules {
    private static volatile Set<String> required = Set.of();
    private static volatile Set<String> forbidden = Set.of();
    private static volatile String proposed = "";

    private HudRules() {
    }

    public static void set(Collection<String> requiredIds, Collection<String> forbiddenIds, String proposedCode) {
        required = Set.copyOf(requiredIds);
        forbidden = Set.copyOf(forbiddenIds);
        proposed = proposedCode == null ? "" : proposedCode.strip();
    }

    public static void clear() {
        set(Set.of(), Set.of(), "");
    }

    /** The player's own choice, unless the server decides: required wins over forbidden when both list an element. */
    public static boolean hidden(String id, boolean playersChoice) {
        if (required.contains(id)) {
            return false;
        }
        return forbidden.contains(id) || playersChoice;
    }

    public static boolean locked(String id) {
        return required.contains(id) || forbidden.contains(id);
    }

    public static String proposed() {
        return proposed;
    }
}
