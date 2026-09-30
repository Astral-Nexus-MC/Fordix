package com.omneria.fordix.rarity;

import java.util.List;

/**
 * How strongly an item is enchanted, and how many rarity tiers that adds: each enchantment weighs
 * its own rarity (common 1 ... very rare 4, +1 for treasure) scaled by how close its level is to the
 * maximum; curses weigh nothing. 4 or more raises the item one tier, 9 or more two.
 * Plain Java, unit tested.
 */
public final class EnchantPower {
    public static final double ONE_TIER = 4.0;
    public static final double TWO_TIERS = 9.0;

    private EnchantPower() {
    }

    public record Entry(String rarityName, int level, int maxLevel, boolean treasure, boolean curse) {
    }

    public static double score(List<Entry> enchantments) {
        double total = 0.0;
        for (Entry entry : enchantments) {
            if (entry.curse()) {
                continue;
            }
            double weight = switch (entry.rarityName().toUpperCase()) {
                case "COMMON" -> 1.0;
                case "UNCOMMON" -> 2.0;
                case "RARE" -> 3.0;
                case "VERY_RARE" -> 4.0;
                default -> 2.0;
            } + (entry.treasure() ? 1.0 : 0.0);
            total += weight * Math.min(1.0, entry.level() / (double) Math.max(1, entry.maxLevel()));
        }
        return total;
    }

    public static int tiersAdded(double score) {
        return score >= TWO_TIERS ? 2 : score >= ONE_TIER ? 1 : 0;
    }
}
