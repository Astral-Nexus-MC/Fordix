package com.omneria.fordix.client.tooltip;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EnchantEffectsTest {

    @Test
    void sharpnessFiveAddsThreeDamage() {
        assertEquals("+3 dégâts", EnchantEffects.describe("minecraft:sharpness", 5));
        assertEquals("+1 dégâts", EnchantEffects.describe("minecraft:sharpness", 1));
    }

    @Test
    void protectionIsFourPercentPerLevel() {
        assertEquals("-16 % dégâts subis", EnchantEffects.describe("minecraft:protection", 4));
    }

    @Test
    void efficiencyIsLevelSquaredPlusOne() {
        assertEquals("+10 vitesse de minage", EnchantEffects.describe("minecraft:efficiency", 3));
    }

    @Test
    void fortuneTwoAveragesOneAndThreeQuarters() {
        assertEquals(1.75, EnchantEffects.fortuneAverage(2), 1e-9);
        assertEquals("butin moyen ×1,75", EnchantEffects.describe("minecraft:fortune", 2));
    }

    @Test
    void unbreakingMultipliesDurability() {
        assertEquals("durabilité ×4", EnchantEffects.describe("minecraft:unbreaking", 3));
    }

    @Test
    void unknownEnchantmentsHaveNoDescription() {
        assertNull(EnchantEffects.describe("somemod:custom", 2));
    }

    @Test
    void decimalsUseACommaAndNoTrailingZeros() {
        assertEquals("2,5", EnchantEffects.fmt(2.5));
        assertEquals("0,25", EnchantEffects.fmt(0.25));
        assertEquals("7", EnchantEffects.fmt(7.0));
    }

    @Test
    void describesMoreVanillaAndModdedEnchantments() {
        assertEquals("-5 s d'attente du poisson", EnchantEffects.describe("minecraft:lure", 1));
        assertEquals("ralentissement dans l'eau -100 %", EnchantEffects.describe("minecraft:depth_strider", 3));
        assertEquals("+40,5 % vitesse (sable des âmes)", EnchantEffects.describe("minecraft:soul_speed", 1));
        assertEquals("ralentit les attaquants", EnchantEffects.describe("twilightforest:chill_aura", 2));
        assertNull(EnchantEffects.describe("somemod:unknown", 1));
    }
}
