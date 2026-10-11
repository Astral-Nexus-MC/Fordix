package com.omneria.fordix.client.guide;

import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.ModNames;
import com.omneria.fordix.client.tooltip.AttributeMath;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the game itself knows about an item, gathered from the recipes the client was sent and the
 * item's own properties: how to make it, what it is used in, and its main figures. Exact for the
 * installed modpack (modded and changed recipes included), and needs no network.
 */
public final class ItemFacts {
    private static final int MAX_RECIPES = 12;
    private static final int MAX_USES = 80;

    private ItemFacts() {
    }

    /** One recipe as a grid of cells; each cell lists the stacks that fit it (empty = nothing). */
    public record RecipeView(String station, int columns, int rows, List<List<ItemStack>> cells, ItemStack result, String note) {
    }

    /**
     * A line of figures; {@code color} is an opaque ARGB color or 0 for the default text. A line with a {@code fraction} (0..1, or
     * -1 for none) gets a bar; {@code best} and {@code held} are marks on it (the best item of the game, the item in hand, -1
     * for none), {@code delta} the written gap to the held item. {@code group} puts it under a heading: 0 none, 1 combat, 2 sturdiness.
     */
    public record Fact(String label, String value, int color, float fraction, int barColor, float best, float held,
                       String delta, int deltaColor, int group) {
        public Fact(String label, String value, int color) {
            this(label, value, color, -1.0F, 0, -1.0F, -1.0F, "", 0, 0);
        }
    }

    public record Sheet(ItemStack stack, String name, String id, String mod, List<RecipeView> makes,
                        List<ItemStack> uses, List<Fact> facts) {
    }

    public static Sheet of(ItemStack input) {
        ItemStack stack = input.copyWithCount(1);
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String id = key == null ? "?" : key.toString();
        List<RecipeView> makes = new ArrayList<>();
        Map<net.minecraft.world.item.Item, ItemStack> uses = new LinkedHashMap<>();
        var level = Minecraft.getInstance().level;
        if (level != null) {
            RegistryAccess access = level.registryAccess();
            for (Recipe<?> recipe : level.getRecipeManager().getRecipes()) {
                ItemStack result = recipe.getResultItem(access);
                if (result.is(stack.getItem()) && makes.size() < MAX_RECIPES) {
                    makes.add(view(recipe, result));
                }
                if (uses.size() < MAX_USES && !result.isEmpty() && !result.is(stack.getItem())
                        && recipe.getIngredients().stream().anyMatch(ingredient -> ingredient.test(stack))) {
                    uses.putIfAbsent(result.getItem(), result.copyWithCount(1));
                }
            }
        }
        return new Sheet(stack, stack.getHoverName().getString(), id, key == null ? "" : ModNames.of(key.getNamespace()),
                makes, new ArrayList<>(uses.values()), facts(stack));
    }

    private static RecipeView view(Recipe<?> recipe, ItemStack result) {
        List<Ingredient> ingredients = recipe.getIngredients();
        if (recipe instanceof ShapedRecipe shaped) {
            List<List<ItemStack>> cells = new ArrayList<>();
            for (int row = 0; row < shaped.getHeight(); row++) {
                for (int column = 0; column < shaped.getWidth(); column++) {
                    int index = row * shaped.getWidth() + column;
                    cells.add(index < ingredients.size() ? alternatives(ingredients.get(index)) : List.of());
                }
            }
            return new RecipeView(Tr.s("sheet.station.crafting"), shaped.getWidth(), shaped.getHeight(), cells, result.copy(), "");
        }
        if (recipe instanceof AbstractCookingRecipe cooking) {
            return new RecipeView(cookingStation(recipe.getType()), 1, 1,
                    List.of(alternatives(ingredients.isEmpty() ? Ingredient.EMPTY : ingredients.get(0))), result.copy(),
                    (cooking.getCookingTime() / 20) + " s");
        }
        if (recipe instanceof StonecutterRecipe) {
            return new RecipeView(Tr.s("sheet.station.stonecutter"), 1, 1,
                    List.of(alternatives(ingredients.isEmpty() ? Ingredient.EMPTY : ingredients.get(0))), result.copy(), "");
        }
        List<List<ItemStack>> cells = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            cells.add(alternatives(ingredient));
        }
        int columns = Math.max(1, Math.min(3, cells.size()));
        int rows = Math.max(1, (cells.size() + columns - 1) / columns);
        String type = ForgeRegistries.RECIPE_TYPES.getKey(recipe.getType()) == null ? "?"
                : ForgeRegistries.RECIPE_TYPES.getKey(recipe.getType()).getPath();
        String station = recipe.getType() == RecipeType.CRAFTING ? Tr.s("sheet.station.shapeless") : type.replace('_', ' ');
        return new RecipeView(station, columns, rows, cells, result.copy(), "");
    }

    private static String cookingStation(RecipeType<?> type) {
        if (type == RecipeType.BLASTING) {
            return Tr.s("sheet.station.blasting");
        }
        if (type == RecipeType.SMOKING) {
            return Tr.s("sheet.station.smoking");
        }
        if (type == RecipeType.CAMPFIRE_COOKING) {
            return Tr.s("sheet.station.campfire");
        }
        return Tr.s("sheet.station.furnace");
    }

    private static List<ItemStack> alternatives(Ingredient ingredient) {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack stack : ingredient.getItems()) {
            list.add(stack.copyWithCount(1));
            if (list.size() >= 12) {
                break;
            }
        }
        return list;
    }

    private static final int GOOD = 0xFF5ED68A;
    private static final int BAD = 0xFFE5534B;

    /** The best figures among every item of the game (mods included), found once, on first use. */
    private static final class Bests {
        private static Map<String, Double> byKey;
        private static double durability;
        private static double enchantability;

        static synchronized void load() {
            if (byKey != null) {
                return;
            }
            Map<String, Double> found = new LinkedHashMap<>();
            for (net.minecraft.world.item.Item item : ForgeRegistries.ITEMS) {
                ItemStack stack = new ItemStack(item);
                durability = Math.max(durability, stack.getMaxDamage());
                enchantability = Math.max(enchantability, stack.getEnchantmentValue());
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    for (Map.Entry<Attribute, AttributeModifier> entry : stack.getAttributeModifiers(slot).entries()) {
                        if (entry.getValue().getOperation() == AttributeModifier.Operation.ADDITION) {
                            found.merge(key(entry.getKey(), slot), amount(entry.getKey(), entry.getValue()), Math::max);
                        }
                    }
                }
            }
            byKey = found;
        }

        static double of(Attribute attribute, EquipmentSlot slot) {
            return byKey == null ? -1.0 : byKey.getOrDefault(key(attribute, slot), -1.0);
        }
    }

    private static String key(Attribute attribute, EquipmentSlot slot) {
        return attribute.getDescriptionId() + "/" + slot.name();
    }

    /** An attribute's figure as the player reads it: the base attack damage (1) and speed (4) are added to the item's own. */
    private static double amount(Attribute attribute, AttributeModifier modifier) {
        double amount = modifier.getAmount();
        ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        if (modifier.getOperation() == AttributeModifier.Operation.ADDITION && id != null) {
            if (id.getPath().equals("generic.attack_damage")) {
                amount += 1;
            } else if (id.getPath().equals("generic.attack_speed")) {
                amount += 4;
            }
        }
        return amount;
    }

    /** The figure of {@code attribute} on {@code stack} in {@code slot}, or -1 when it has none there. */
    private static double figure(ItemStack stack, EquipmentSlot slot, Attribute attribute) {
        if (stack.isEmpty()) {
            return -1.0;
        }
        for (Map.Entry<Attribute, AttributeModifier> entry : stack.getAttributeModifiers(slot).entries()) {
            if (entry.getKey() == attribute && entry.getValue().getOperation() == AttributeModifier.Operation.ADDITION) {
                return amount(attribute, entry.getValue());
            }
        }
        return -1.0;
    }

    private static Fact bar(String label, double value, double bestOfGame, double held, int barColor, int group) {
        double best = bestOfGame > 0.0 ? bestOfGame : -1.0;
        double full = StatScale.full(value, best, held);
        String delta = "";
        int deltaColor = 0;
        if (held >= 0.0 && Math.abs(value - held) >= 0.005) {
            delta = AttributeMath.delta(value - held);
            deltaColor = value > held ? GOOD : BAD;
        }
        return new Fact(label, AttributeMath.number(value), 0, StatScale.fraction(value, full), barColor,
                StatScale.fraction(best, full), StatScale.fraction(held, full), delta, deltaColor, group);
    }

    private static List<Fact> facts(ItemStack stack) {
        List<Fact> general = new ArrayList<>();
        List<Fact> combat = new ArrayList<>();
        List<Fact> sturdy = new ArrayList<>();
        var player = Minecraft.getInstance().player;
        ItemStack hand = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        boolean sameAsHand = hand.isEmpty() || hand.is(stack.getItem());
        try {
            Bests.load();
        } catch (RuntimeException e) {
            // A mod item that breaks on a default stack: the bars simply have no best mark.
        }
        general.add(new Fact(Tr.s("sheet.fact.stack"), String.valueOf(stack.getMaxStackSize()), 0));
        if (stack.getMaxDamage() > 0) {
            sturdy.add(bar(Tr.s("sheet.fact.durability"), stack.getMaxDamage(), Bests.durability,
                    sameAsHand || hand.getMaxDamage() <= 0 ? -1.0 : hand.getMaxDamage(), 0xFF5ED68A, 2));
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            for (Map.Entry<Attribute, AttributeModifier> entry : stack.getAttributeModifiers(slot).entries()) {
                String name = Component.translatable(entry.getKey().getDescriptionId()).getString() + " (" + slotName(slot) + ")";
                ResourceLocation attribute = ForgeRegistries.ATTRIBUTES.getKey(entry.getKey());
                if (entry.getValue().getOperation() != AttributeModifier.Operation.ADDITION || attribute == null) {
                    double value = entry.getValue().getAmount();
                    general.add(new Fact(name, entry.getValue().getOperation() == AttributeModifier.Operation.ADDITION
                            ? AttributeMath.number(value) : AttributeMath.number(value * 100) + " %", 0));
                    continue;
                }
                double value = amount(entry.getKey(), entry.getValue());
                ItemStack worn = slot == EquipmentSlot.MAINHAND ? hand : player == null ? ItemStack.EMPTY : player.getItemBySlot(slot);
                double held = worn.is(stack.getItem()) ? -1.0 : figure(worn, slot, entry.getKey());
                double best = Bests.of(entry.getKey(), slot);
                switch (attribute.getPath()) {
                    case "generic.attack_damage" -> combat.add(bar(name, value, best, held, 0xFFE5534B, 1));
                    case "generic.attack_speed" -> combat.add(bar(name, value, best, held, 0xFFE8A64A, 1));
                    case "generic.armor" -> sturdy.add(bar(name, value, best, held, 0xFFC6D3DC, 2));
                    case "generic.armor_toughness" -> sturdy.add(bar(name, value, best, held, 0xFF9BE0FF, 2));
                    case "generic.knockback_resistance" -> sturdy.add(bar(name, value, best, held, 0xFFC9A8FF, 2));
                    case "generic.max_health" -> sturdy.add(bar(name, value, best, held, 0xFFE5534B, 2));
                    default -> general.add(new Fact(name, AttributeMath.number(value), 0));
                }
            }
        }
        FoodProperties food = player == null ? null : stack.getFoodProperties(player);
        if (food != null) {
            general.add(new Fact(Tr.s("sheet.fact.food"), Tr.s("sheet.food_value", food.getNutrition(), AttributeMath.number(food.getSaturationModifier() * food.getNutrition() * 2)), 0));
        }
        int burn = ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
        if (burn > 0) {
            general.add(new Fact(Tr.s("sheet.fact.fuel"), Tr.s("sheet.fuel_value", burn / 20, AttributeMath.number(burn / 200.0)), 0));
        }
        if (stack.getEnchantmentValue() > 0) {
            sturdy.add(bar(Tr.s("sheet.fact.enchantability"), stack.getEnchantmentValue(), Bests.enchantability,
                    sameAsHand || hand.getEnchantmentValue() <= 0 ? -1.0 : hand.getEnchantmentValue(), 0xFFB45FE0, 2));
        }
        List<Fact> facts = new ArrayList<>(general);
        facts.addAll(combat);
        facts.addAll(sturdy);
        return facts;
    }

    private static String slotName(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> Tr.s("sheet.slot.mainhand");
            case OFFHAND -> Tr.s("sheet.slot.offhand");
            case HEAD -> Tr.s("sheet.slot.head");
            case CHEST -> Tr.s("sheet.slot.chest");
            case LEGS -> Tr.s("sheet.slot.legs");
            case FEET -> Tr.s("sheet.slot.feet");
        };
    }
}
