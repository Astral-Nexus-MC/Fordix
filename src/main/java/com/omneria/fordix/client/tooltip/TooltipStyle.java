package com.omneria.fordix.client.tooltip;

import com.omneria.fordix.FordixConfig;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;

/**
 * Item tooltip lines in the Fordix style: each enchantment is colored by its rarity (the
 * same scale as items) and followed by what it does in numbers. Works for any enchantment
 * from any mod: rarity, curse and treasure flags come from the enchantment itself, and the
 * numbers only for those {@link EnchantEffects} knows.
 */
public final class TooltipStyle {
    public static final int COMMON = 0xFFB8C4CC;
    public static final int UNCOMMON = 0xFF5ED68A;
    public static final int RARE = 0xFF4EA3FF;
    public static final int VERY_RARE = 0xFFC58BFF;
    public static final int CURSE = 0xFFE0524A;
    public static final int TREASURE = 0xFFE3C045;
    private static final int EFFECT = 0xFFC9A8FF;

    private TooltipStyle() {
    }

    /** The color an enchantment is shown in: curse and treasure win over its rarity tier. */
    public static int colorOf(Enchantment enchantment) {
        if (enchantment.isCurse()) {
            return CURSE;
        }
        if (enchantment.isTreasureOnly()) {
            return TREASURE;
        }
        return switch (enchantment.getRarity()) {
            case COMMON -> COMMON;
            case UNCOMMON -> UNCOMMON;
            case RARE -> RARE;
            case VERY_RARE -> VERY_RARE;
        };
    }

    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!FordixConfig.VOIDFRAME_VANILLA_SCREENS.get()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        List<Component> lines = event.getToolTip();
        if (FordixConfig.VOIDFRAME_TOOLTIPS.get()) {
            removeVanillaModifierLines(stack, lines);
        }
        for (Map.Entry<Enchantment, Integer> entry : enchantmentsOf(stack).entrySet()) {
            Enchantment enchantment = entry.getKey();
            int level = entry.getValue();
            String vanillaLine = enchantment.getFullname(level).getString();
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).getString().equals(vanillaLine)) {
                    lines.set(i, styled(enchantment, level));
                    break;
                }
            }
        }
    }

    /**
     * Drops the "When in main hand: +7 Attack Damage" block the game writes, since the tooltip
     * draws attributes as bars. Each slot's block is a blank line, its title, then one line per modifier.
     */
    private static void removeVanillaModifierLines(ItemStack stack, List<Component> lines) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            int count = stack.getAttributeModifiers(slot).size();
            if (count == 0) {
                continue;
            }
            String title = Component.translatable("item.modifiers." + slot.getName()).getString();
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).getString().equals(title)) {
                    int from = i > 0 && lines.get(i - 1).getString().isEmpty() ? i - 1 : i;
                    int to = Math.min(lines.size(), i + 1 + count);
                    lines.subList(from, to).clear();
                    break;
                }
            }
        }
    }

    private static MutableComponent styled(Enchantment enchantment, int level) {
        int color = colorOf(enchantment);
        String star = enchantment.isTreasureOnly() && !enchantment.isCurse() ? "★ " : "";
        MutableComponent line = Component.literal(star + enchantment.getFullname(level).getString())
                .withStyle(style -> style.withColor(color & 0xFFFFFF));
        var key = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        String effect = key == null ? null : EnchantEffects.describe(key.toString(), level);
        if (effect != null) {
            line.append(Component.literal("  " + effect).withStyle(style -> style.withColor(EFFECT & 0xFFFFFF)));
        }
        return line;
    }

    /** Enchantments on the stack; books keep theirs under a different tag than tools and armor. */
    private static Map<Enchantment, Integer> enchantmentsOf(ItemStack stack) {
        if (stack.getItem() instanceof EnchantedBookItem) {
            ListTag stored = EnchantedBookItem.getEnchantments(stack);
            return EnchantmentHelper.deserializeEnchantments(stored);
        }
        return EnchantmentHelper.getEnchantments(stack);
    }
}
