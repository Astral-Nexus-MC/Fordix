package com.omneria.fordix.client.tooltip;

import java.util.Locale;

/**
 * Fordix's item rarity scale, by the rarity's name so modded tiers fit too: common is white,
 * rare blue, epic violet, legendary orange (uncommon green). A name it doesn't know gets null,
 * and the caller keeps the rarity's own color. Plain Java, unit tested.
 */
public final class RarityPalette {
    public static final int COMMON = 0xFFEDF2F5;
    public static final int UNCOMMON = 0xFF5ED68A;
    public static final int RARE = 0xFF4EA3FF;
    public static final int EPIC = 0xFFC58BFF;
    public static final int LEGENDARY = 0xFFFF9A2E;

    private RarityPalette() {
    }

    /** ARGB color for a rarity name (vanilla's COMMON...EPIC, or any name containing LEGEND); null when unknown. */
    public static Integer colorOf(String rarityName) {
        String name = rarityName.toUpperCase(Locale.ROOT);
        if (name.contains("LEGEND")) {
            return LEGENDARY;
        }
        return switch (name) {
            case "COMMON" -> COMMON;
            case "UNCOMMON" -> UNCOMMON;
            case "RARE" -> RARE;
            case "EPIC" -> EPIC;
            default -> null;
        };
    }

    /** French label for a rarity name; unknown names are capitalized as they are. */
    public static String labelOf(String rarityName) {
        String name = rarityName.toUpperCase(Locale.ROOT);
        if (name.contains("LEGEND")) {
            return "Légendaire";
        }
        return switch (name) {
            case "COMMON" -> "Commun";
            case "UNCOMMON" -> "Peu commun";
            case "RARE" -> "Rare";
            case "EPIC" -> "Épique";
            default -> rarityName.isEmpty() ? rarityName
                    : rarityName.substring(0, 1).toUpperCase(Locale.ROOT) + rarityName.substring(1).toLowerCase(Locale.ROOT);
        };
    }
}
