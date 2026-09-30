package com.omneria.fordix.rarity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Rarity by item id, written as lines "pattern | TIER". A pattern is an item id where "*" stands
 * for any text and "{a,b}" for a choice, e.g. "twilightforest:knightmetal_*" or
 * "minecraft:iron_{sword,pickaxe}". The first line that matches an id wins. Shared by server config
 * (validation, defaults) and client (matching). Plain Java, unit tested.
 */
public final class RarityRules {
    /** What a server that doesn't say otherwise gets, and what the client uses outside such a server. */
    public static final List<String> DEFAULT_LINES = List.of(
            // Boss trophies and heads.
            "twilightforest:*_trophy | LEGENDARY",
            "minecraft:dragon_head | LEGENDARY",
            // Twilight Forest's giant tools and blocks.
            "twilightforest:giant_* | EPIC",
            // Twilight Forest's banner patterns have a custom vanilla rarity that maps to no tier.
            "twilightforest:*_banner_pattern | RARE",
            // Netherite and its modded peers.
            "minecraft:netherite_* | EPIC",
            "minecraft:ancient_debris | EPIC",
            "create:netherite_* | EPIC",
            "twilightforest:fiery_* | EPIC",
            // Gems and strong metals.
            "minecraft:*diamond* | RARE",
            "minecraft:*emerald* | RARE",
            "twilightforest:steeleaf_* | RARE",
            "twilightforest:knightmetal_* | RARE",
            "twilightforest:carminite* | RARE",
            // Iron, gold, and their kin.
            "minecraft:{iron_ingot,iron_nugget,iron_ore,deepslate_iron_ore,raw_iron,raw_iron_block,iron_block} | UNCOMMON",
            "minecraft:iron_{sword,pickaxe,axe,shovel,hoe,helmet,chestplate,leggings,boots,horse_armor} | UNCOMMON",
            "minecraft:{gold_ingot,gold_nugget,gold_ore,deepslate_gold_ore,nether_gold_ore,raw_gold,raw_gold_block,gold_block} | UNCOMMON",
            "minecraft:golden_{sword,pickaxe,axe,shovel,hoe,helmet,chestplate,leggings,boots,horse_armor} | UNCOMMON",
            "create:brass_{ingot,nugget,block,sheet} | UNCOMMON",
            "twilightforest:ironwood_* | UNCOMMON",
            "twilightforest:naga_scale | UNCOMMON",
            "mowziesmobs:wrought_* | UNCOMMON",
            // Basic materials, so enchantments can raise them like the rest.
            "minecraft:{wooden,stone,leather,chainmail}_* | COMMON"
    );

    public record Rule(List<Pattern> patterns, RarityTier tier) {
        boolean matches(String id) {
            for (Pattern pattern : patterns) {
                if (pattern.matcher(id).matches()) {
                    return true;
                }
            }
            return false;
        }
    }

    private final List<Rule> rules;

    private RarityRules(List<Rule> rules) {
        this.rules = rules;
    }

    /** Parses lines, skipping invalid ones. */
    public static RarityRules parse(List<? extends String> lines) {
        List<Rule> rules = new ArrayList<>();
        for (String line : lines) {
            Rule rule = parseLine(line);
            if (rule != null) {
                rules.add(rule);
            }
        }
        return new RarityRules(List.copyOf(rules));
    }

    /** One line, or null when it isn't "pattern | TIER" with a known tier and a namespaced pattern. */
    public static Rule parseLine(String line) {
        if (line == null) {
            return null;
        }
        String[] parts = line.split("\\|");
        if (parts.length != 2) {
            return null;
        }
        String pattern = parts[0].trim();
        RarityTier tier = RarityTier.fromName(parts[1]);
        if (tier == null || pattern.isEmpty() || !pattern.contains(":") || !balanced(pattern)) {
            return null;
        }
        List<Pattern> compiled = new ArrayList<>();
        for (String expanded : expandBraces(pattern)) {
            compiled.add(toRegex(expanded));
        }
        return new Rule(List.copyOf(compiled), tier);
    }

    /** The tier of the first rule matching {@code itemId} ("namespace:path"), or null. */
    public RarityTier tierOf(String itemId) {
        for (Rule rule : rules) {
            if (rule.matches(itemId)) {
                return rule.tier();
            }
        }
        return null;
    }

    private static boolean balanced(String pattern) {
        int depth = 0;
        for (char c : pattern.toCharArray()) {
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
            if (depth < 0 || depth > 1) {
                return false;
            }
        }
        return depth == 0;
    }

    /** "a_{b,c}_d" gives "a_b_d" and "a_c_d"; one brace group per pattern. */
    private static List<String> expandBraces(String pattern) {
        int open = pattern.indexOf('{');
        if (open < 0) {
            return List.of(pattern);
        }
        int close = pattern.indexOf('}', open);
        List<String> result = new ArrayList<>();
        for (String choice : pattern.substring(open + 1, close).split(",")) {
            result.add(pattern.substring(0, open) + choice.trim() + pattern.substring(close + 1));
        }
        return result;
    }

    private static Pattern toRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        for (String piece : glob.split("\\*", -1)) {
            if (regex.length() > 0) {
                regex.append(".*");
            }
            regex.append(Pattern.quote(piece));
        }
        return Pattern.compile(regex.toString());
    }
}
