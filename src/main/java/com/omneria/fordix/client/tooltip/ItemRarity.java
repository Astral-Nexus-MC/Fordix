package com.omneria.fordix.client.tooltip;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/** The Fordix color and label of an item's rarity; tiers the scale doesn't know keep the rarity's own color. */
public final class ItemRarity {
    private ItemRarity() {
    }

    /** Opaque ARGB color of the stack's rarity. */
    public static int color(ItemStack stack) {
        Rarity rarity = stack.getRarity();
        Integer color = RarityPalette.colorOf(rarity.name());
        if (color != null) {
            return color;
        }
        Integer own = rarity.color.getColor();
        return own == null ? RarityPalette.COMMON : 0xFF000000 | own;
    }

    public static String label(ItemStack stack) {
        return RarityPalette.labelOf(stack.getRarity().name());
    }

    /** Commons stay unmarked: a frame on every plain item would drown the rare ones. */
    public static boolean isNotable(ItemStack stack) {
        return !stack.isEmpty() && !"COMMON".equals(stack.getRarity().name());
    }
}
