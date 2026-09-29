package com.omneria.fordix.client.tooltip;

import java.util.Locale;

/**
 * What an enchantment actually does, in numbers, for the item tooltip. Formulas are Minecraft
 * 1.20.1's; enchantments it doesn't know (other mods') return null and show as a plain line.
 * Plain Java, unit tested.
 */
public final class EnchantEffects {
    private EnchantEffects() {
    }

    /** A short French description of the effect at this level, or null when unknown. */
    public static String describe(String id, int level) {
        return switch (id) {
            case "minecraft:sharpness" -> "+" + fmt(0.5 * level + 0.5) + " dégâts";
            case "minecraft:smite" -> "+" + fmt(2.5 * level) + " dégâts (morts-vivants)";
            case "minecraft:bane_of_arthropods" -> "+" + fmt(2.5 * level) + " dégâts (arthropodes)";
            case "minecraft:impaling" -> "+" + fmt(2.5 * level) + " dégâts (aquatiques)";
            case "minecraft:fire_aspect" -> "brûle " + 4 * level + " s";
            case "minecraft:knockback" -> "recul accru";
            case "minecraft:sweeping" -> "balayage à " + fmt(100.0 * level / (level + 1)) + " %";
            case "minecraft:looting" -> "jusqu'à +" + level + " butin";
            case "minecraft:protection" -> "-" + 4 * level + " % dégâts subis";
            case "minecraft:fire_protection" -> "-" + 8 * level + " % dégâts de feu";
            case "minecraft:blast_protection" -> "-" + 8 * level + " % explosions";
            case "minecraft:projectile_protection" -> "-" + 8 * level + " % projectiles";
            case "minecraft:feather_falling" -> "-" + 12 * level + " % dégâts de chute";
            case "minecraft:thorns" -> 15 * level + " % · renvoie 1–4 dégâts";
            case "minecraft:respiration" -> "+" + 15 * level + " s d'air";
            case "minecraft:swift_sneak" -> "+" + 15 * level + " % vitesse accroupi";
            case "minecraft:frost_walker" -> "gèle l'eau sur " + Math.min(16, level + 2) + " blocs";
            case "minecraft:efficiency" -> "+" + (level * level + 1) + " vitesse de minage";
            case "minecraft:fortune" -> "butin moyen ×" + fmt(fortuneAverage(level));
            case "minecraft:unbreaking" -> "durabilité ×" + (level + 1);
            case "minecraft:power" -> "+" + 25 * (level + 1) + " % dégâts d'arc";
            case "minecraft:quick_charge" -> "-" + fmt(0.25 * level) + " s de chargement";
            case "minecraft:piercing" -> "traverse " + level + " cible" + (level > 1 ? "s" : "");
            case "minecraft:multishot" -> "3 flèches";
            case "minecraft:mending" -> "se répare avec l'XP";
            case "minecraft:silk_touch" -> "récolte les blocs intacts";
            case "minecraft:aqua_affinity" -> "minage normal sous l'eau";
            case "minecraft:infinity" -> "flèches infinies";
            case "minecraft:flame" -> "flèches enflammées";
            case "minecraft:channeling" -> "invoque la foudre";
            case "minecraft:binding_curse" -> "impossible à retirer";
            case "minecraft:vanishing_curse" -> "disparaît à la mort";
            default -> null;
        };
    }

    /**
     * Average drop multiplier of Fortune: the game rolls 0..level+1 and drops max(1, roll) times,
     * so the average is (1 + sum of 1..level+1) / (level+2).
     */
    public static double fortuneAverage(int level) {
        return (1.0 + (level + 1) * (level + 2) / 2.0) / (level + 2);
    }

    /** Whole numbers without decimals, others with a decimal comma: 3, 2,5, 1,75. */
    static String fmt(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        String text = String.format(Locale.ROOT, "%.2f", value);
        text = text.replaceAll("0+$", "").replaceAll("\\.$", "");
        return text.replace('.', ',');
    }
}
