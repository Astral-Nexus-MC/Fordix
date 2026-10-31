package com.omneria.fordix.client.tooltip;

import com.omneria.fordix.Tr;
import com.google.common.collect.Multimap;
import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.Glass;
import com.omneria.fordix.client.hud.HudStyle;
import com.omneria.fordix.client.gui.ModNames;
import com.omneria.fordix.client.gui.vanilla.Chevron;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector2ic;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws item tooltips in the Fordix layout: a header with the item's icon, its name in the color
 * of its rarity and a "type - rarity - mod" line, then one chevron bar per attribute (with the
 * change against what you currently wear or hold, and the enchantment's share in violet), then
 * the remaining lines the game and other mods added (enchantments, lore...), then durability.
 * Everything is computed from the stack itself, so items and attributes from any mod get bars.
 */
public final class ItemTooltipRenderer {
    private static final int PAD = 9;
    private static final int HEADER_HEIGHT = 38;
    private static final int ROW_HEIGHT = 14;
    private static final int SECTION_HEIGHT = 16;
    private static final int LABEL_WIDTH = 80;
    private static final int VALUE_WIDTH = 46;
    private static final int DELTA_WIDTH = 34;
    private static final int BAR_MIN_WIDTH = 60;
    private static final int COMPACT_HEADER_HEIGHT = 22;
    private static final int CHIP_ROW_HEIGHT = 17;
    private static final int ENCHANT_SECTION = 0xFF8E77C8;
    private static final int ENCHANT_COLOR = 0xFFC9A8FF;
    private static final int UP = 0xFF5ED68A;
    private static final int DOWN = 0xFFE0524A;
    private static final int STEADY = 0xFF71838F;
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
            EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private record Row(String label, double value, double enchantBonus, String delta, int deltaColor,
                       float fraction, float enchantFraction, int top, int bottom) {
    }

    private record Section(Component title, List<Row> rows) {
    }

    /** The tooltip follows the compact setting of the information panels. */
    private static boolean compact() {
        try {
            return FordixConfig.HUD_COMPACT.get();
        } catch (IllegalStateException notLoadedYet) {
            return false;
        }
    }

    private ItemTooltipRenderer() {
    }

    public static void onPre(RenderTooltipEvent.Pre event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || event.getComponents().isEmpty()) {
            return;
        }
        boolean compact = compact();
        int headerHeight = compact ? COMPACT_HEADER_HEIGHT : HEADER_HEIGHT;
        int sectionHeight = compact ? 0 : SECTION_HEIGHT;
        List<Chip> chips = compact ? List.of() : chipsOf(stack);
        Font font = event.getFont();
        List<ClientTooltipComponent> components = event.getComponents();
        ClientTooltipComponent title = components.get(0);
        List<ClientTooltipComponent> rest = components.subList(1, components.size());
        List<GearCompare.Diff> diffs = new ArrayList<>();
        String[] comparedWith = new String[1];
        List<Section> sections = sections(stack, diffs, comparedWith);
        GearCompare.Verdict verdict = diffs.isEmpty() ? null : GearCompare.verdict(diffs);

        String subLine = (typeOf(stack) + " · " + ItemRarity.label(stack) + " · " + modName(stack)).toUpperCase(java.util.Locale.ROOT);
        int width = Math.max(title.getWidth(font) + 30, compact ? 0 : font.width(subLine) + 30);
        for (Chip chip : chips) {
            width = Math.max(width, chip.width(font) + 8 + font.width(chip.effect()));
        }
        for (ClientTooltipComponent component : rest) {
            width = Math.max(width, component.getWidth(font));
        }
        if (!sections.isEmpty()) {
            width = Math.max(width, LABEL_WIDTH + VALUE_WIDTH + BAR_MIN_WIDTH + DELTA_WIDTH + 12);
        }
        boolean durability = stack.isDamageableItem();
        int height = headerHeight + 4;
        for (Section section : sections) {
            height += sectionHeight + section.rows().size() * ROW_HEIGHT + 4;
        }
        String verdictLine = verdict == null ? null : Tr.s("tooltip.compare." + verdict.name().toLowerCase(java.util.Locale.ROOT), comparedWith[0]);
        if (verdictLine != null) {
            height += ROW_HEIGHT + 4;
            width = Math.max(width, font.width(verdictLine) + 12);
        }
        if (!chips.isEmpty()) {
            height += SECTION_HEIGHT + chips.size() * CHIP_ROW_HEIGHT + 4;
        }
        for (ClientTooltipComponent component : rest) {
            height += component.getHeight();
        }
        if (durability) {
            height += ROW_HEIGHT + 6;
        }
        height += 4;
        int totalWidth = width + PAD * 2;
        int totalHeight = height + PAD - 4;

        Vector2ic position = event.getTooltipPositioner().positionTooltip(event.getScreenWidth(), event.getScreenHeight(),
                event.getX(), event.getY(), totalWidth, totalHeight);
        int x = position.x();
        int y = position.y();
        event.setCanceled(true);

        GuiGraphics guiGraphics = event.getGraphics();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 400.0F);
        int accent = rarityColor(stack);
        HudStyle.panel(guiGraphics, x, y, totalWidth, totalHeight, accent & 0xFFFFFF, 246, FordixPalette.PANEL_FILL & 0xFFFFFF);
        Glass.rule(guiGraphics, x + 6, y + headerHeight - 1, totalWidth - 12, accent);

        guiGraphics.renderItem(stack, x + PAD + 4, y + (compact ? 3 : 11));
        int textX = x + PAD + 26;
        title.renderText(font, textX, y + (compact ? 6 : 9), guiGraphics.pose().last().pose(), guiGraphics.bufferSource());
        if (!compact) {
            guiGraphics.drawString(font, subLine, textX, y + 23, FordixPalette.TEXT, false);
        }

        int cursorY = y + headerHeight + 4;
        int contentX = x + PAD + 3;
        int contentWidth = width - 3;
        for (Section section : sections) {
            if (!compact) {
                Glass.label(guiGraphics, font, section.title().getString(), contentX, cursorY + 4);
            }
            cursorY += sectionHeight;
            for (Row row : section.rows()) {
                drawRow(guiGraphics, font, row, contentX, cursorY, contentWidth);
                cursorY += ROW_HEIGHT;
            }
            cursorY += 4;
        }
        if (verdictLine != null) {
            int color = verdict == GearCompare.Verdict.BETTER ? UP : verdict == GearCompare.Verdict.WORSE ? DOWN
                    : verdict == GearCompare.Verdict.MIXED ? 0xFFFFB347 : STEADY;
            String mark = verdict == GearCompare.Verdict.BETTER ? "\u25B2 " : verdict == GearCompare.Verdict.WORSE ? "\u25BC " : "\u25C6 ";
            guiGraphics.drawString(font, mark + verdictLine, contentX, cursorY + 1, color, false);
            cursorY += ROW_HEIGHT + 4;
        }
        if (!chips.isEmpty()) {
            guiGraphics.drawString(font, Tr.s("tooltip.enchantments").toUpperCase(java.util.Locale.ROOT), contentX, cursorY + 4, ENCHANT_SECTION, false);
            cursorY += SECTION_HEIGHT;
            for (Chip chip : chips) {
                drawChip(guiGraphics, font, chip, contentX, cursorY);
                cursorY += CHIP_ROW_HEIGHT;
            }
            cursorY += 4;
        }
        for (ClientTooltipComponent component : rest) {
            component.renderText(font, contentX, cursorY, guiGraphics.pose().last().pose(), guiGraphics.bufferSource());
            component.renderImage(font, contentX, cursorY, guiGraphics);
            cursorY += component.getHeight();
        }
        if (durability) {
            cursorY += 2;
            drawDurability(guiGraphics, font, stack, contentX, cursorY, contentWidth);
        }
        guiGraphics.flush();
        guiGraphics.pose().popPose();
        PatchouliLookup.draw(guiGraphics, stack, x, y);
    }

    /** One enchantment: a pill named in its rarity's color, then what it does in numbers. */
    private record Chip(String name, int color, String effect) {
        int width(Font font) {
            return font.width(name) + 14;
        }
    }

    private static List<Chip> chipsOf(ItemStack stack) {
        List<Chip> chips = new ArrayList<>();
        for (var entry : TooltipStyle.enchantmentsOf(stack).entrySet()) {
            var enchantment = entry.getKey();
            String star = enchantment.isTreasureOnly() && !enchantment.isCurse() ? "★ " : "";
            var key = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
            var described = key == null ? null : EnchantEffects.describe(key.toString(), entry.getValue());
            String effect = described == null ? null : Tr.s(described.key(), described.args().toArray());
            chips.add(new Chip(star + enchantment.getFullname(entry.getValue()).getString(), TooltipStyle.colorOf(enchantment),
                    effect == null ? "" : effect));
        }
        return chips;
    }

    private static void drawChip(GuiGraphics guiGraphics, Font font, Chip chip, int x, int y) {
        int pill = Glass.pill(guiGraphics, font, chip.name(), x, y, chip.color());
        guiGraphics.drawString(font, chip.effect(), x + pill + 6, y + 2, ENCHANT_COLOR, false);
    }

    private static void drawRow(GuiGraphics guiGraphics, Font font, Row row, int x, int y, int width) {
        guiGraphics.drawString(font, row.label(), x, y + 3, FordixPalette.TEXT, false);
        String value = AttributeMath.number(row.value() + row.enchantBonus());
        guiGraphics.drawString(font, value, x + LABEL_WIDTH + VALUE_WIDTH - 4 - font.width(value), y + 3,
                FordixPalette.TEXT_BRIGHT, false);
        int barX = x + LABEL_WIDTH + VALUE_WIDTH;
        int barWidth = width - LABEL_WIDTH - VALUE_WIDTH - DELTA_WIDTH;
        Chevron.draw(guiGraphics, barX, y + 3, barWidth, 7, true, 3, row.fraction(), row.top(), row.bottom(), false);
        if (row.enchantBonus() > 0) {
            // The enchantment's share, in violet, right after the base part of the bar.
            int inner = barWidth - 2;
            int start = Math.round(inner * row.fraction());
            int length = Math.min(inner - start, Math.max(1, Math.round(inner * row.enchantFraction())));
            guiGraphics.fill(barX + 1 + start, y + 4, barX + 1 + start + length, y + 9, ENCHANT_COLOR);
        }
        guiGraphics.drawString(font, row.delta(), barX + barWidth + 4, y + 3, row.deltaColor(), false);
    }

    private static void drawDurability(GuiGraphics guiGraphics, Font font, ItemStack stack, int x, int y, int width) {
        int max = stack.getMaxDamage();
        int left = max - stack.getDamageValue();
        guiGraphics.drawString(font, Component.translatable("item.durability", left, max), x, y + 3, FordixPalette.TEXT, false);
        int color = 0xFF000000 | stack.getBarColor();
        int barX = x + LABEL_WIDTH + VALUE_WIDTH;
        Chevron.draw(guiGraphics, barX, y + 3, width - LABEL_WIDTH - VALUE_WIDTH - DELTA_WIDTH, 7, true, 3,
                (float) left / max, Chevron.brighten(color, 40), color, false);
    }

    /** One section per equipment slot the item gives attributes in, each row an attribute. */
    private static List<Section> sections(ItemStack stack, List<GearCompare.Diff> diffs, String[] comparedWith) {
        Player player = Minecraft.getInstance().player;
        List<Section> sections = new ArrayList<>();
        for (EquipmentSlot slot : SLOTS) {
            Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(slot);
            if (modifiers.isEmpty()) {
                continue;
            }
            ItemStack worn = player == null ? ItemStack.EMPTY : player.getItemBySlot(slot);
            List<Row> rows = new ArrayList<>();
            for (Attribute attribute : modifiers.keySet()) {
                rows.add(row(stack, worn, slot, attribute, modifiers, player, diffs));
            }
            if (!worn.isEmpty() && !ItemStack.isSameItemSameTags(worn, stack) && comparedWith[0] == null) {
                comparedWith[0] = worn.getHoverName().getString();
            }
            sections.add(new Section(Component.translatable("item.modifiers." + slot.getName()), rows));
        }
        return sections;
    }

    private static Row row(ItemStack stack, ItemStack worn, EquipmentSlot slot, Attribute attribute,
                           Multimap<Attribute, AttributeModifier> modifiers, Player player, List<GearCompare.Diff> diffs) {
        String id = String.valueOf(ForgeRegistries.ATTRIBUTES.getKey(attribute));
        double base = slot == EquipmentSlot.MAINHAND && player != null ? player.getAttributeBaseValue(attribute) : 0.0;
        double value = AttributeMath.total(base, mods(modifiers.get(attribute)));
        double enchant = id.equals("minecraft:generic.attack_damage") ? EnchantmentHelper.getDamageBonus(stack, MobType.UNDEFINED) : 0.0;
        double reference = base;
        if (!worn.isEmpty() && !ItemStack.isSameItemSameTags(worn, stack)) {
            reference = AttributeMath.total(base, mods(worn.getAttributeModifiers(slot).get(attribute)));
        } else if (!worn.isEmpty()) {
            reference = value;
        }
        // What the worn item gets from its own enchantments counts too (a Sharpness sword against a plain one).
        double wornEnchant = !worn.isEmpty() && !ItemStack.isSameItemSameTags(worn, stack) && id.equals("minecraft:generic.attack_damage")
                ? EnchantmentHelper.getDamageBonus(worn, MobType.UNDEFINED) : 0.0;
        double difference = value + enchant - reference - wornEnchant;
        if (!worn.isEmpty() && !ItemStack.isSameItemSameTags(worn, stack)) {
            diffs.add(new GearCompare.Diff(difference, scaleOf(id) > 0 ? scaleOf(id) : Math.max(1.0, Math.abs(value) + Math.abs(reference))));
        }
        int deltaColor = Math.abs(difference) < 0.005 ? STEADY : difference > 0 ? UP : DOWN;
        int[] colors = colorsOf(id);
        return new Row(Component.translatable(attribute.getDescriptionId()).getString(), value, enchant,
                AttributeMath.delta(difference), deltaColor, AttributeMath.fraction(value, scaleOf(id)),
                AttributeMath.fraction(enchant, scaleOf(id)), colors[0], colors[1]);
    }

    private static List<AttributeMath.Mod> mods(Iterable<AttributeModifier> modifiers) {
        List<AttributeMath.Mod> list = new ArrayList<>();
        for (AttributeModifier modifier : modifiers) {
            list.add(new AttributeMath.Mod(modifier.getOperation().toValue(), modifier.getAmount()));
        }
        return list;
    }

    private static double scaleOf(String id) {
        return switch (id) {
            case "minecraft:generic.attack_damage" -> 16.0;
            case "minecraft:generic.attack_speed" -> 4.0;
            case "minecraft:generic.armor" -> 20.0;
            case "minecraft:generic.armor_toughness" -> 8.0;
            case "minecraft:generic.knockback_resistance" -> 1.0;
            case "minecraft:generic.movement_speed" -> 0.2;
            case "minecraft:generic.max_health" -> 20.0;
            default -> 0.0;
        };
    }

    private static int[] colorsOf(String id) {
        return switch (id) {
            case "minecraft:generic.attack_damage", "minecraft:generic.max_health" -> new int[]{0xFFFF7468, 0xFFA82F29};
            case "minecraft:generic.attack_speed" -> new int[]{0xFFF5B05A, 0xFFA86A25};
            case "minecraft:generic.armor" -> new int[]{0xFFC6D3DC, 0xFF6F8290};
            case "minecraft:generic.armor_toughness" -> new int[]{0xFF9BE0FF, 0xFF3A8FBF};
            case "minecraft:generic.knockback_resistance" -> new int[]{0xFFC9A8FF, 0xFF7A4FD0};
            case "minecraft:generic.movement_speed" -> new int[]{0xFF7DFFB0, 0xFF2BB56A};
            default -> new int[]{FordixPalette.ACCENT, Chevron.mix(FordixPalette.ACCENT, 0xFF000000, 0.4F)};
        };
    }

    private static int rarityColor(ItemStack stack) {
        return ItemRarity.isNotable(stack) ? ItemRarity.color(stack) : FordixPalette.TOOLTIP_BORDER_TOP | 0xFF000000;
    }

    public static String typeOf(ItemStack stack) {
        var item = stack.getItem();
        if (item instanceof ArmorItem) {
            return Tr.s("tooltip.type.armor");
        }
        if (item instanceof SwordItem || item instanceof TridentItem
                || !stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).isEmpty()) {
            return Tr.s(item instanceof DiggerItem ? "tooltip.type.tool" : "tooltip.type.weapon");
        }
        if (item instanceof DiggerItem) {
            return Tr.s("tooltip.type.tool");
        }
        return Tr.s(item instanceof BlockItem ? "tooltip.type.block" : "tooltip.type.item");
    }

    private static String modName(ItemStack stack) {
        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return ModNames.of(key == null ? "minecraft" : key.getNamespace());
    }
}
