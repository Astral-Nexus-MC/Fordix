package com.omneria.fordix.client.tooltip;

import com.omneria.fordix.Tr;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.omneria.fordix.Fordix;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/**
 * {@code /fordixdump}: writes every item (with the attributes it gives per slot, and its rarity)
 * and every enchantment (rarity, max level, curse/treasure, and whether the tooltip knows its
 * numbers) of the installed mods to {@code fordix-analysis.json} in the game folder, with a
 * per-mod summary. It's how the tooltip rules get checked against a real modpack.
 */
public final class AnalysisCommand {
    private AnalysisCommand() {
    }

    public static void onRegister(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("fordixdump").executes(context -> {
            try {
                Path file = dump();
                context.getSource().sendSuccess(() -> Tr.c("cmd.dump.done", file.toString()), false);
                return 1;
            } catch (IOException | RuntimeException e) {
                Fordix.LOGGER.warn("Fordix analysis failed", e);
                context.getSource().sendFailure(Tr.c("cmd.dump.failed", String.valueOf(e.getMessage())));
                return 0;
            }
        }));
    }

    private static Path dump() throws IOException {
        JsonObject root = new JsonObject();
        Map<String, JsonObject> mods = new TreeMap<>();

        JsonArray enchantments = new JsonArray();
        for (Enchantment enchantment : ForgeRegistries.ENCHANTMENTS) {
            ResourceLocation key = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
            if (key == null) {
                continue;
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("id", key.toString());
            entry.addProperty("rarity", enchantment.getRarity().name());
            entry.addProperty("maxLevel", enchantment.getMaxLevel());
            entry.addProperty("curse", enchantment.isCurse());
            entry.addProperty("treasure", enchantment.isTreasureOnly());
            boolean known = EnchantEffects.describe(key.toString(), 1) != null;
            entry.addProperty("numbersKnown", known);
            enchantments.add(entry);
            JsonObject mod = summary(mods, key.getNamespace());
            mod.addProperty("enchantments", mod.get("enchantments").getAsInt() + 1);
            if (!known) {
                mod.addProperty("enchantmentsWithoutNumbers", mod.get("enchantmentsWithoutNumbers").getAsInt() + 1);
            }
        }
        root.add("enchantments", enchantments);

        JsonArray items = new JsonArray();
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
            if (key == null) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            JsonObject entry = new JsonObject();
            entry.addProperty("id", key.toString());
            entry.addProperty("rarity", stack.getRarity().name());
            entry.addProperty("maxDamage", stack.getMaxDamage());
            JsonObject slots = new JsonObject();
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(slot);
                if (modifiers.isEmpty()) {
                    continue;
                }
                JsonObject attributes = new JsonObject();
                for (Map.Entry<Attribute, AttributeModifier> modifier : modifiers.entries()) {
                    ResourceLocation attribute = ForgeRegistries.ATTRIBUTES.getKey(modifier.getKey());
                    attributes.addProperty(String.valueOf(attribute) + " (" + modifier.getValue().getOperation().name() + ")",
                            modifier.getValue().getAmount());
                }
                slots.add(slot.getName(), attributes);
            }
            boolean hasAttributes = !slots.entrySet().isEmpty();
            if (hasAttributes) {
                entry.add("attributes", slots);
            }
            items.add(entry);
            JsonObject mod = summary(mods, key.getNamespace());
            mod.addProperty("items", mod.get("items").getAsInt() + 1);
            if (hasAttributes) {
                mod.addProperty("itemsWithAttributes", mod.get("itemsWithAttributes").getAsInt() + 1);
            }
        }
        root.add("items", items);

        JsonObject summary = new JsonObject();
        mods.forEach(summary::add);
        root.add("mods", summary);

        Path file = FMLPaths.GAMEDIR.get().resolve("fordix-analysis.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(root, writer);
        }
        return file;
    }

    private static JsonObject summary(Map<String, JsonObject> mods, String modId) {
        return mods.computeIfAbsent(modId, id -> {
            JsonObject mod = new JsonObject();
            mod.addProperty("items", 0);
            mod.addProperty("itemsWithAttributes", 0);
            mod.addProperty("enchantments", 0);
            mod.addProperty("enchantmentsWithoutNumbers", 0);
            return mod;
        });
    }
}
