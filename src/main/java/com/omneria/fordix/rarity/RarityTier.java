package com.omneria.fordix.rarity;

import java.util.Locale;

/** Fordix's five rarity tiers, lowest first. Plain Java, unit tested. */
public enum RarityTier {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY;

    /** The tier called {@code name} (any case), or null. */
    public static RarityTier fromName(String name) {
        if (name == null) {
            return null;
        }
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** This tier raised by {@code steps} (or lowered, if negative), kept within COMMON..LEGENDARY. */
    public RarityTier bump(int steps) {
        RarityTier[] all = values();
        return all[Math.max(0, Math.min(all.length - 1, ordinal() + steps))];
    }
}
