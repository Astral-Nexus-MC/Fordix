package com.omneria.fordix.client.tooltip;

import java.util.List;
import java.util.Locale;

/**
 * Arithmetic behind the tooltip's attribute bars: the value an attribute ends up at once an
 * item's modifiers are applied (the game's order: additions, then multiply-base, then
 * multiply-total), how full its bar is, and how it's written. Plain Java, unit tested.
 */
public final class AttributeMath {
    private AttributeMath() {
    }

    /** One attribute modifier; {@code operation} is 0 (add), 1 (multiply base) or 2 (multiply total). */
    public record Mod(int operation, double amount) {
    }

    public static double total(double base, List<Mod> mods) {
        double added = base;
        double multiplyBase = 0.0;
        for (Mod mod : mods) {
            switch (mod.operation()) {
                case 0 -> added += mod.amount();
                case 1 -> multiplyBase += mod.amount();
                default -> {
                }
            }
        }
        double value = added * (1.0 + multiplyBase);
        for (Mod mod : mods) {
            if (mod.operation() == 2) {
                value *= 1.0 + mod.amount();
            }
        }
        return value;
    }

    /** How full a bar is, 0..1: against a known scale, or a soft curve when the attribute's range isn't known. */
    public static float fraction(double value, double scale) {
        double magnitude = Math.abs(value);
        double ratio = scale > 0 ? magnitude / scale : magnitude / (magnitude + 10.0);
        return (float) Math.max(0.0, Math.min(1.0, ratio));
    }

    /** The decimal separator of the game language: set by the client, a comma until then. */
    public static volatile java.util.function.Supplier<Character> separator = () -> ',';

    /** Numbers as the game shows them: up to two decimals, the language's separator, no trailing zeros ("1,6", "7"). */
    public static String number(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        String text = String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
        return text.replace('.', separator.get());
    }

    /** A signed difference: "+2", "-0,4", or "=" when there is none. */
    public static String delta(double difference) {
        if (Math.abs(difference) < 0.005) {
            return "=";
        }
        return (difference > 0 ? "+" : "-") + number(Math.abs(difference));
    }
}
