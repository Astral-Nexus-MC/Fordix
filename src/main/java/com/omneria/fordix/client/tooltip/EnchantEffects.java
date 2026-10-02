package com.omneria.fordix.client.tooltip;

import java.util.ArrayList;
import java.util.List;

/**
 * What an enchantment actually does, in numbers, for the item tooltip. Formulas are Minecraft
 * 1.20.1's; enchantments it doesn't know (other mods') return null and show as a plain line. The
 * description is a language key (under {@code fordix.}) with its numbers, so it follows the game
 * language. Plain Java, unit tested.
 */
public final class EnchantEffects {
    private EnchantEffects() {
    }

    /** A language key and the numbers to put in it. */
    public record Effect(String key, List<String> args) {
    }

    private static Effect effect(String key, Object... args) {
        List<String> list = new ArrayList<>();
        for (Object arg : args) {
            list.add(String.valueOf(arg));
        }
        return new Effect(key, list);
    }

    /** The effect at this level, or null when unknown. */
    public static Effect describe(String id, int level) {
        return switch (id) {
            case "minecraft:sharpness" -> effect("enchant.minecraft.sharpness", fmt(0.5 * level + 0.5));
            case "minecraft:smite" -> effect("enchant.minecraft.smite", fmt(2.5 * level));
            case "minecraft:bane_of_arthropods" -> effect("enchant.minecraft.bane_of_arthropods", fmt(2.5 * level));
            case "minecraft:impaling" -> effect("enchant.minecraft.impaling", fmt(2.5 * level));
            case "minecraft:fire_aspect" -> effect("enchant.minecraft.fire_aspect", 4 * level);
            case "minecraft:knockback" -> effect("enchant.minecraft.knockback");
            case "minecraft:sweeping" -> effect("enchant.minecraft.sweeping", fmt(100.0 * level / (level + 1)));
            case "minecraft:looting" -> effect("enchant.minecraft.looting", level);
            case "minecraft:protection" -> effect("enchant.minecraft.protection", 4 * level);
            case "minecraft:fire_protection" -> effect("enchant.minecraft.fire_protection", 8 * level);
            case "minecraft:blast_protection" -> effect("enchant.minecraft.blast_protection", 8 * level);
            case "minecraft:projectile_protection" -> effect("enchant.minecraft.projectile_protection", 8 * level);
            case "minecraft:feather_falling" -> effect("enchant.minecraft.feather_falling", 12 * level);
            case "minecraft:thorns" -> effect("enchant.minecraft.thorns", 15 * level);
            case "minecraft:respiration" -> effect("enchant.minecraft.respiration", 15 * level);
            case "minecraft:swift_sneak" -> effect("enchant.minecraft.swift_sneak", 15 * level);
            case "minecraft:frost_walker" -> effect("enchant.minecraft.frost_walker", Math.min(16, level + 2));
            case "minecraft:efficiency" -> effect("enchant.minecraft.efficiency", level * level + 1);
            case "minecraft:fortune" -> effect("enchant.minecraft.fortune", fmt(fortuneAverage(level)));
            case "minecraft:unbreaking" -> effect("enchant.minecraft.unbreaking", level + 1);
            case "minecraft:power" -> effect("enchant.minecraft.power", 25 * (level + 1));
            case "minecraft:quick_charge" -> effect("enchant.minecraft.quick_charge", fmt(0.25 * level));
            case "minecraft:piercing" -> effect(level > 1 ? "enchant.minecraft.piercing.many" : "enchant.minecraft.piercing", level);
            case "minecraft:multishot" -> effect("enchant.minecraft.multishot");
            case "minecraft:mending" -> effect("enchant.minecraft.mending");
            case "minecraft:silk_touch" -> effect("enchant.minecraft.silk_touch");
            case "minecraft:aqua_affinity" -> effect("enchant.minecraft.aqua_affinity");
            case "minecraft:infinity" -> effect("enchant.minecraft.infinity");
            case "minecraft:flame" -> effect("enchant.minecraft.flame");
            case "minecraft:channeling" -> effect("enchant.minecraft.channeling");
            case "minecraft:depth_strider" -> effect("enchant.minecraft.depth_strider", fmt(100.0 * level / 3.0));
            case "minecraft:soul_speed" -> effect("enchant.minecraft.soul_speed", fmt(30.0 + 10.5 * level));
            case "minecraft:punch" -> effect("enchant.minecraft.punch", level);
            case "minecraft:luck_of_the_sea" -> effect("enchant.minecraft.luck_of_the_sea", level);
            case "minecraft:lure" -> effect("enchant.minecraft.lure", 5 * level);
            case "minecraft:loyalty" -> effect("enchant.minecraft.loyalty", level);
            case "minecraft:riptide" -> effect("enchant.minecraft.riptide", level);
            case "create:capacity" -> effect("enchant.create.capacity");
            case "create:potato_recovery" -> effect("enchant.create.potato_recovery");
            case "twilightforest:fire_react" -> effect("enchant.twilightforest.fire_react");
            case "twilightforest:chill_aura" -> effect("enchant.twilightforest.chill_aura");
            case "minecraft:binding_curse" -> effect("enchant.minecraft.binding_curse");
            case "minecraft:vanishing_curse" -> effect("enchant.minecraft.vanishing_curse");
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

    /** Whole numbers without decimals, others with the game language's decimal separator: 3, 2,5, 1,75. */
    static String fmt(double value) {
        return AttributeMath.number(value);
    }
}
