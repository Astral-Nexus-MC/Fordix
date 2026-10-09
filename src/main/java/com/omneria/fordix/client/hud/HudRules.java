package com.omneria.fordix.client.hud;

import com.omneria.fordix.hud.SituationTable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What the server this player is on requires of the HUD: elements that can never be hidden (life, hotbar), elements that are
 * always hidden, and a layout it suggests. Pure Java, unit tested; {@link #hidden} is what every "is it hidden" test goes through.
 */
public final class HudRules {
    private static volatile Set<String> required = Set.of();
    private static volatile Set<String> forbidden = Set.of();
    private static volatile String proposed = "";
    private static volatile boolean situationsForced;
    private static volatile String autoMode = "player";
    private static volatile Map<String, float[]> situationTable = Map.of();
    private static volatile String[] situationProfiles = new String[SituationTable.SITUATIONS.size()];

    private HudRules() {
    }

    public static void set(Collection<String> requiredIds, Collection<String> forbiddenIds, String proposedCode) {
        required = Set.copyOf(requiredIds);
        forbidden = Set.copyOf(forbiddenIds);
        proposed = proposedCode == null ? "" : proposedCode.strip();
    }

    /** The server's imposed situations: the table and the profile code of each situation (empty text for none). */
    public static void setSituations(boolean forced, List<String> tableLines, List<String> profileLines) {
        situationsForced = forced;
        situationTable = forced ? SituationTable.rows(tableLines) : Map.of();
        situationProfiles = forced ? SituationTable.profiles(profileLines) : emptyCodes();
    }

    private static String[] emptyCodes() {
        String[] codes = new String[SituationTable.SITUATIONS.size()];
        java.util.Arrays.fill(codes, "");
        return codes;
    }

    /** What the server says about the automatic situations: "on" (always), "off" (never) or "player" (the player decides). */
    public static void setAutoMode(String mode) {
        autoMode = "on".equals(mode) || "off".equals(mode) ? mode : "player";
    }

    public static String autoMode() {
        return autoMode;
    }

    public static boolean situationsForced() {
        return situationsForced;
    }

    /** The server's value for a panel in a situation (by {@link SituationTable#SITUATIONS} order), or -1 when it has none. */
    public static float situationFactor(int situation, String panel) {
        float[] row = situationTable.get(panel);
        return row == null ? -1.0F : row[situation];
    }

    /** The share code of the layout the server loads in a situation, empty for none. */
    public static String situationProfile(int situation) {
        String[] codes = situationProfiles;
        return codes.length > situation && codes[situation] != null ? codes[situation] : "";
    }

    public static void clear() {
        set(Set.of(), Set.of(), "");
        setSituations(false, List.of(), List.of());
        autoMode = "player";
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
