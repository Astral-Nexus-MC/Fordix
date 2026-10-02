package com.omneria.fordix.client.tooltip;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EnchantEffectsTest {

    private static void assertEffect(String key, List<String> args, EnchantEffects.Effect effect) {
        assertEquals(key, effect.key());
        assertEquals(args, effect.args());
    }

    @Test
    void sharpnessFiveAddsThreeDamage() {
        assertEffect("enchant.minecraft.sharpness", List.of("3"), EnchantEffects.describe("minecraft:sharpness", 5));
        assertEffect("enchant.minecraft.sharpness", List.of("1"), EnchantEffects.describe("minecraft:sharpness", 1));
    }

    @Test
    void protectionIsFourPercentPerLevel() {
        assertEffect("enchant.minecraft.protection", List.of("16"), EnchantEffects.describe("minecraft:protection", 4));
    }

    @Test
    void efficiencyIsLevelSquaredPlusOne() {
        assertEffect("enchant.minecraft.efficiency", List.of("10"), EnchantEffects.describe("minecraft:efficiency", 3));
    }

    @Test
    void fortuneTwoAveragesOneAndThreeQuarters() {
        assertEquals(1.75, EnchantEffects.fortuneAverage(2), 1e-9);
        assertEffect("enchant.minecraft.fortune", List.of("1,75"), EnchantEffects.describe("minecraft:fortune", 2));
    }

    @Test
    void unbreakingMultipliesDurability() {
        assertEffect("enchant.minecraft.unbreaking", List.of("4"), EnchantEffects.describe("minecraft:unbreaking", 3));
    }

    @Test
    void piercingSwitchesToThePluralFromLevelTwo() {
        assertEffect("enchant.minecraft.piercing", List.of("1"), EnchantEffects.describe("minecraft:piercing", 1));
        assertEffect("enchant.minecraft.piercing.many", List.of("3"), EnchantEffects.describe("minecraft:piercing", 3));
    }

    @Test
    void unknownEnchantmentsHaveNoDescription() {
        assertNull(EnchantEffects.describe("somemod:custom", 2));
    }

    @Test
    void decimalsUseTheSeparatorAndNoTrailingZeros() {
        assertEquals("2,5", EnchantEffects.fmt(2.5));
        assertEquals("0,25", EnchantEffects.fmt(0.25));
        assertEquals("7", EnchantEffects.fmt(7.0));
        AttributeMath.separator = () -> '.';
        try {
            assertEquals("2.5", EnchantEffects.fmt(2.5));
        } finally {
            AttributeMath.separator = () -> ',';
        }
    }

    @Test
    void describesMoreVanillaAndModdedEnchantments() {
        assertEffect("enchant.minecraft.lure", List.of("5"), EnchantEffects.describe("minecraft:lure", 1));
        assertEffect("enchant.minecraft.depth_strider", List.of("100"), EnchantEffects.describe("minecraft:depth_strider", 3));
        assertEffect("enchant.minecraft.soul_speed", List.of("40,5"), EnchantEffects.describe("minecraft:soul_speed", 1));
        assertEffect("enchant.twilightforest.chill_aura", List.of(), EnchantEffects.describe("twilightforest:chill_aura", 2));
        assertNull(EnchantEffects.describe("somemod:unknown", 1));
    }
}
