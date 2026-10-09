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
    private static volatile boolean situationsKnown;
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
    public static void setSituations(List<String> tableLines, List<String> profileLines) {
        situationsKnown = true;
        situationTable = SituationTable.rows(tableLines);
        situationProfiles = SituationTable.profiles(profileLines);
    }

    private static String[] emptyCodes() {
        String[] codes = new String[SituationTable.SITUATIONS.size()];
        java.util.Arrays.fill(codes, "");
        return codes;
    }

    /** Whether the server's situations have been received (before that, the built-in table applies). */
    public static boolean situationsKnown() {
        return situationsKnown;
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
        situationsKnown = false;
        situationTable = Map.of();
        situationProfiles = emptyCodes();
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
