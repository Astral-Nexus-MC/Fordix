package com.omneria.fordix.rarity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RarityRulesTest {

    @Test
    void wildcardMatchesAnyText() {
        RarityRules rules = RarityRules.parse(List.of("twilightforest:knightmetal_* | RARE"));
        assertEquals(RarityTier.RARE, rules.tierOf("twilightforest:knightmetal_ingot"));
        assertEquals(RarityTier.RARE, rules.tierOf("twilightforest:knightmetal_sword"));
        assertNull(rules.tierOf("twilightforest:ironwood_ingot"));
    }

    @Test
    void bracesOfferChoices() {
        RarityRules rules = RarityRules.parse(List.of("minecraft:iron_{sword,axe} | UNCOMMON"));
        assertEquals(RarityTier.UNCOMMON, rules.tierOf("minecraft:iron_sword"));
        assertEquals(RarityTier.UNCOMMON, rules.tierOf("minecraft:iron_axe"));
        assertNull(rules.tierOf("minecraft:iron_pickaxe"));
    }

    @Test
    void theFirstMatchingRuleWins() {
        RarityRules rules = RarityRules.parse(List.of("minecraft:*diamond* | RARE", "minecraft:diamond_sword | EPIC"));
        assertEquals(RarityTier.RARE, rules.tierOf("minecraft:diamond_sword"));
    }

    @Test
    void matchingIsOnTheWholeId() {
        RarityRules rules = RarityRules.parse(List.of("minecraft:coal | COMMON"));
        assertNull(rules.tierOf("minecraft:coal_ore"));
    }

    @Test
    void invalidLinesAreRejected() {
        assertNull(RarityRules.parseLine("no separator"));
        assertNull(RarityRules.parseLine("minecraft:dirt | SHINY"));
        assertNull(RarityRules.parseLine("dirt | RARE"));
        assertNull(RarityRules.parseLine("minecraft:{iron | RARE"));
        assertNotNull(RarityRules.parseLine("minecraft:dirt | rare"));
    }

    @Test
    void defaultRulesFollowTheAgreedScale() {
        RarityRules rules = RarityRules.parse(RarityRules.DEFAULT_LINES);
        assertEquals(RarityTier.RARE, rules.tierOf("minecraft:diamond_helmet"));
        assertEquals(RarityTier.RARE, rules.tierOf("minecraft:deepslate_emerald_ore"));
        assertEquals(RarityTier.EPIC, rules.tierOf("minecraft:netherite_sword"));
        assertEquals(RarityTier.UNCOMMON, rules.tierOf("minecraft:iron_ingot"));
        assertEquals(RarityTier.UNCOMMON, rules.tierOf("minecraft:golden_boots"));
        assertEquals(RarityTier.UNCOMMON, rules.tierOf("create:brass_ingot"));
        assertEquals(RarityTier.EPIC, rules.tierOf("twilightforest:fiery_ingot"));
        assertEquals(RarityTier.RARE, rules.tierOf("twilightforest:steeleaf_axe"));
        assertEquals(RarityTier.LEGENDARY, rules.tierOf("twilightforest:naga_trophy"));
        assertEquals(RarityTier.LEGENDARY, rules.tierOf("minecraft:dragon_head"));
        assertEquals(RarityTier.COMMON, rules.tierOf("minecraft:wooden_sword"));
        assertNull(rules.tierOf("create:zinc_ingot"));
        assertEquals(RarityTier.UNCOMMON, rules.tierOf("twilightforest:ironwood_boots"));
    }

    @Test
    void everyDefaultLineIsValid() {
        for (String line : RarityRules.DEFAULT_LINES) {
            assertNotNull(RarityRules.parseLine(line), line);
        }
    }

    @Test
    void tiersBumpAndStayWithinTheScale() {
        assertEquals(RarityTier.EPIC, RarityTier.RARE.bump(1));
        assertEquals(RarityTier.LEGENDARY, RarityTier.RARE.bump(5));
        assertEquals(RarityTier.COMMON, RarityTier.UNCOMMON.bump(-3));
    }

    @Test
    void enchantPowerRaisesTheTierOnStrongEnchantments() {
        double light = EnchantPower.score(List.of(new EnchantPower.Entry("COMMON", 1, 5, false, false)));
        assertEquals(0, EnchantPower.tiersAdded(light));
        double strong = EnchantPower.score(List.of(
                new EnchantPower.Entry("COMMON", 5, 5, false, false),
                new EnchantPower.Entry("UNCOMMON", 3, 3, false, false),
                new EnchantPower.Entry("RARE", 1, 1, false, false)));
        assertEquals(6.0, strong, 1e-9);
        assertEquals(1, EnchantPower.tiersAdded(strong));
        assertEquals(2, EnchantPower.tiersAdded(9.5));
    }

    @Test
    void cursesWeighNothingAndTreasureWeighsMore() {
        assertEquals(0.0, EnchantPower.score(List.of(new EnchantPower.Entry("VERY_RARE", 1, 1, false, true))), 1e-9);
        assertEquals(3.0, EnchantPower.score(List.of(new EnchantPower.Entry("UNCOMMON", 1, 1, true, false))), 1e-9);
    }
}
