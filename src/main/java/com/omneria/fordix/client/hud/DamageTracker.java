package com.omneria.fordix.client.hud;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Notices health changes from one tick to the next, per creature, and writes them as floating numbers. Pure Java, unit tested. */
public final class DamageTracker {
    private final Map<Integer, Float> last = new HashMap<>();

    /** The change since this creature was last seen (negative: damage); 0 the first time. */
    public float update(int id, float health) {
        Float before = last.put(id, health);
        return before == null ? 0.0F : health - before;
    }

    /** Forgets every creature not in the set (gone from the world). */
    public void retain(Set<Integer> alive) {
        last.keySet().retainAll(alive);
    }

    public void clear() {
        last.clear();
    }

    /** "5", "12", "2.5": whole numbers as such, one decimal for the small fractional ones. */
    public static String format(float amount) {
        float magnitude = Math.abs(amount);
        if (magnitude >= 10.0F || Math.abs(magnitude - Math.round(magnitude)) < 0.05F) {
            return Integer.toString(Math.round(magnitude));
        }
        return String.format(Locale.ROOT, "%.1f", magnitude);
    }
}
