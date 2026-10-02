package com.omneria.fordix.client.guide;

import com.omneria.fordix.client.gui.ModNames;
import com.omneria.fordix.client.tooltip.AttributeMath;
import com.omneria.fordix.client.tooltip.ItemRarity;
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

    /** A line of figures; {@code color} is an opaque ARGB color or 0 for the default text. */
    public record Fact(String label, String value, int color) {
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
            return new RecipeView("Table de craft", shaped.getWidth(), shaped.getHeight(), cells, result.copy(), "");
        }
        if (recipe instanceof AbstractCookingRecipe cooking) {
            return new RecipeView(cookingStation(recipe.getType()), 1, 1,
                    List.of(alternatives(ingredients.isEmpty() ? Ingredient.EMPTY : ingredients.get(0))), result.copy(),
                    (cooking.getCookingTime() / 20) + " s");
        }
        if (recipe instanceof StonecutterRecipe) {
            return new RecipeView("Tailleur de pierre", 1, 1,
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
        String station = recipe.getType() == RecipeType.CRAFTING ? "Sans forme" : type.replace('_', ' ');
        return new RecipeView(station, columns, rows, cells, result.copy(), "");
    }

    private static String cookingStation(RecipeType<?> type) {
        if (type == RecipeType.BLASTING) {
            return "Haut fourneau";
        }
        if (type == RecipeType.SMOKING) {
            return "Fumoir";
        }
        if (type == RecipeType.CAMPFIRE_COOKING) {
            return "Feu de camp";
        }
        return "Four";
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

    private static List<Fact> facts(ItemStack stack) {
        List<Fact> facts = new ArrayList<>();
        facts.add(new Fact("Rarete", ItemRarity.label(stack), ItemRarity.color(stack)));
        facts.add(new Fact("Pile maximale", String.valueOf(stack.getMaxStackSize()), 0));
        if (stack.getMaxDamage() > 0) {
            facts.add(new Fact("Durabilite", String.valueOf(stack.getMaxDamage()), 0));
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            for (Map.Entry<Attribute, AttributeModifier> entry : stack.getAttributeModifiers(slot).entries()) {
                double amount = entry.getValue().getAmount();
                String name = Component.translatable(entry.getKey().getDescriptionId()).getString();
                ResourceLocation attribute = ForgeRegistries.ATTRIBUTES.getKey(entry.getKey());
                if (entry.getValue().getOperation() == AttributeModifier.Operation.ADDITION && attribute != null) {
                    if (attribute.getPath().equals("generic.attack_damage")) {
                        amount += 1;
                    } else if (attribute.getPath().equals("generic.attack_speed")) {
                        amount += 4;
                    }
                }
                String value = entry.getValue().getOperation() == AttributeModifier.Operation.ADDITION
                        ? AttributeMath.number(amount) : AttributeMath.number(amount * 100) + " %";
                facts.add(new Fact(name + " (" + slotName(slot) + ")", value, 0));
            }
        }
        var player = Minecraft.getInstance().player;
        FoodProperties food = player == null ? null : stack.getFoodProperties(player);
        if (food != null) {
            facts.add(new Fact("Nourriture", food.getNutrition() + " (satiete " + AttributeMath.number(food.getSaturationModifier() * food.getNutrition() * 2) + ")", 0));
        }
        int burn = ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
        if (burn > 0) {
            facts.add(new Fact("Combustible", (burn / 20) + " s (" + AttributeMath.number(burn / 200.0) + " objets)", 0));
        }
        if (stack.getEnchantmentValue() > 0) {
            facts.add(new Fact("Enchantabilite", String.valueOf(stack.getEnchantmentValue()), 0));
        }
        return facts;
    }

    private static String slotName(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> "main";
            case OFFHAND -> "main gauche";
            case HEAD -> "tete";
            case CHEST -> "torse";
            case LEGS -> "jambes";
            case FEET -> "pieds";
        };
    }
}
