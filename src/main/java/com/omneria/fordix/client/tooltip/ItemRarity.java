package com.omneria.fordix.client.tooltip;

import com.omneria.fordix.Tr;
import com.omneria.fordix.rarity.EnchantPower;
import com.omneria.fordix.rarity.RarityTier;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Fordix rarity of an item stack. An item a rarity rule matches (see {@code RarityRules}: ores,
 * ingots, tools and armor of a material, boss trophies...) takes the rule's tier, raised one or two
 * tiers by strong enchantments. Any other item keeps its own rarity - vanilla's tiers, or a mod's -
 * shown with Fordix's colors, and tiers the scale doesn't know keep the rarity's own color.
 */
public final class ItemRarity {
    private ItemRarity() {
    }

    private record Resolved(String label, int color, boolean notable, int rank) {
    }

    /** Opaque ARGB color of the stack's rarity. */
    public static int color(ItemStack stack) {
        return resolve(stack).color();
    }

    public static String label(ItemStack stack) {
        return resolve(stack).label();
    }

    /** 0 for a common item, up to 4 for a legendary one: what the slot's background and its movement follow. */
    public static int rank(ItemStack stack) {
        return stack.isEmpty() ? 0 : resolve(stack).rank();
    }

    /** Commons stay unmarked: a frame on every plain item would drown the rare ones. */
    public static boolean isNotable(ItemStack stack) {
        return !stack.isEmpty() && resolve(stack).notable();
    }

    /** The rarity's name in the game language: the five known ones are translated, others keep their own name. */
    private static String labelOf(String rarityName) {
        String name = rarityName.toLowerCase(java.util.Locale.ROOT);
        if (name.contains("legend")) {
            return Tr.s("rarity.legendary");
        }
        return switch (name) {
            case "common", "uncommon", "rare", "epic" -> Tr.s("rarity." + name);
            default -> RarityPalette.labelOf(rarityName);
        };
    }

    private static Resolved resolve(ItemStack stack) {
        Optional<RarityTier> ruled = ServerRarities.tierOf(stack.getItem());
        if (ruled.isPresent() && !(stack.getItem() instanceof EnchantedBookItem)) {
            RarityTier tier = ruled.get().bump(EnchantPower.tiersAdded(EnchantPower.score(enchantmentsOf(stack))));
            return new Resolved(labelOf(tier.name()), RarityPalette.colorOf(tier.name()), tier != RarityTier.COMMON, tier.ordinal());
        }
        Rarity rarity = stack.getRarity();
        Integer color = RarityPalette.colorOf(rarity.name());
        if (color == null) {
            Integer own = rarity.color.getColor();
            color = own == null ? RarityPalette.COMMON : 0xFF000000 | own;
        }
        RarityTier known = RarityTier.fromName(rarity.name());
        int rank = known != null ? known.ordinal() : rarity.name().toUpperCase(java.util.Locale.ROOT).contains("LEGEND") ? 4
                : "COMMON".equals(rarity.name()) ? 0 : 2;
        return new Resolved(labelOf(rarity.name()), color, !"COMMON".equals(rarity.name()), rank);
    }

    private static List<EnchantPower.Entry> enchantmentsOf(ItemStack stack) {
        List<EnchantPower.Entry> entries = new ArrayList<>();
        if (!stack.isEnchanted()) {
            return entries;
        }
        for (Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.getEnchantments(stack).entrySet()) {
            Enchantment enchantment = entry.getKey();
            entries.add(new EnchantPower.Entry(enchantment.getRarity().name(), entry.getValue(), enchantment.getMaxLevel(),
                    enchantment.isTreasureOnly(), enchantment.isCurse()));
        }
        return entries;
    }
}
