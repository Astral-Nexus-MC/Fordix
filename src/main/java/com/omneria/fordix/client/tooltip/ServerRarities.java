package com.omneria.fordix.client.tooltip;

import com.omneria.fordix.rarity.RarityRules;
import com.omneria.fordix.rarity.RarityTier;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The rarity rules in force: the connected server's, or the built-in defaults outside a Fordix
 * server. Answers are cached per item, since tooltips and slot frames ask every frame.
 */
public final class ServerRarities {
    private static volatile RarityRules rules = RarityRules.parse(RarityRules.DEFAULT_LINES);
    private static final Map<Item, Optional<RarityTier>> CACHE = new ConcurrentHashMap<>();

    private ServerRarities() {
    }

    public static void set(List<String> lines) {
        rules = RarityRules.parse(lines.isEmpty() ? RarityRules.DEFAULT_LINES : RarityRules.withBuiltins(lines));
        CACHE.clear();
    }

    public static void clear() {
        rules = RarityRules.parse(RarityRules.DEFAULT_LINES);
        CACHE.clear();
    }

    /** The tier the rules give this item, if any rule matches. */
    public static Optional<RarityTier> tierOf(Item item) {
        return CACHE.computeIfAbsent(item, key -> {
            var id = ForgeRegistries.ITEMS.getKey(key);
            return id == null ? Optional.empty() : Optional.ofNullable(rules.tierOf(id.toString()));
        });
    }
}
