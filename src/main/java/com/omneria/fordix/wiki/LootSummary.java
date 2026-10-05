package com.omneria.fordix.wiki;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads a loot table (in the JSON the game itself writes for it) as a list of drops: what, how many, how likely and under
 * which conditions. Pure Java (Gson only), unit tested; the server hands it the table's JSON and sends the lines to clients.
 */
public final class LootSummary {
    public static final String PLAYER_KILL = "P";
    public static final String LOOTING = "L";
    public static final String TAG = "T";
    public static final String TABLE = "S";
    public static final String SMELTED = "F";
    public static final String OTHER_CONDITION = "C";

    /** One possible drop. {@code chance} is a percentage (0-100), {@code flags} a string of the letters above. */
    public record Drop(String item, int min, int max, double chance, String flags) {
    }

    private static final int MAX_DROPS = 60;

    private LootSummary() {
    }

    public static List<Drop> parse(JsonElement table) {
        List<Drop> drops = new ArrayList<>();
        if (table == null || !table.isJsonObject() || !table.getAsJsonObject().has("pools")) {
            return drops;
        }
        for (JsonElement pool : table.getAsJsonObject().getAsJsonArray("pools")) {
            if (pool.isJsonObject()) {
                readPool(pool.getAsJsonObject(), drops);
            }
        }
        return drops;
    }

    private static void readPool(JsonObject pool, List<Drop> drops) {
        int[] rolls = range(pool.get("rolls"), 1, 1);
        Context pooled = Context.NONE.with(pool.get("conditions"));
        JsonArray entries = pool.has("entries") ? pool.getAsJsonArray("entries") : new JsonArray();
        double total = 0;
        for (JsonElement entry : entries) {
            total += weight(entry);
        }
        for (JsonElement entry : entries) {
            if (entry.isJsonObject() && drops.size() < MAX_DROPS) {
                double share = total <= 0 ? 1 : weight(entry) / total;
                readEntry(entry.getAsJsonObject(), pooled, share, rolls, drops);
            }
        }
    }

    private static double weight(JsonElement entry) {
        if (!entry.isJsonObject() || !entry.getAsJsonObject().has("weight")) {
            return 1;
        }
        return Math.max(0, entry.getAsJsonObject().get("weight").getAsDouble());
    }

    private static void readEntry(JsonObject entry, Context inherited, double share, int[] rolls, List<Drop> drops) {
        Context context = inherited.with(entry.get("conditions"));
        String type = entry.has("type") ? strip(entry.get("type").getAsString()) : "";
        switch (type) {
            case "alternatives", "group", "sequence" -> {
                if (entry.has("children")) {
                    for (JsonElement child : entry.getAsJsonArray("children")) {
                        if (child.isJsonObject() && drops.size() < MAX_DROPS) {
                            readEntry(child.getAsJsonObject(), context, share, rolls, drops);
                        }
                    }
                }
            }
            case "item", "tag", "loot_table" -> {
                if (!entry.has("name")) {
                    return;
                }
                int[] count = {1, 1};
                String flags = context.flags;
                for (JsonElement function : array(entry.get("functions"))) {
                    if (!function.isJsonObject()) {
                        continue;
                    }
                    String name = function.getAsJsonObject().has("function") ? strip(function.getAsJsonObject().get("function").getAsString()) : "";
                    if (name.equals("set_count") && !function.getAsJsonObject().has("add")) {
                        count = range(function.getAsJsonObject().get("count"), 1, 1);
                    } else if (name.equals("set_count") && !function.getAsJsonObject().get("add").getAsBoolean()) {
                        count = range(function.getAsJsonObject().get("count"), 1, 1);
                    } else if (name.equals("looting_enchant") && !flags.contains(LOOTING)) {
                        flags += LOOTING;
                    } else if (name.equals("furnace_smelt") && !flags.contains(SMELTED)) {
                        flags += SMELTED;
                    }
                }
                if (type.equals("tag") && !flags.contains(TAG)) {
                    flags += TAG;
                } else if (type.equals("loot_table") && !flags.contains(TABLE)) {
                    flags += TABLE;
                }
                double chance = Math.min(100, Math.round(share * context.chance * 1000.0) / 10.0);
                drops.add(new Drop(entry.get("name").getAsString(), count[0] * rolls[0], count[1] * rolls[1], chance, flags));
            }
            default -> {
            }
        }
    }

    /** What the conditions on the way down to an entry add up to: a chance factor and flags. */
    private record Context(double chance, String flags) {
        static final Context NONE = new Context(1, "");

        Context with(JsonElement conditions) {
            double factor = chance;
            String letters = flags;
            for (JsonElement condition : array(conditions)) {
                if (!condition.isJsonObject()) {
                    continue;
                }
                JsonObject object = condition.getAsJsonObject();
                String name = object.has("condition") ? strip(object.get("condition").getAsString()) : "";
                switch (name) {
                    case "random_chance" -> factor *= number(object, "chance", 1);
                    case "random_chance_with_looting" -> {
                        factor *= number(object, "chance", 1);
                        letters = add(letters, LOOTING);
                    }
                    case "killed_by_player" -> letters = add(letters, PLAYER_KILL);
                    case "inverted", "damage_source_properties", "entity_properties", "match_tool", "location_check", "weather_check" ->
                            letters = add(letters, OTHER_CONDITION);
                    default -> {
                        if (!name.isEmpty()) {
                            letters = add(letters, OTHER_CONDITION);
                        }
                    }
                }
            }
            return new Context(factor, letters);
        }
    }

    private static String add(String letters, String flag) {
        return letters.contains(flag) ? letters : letters + flag;
    }

    private static double number(JsonObject object, String key, double fallback) {
        try {
            return object.has(key) ? object.get(key).getAsDouble() : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static Iterable<JsonElement> array(JsonElement element) {
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
    }

    /** A number provider as {min, max}: a plain number, {min,max}, or a binomial (0 to n). */
    static int[] range(JsonElement provider, int fallbackMin, int fallbackMax) {
        try {
            if (provider == null || provider.isJsonNull()) {
                return new int[]{fallbackMin, fallbackMax};
            }
            if (provider.isJsonPrimitive()) {
                int value = (int) Math.round(provider.getAsDouble());
                return new int[]{value, value};
            }
            JsonObject object = provider.getAsJsonObject();
            if (object.has("min") && object.has("max")) {
                return new int[]{(int) Math.floor(object.get("min").getAsDouble()), (int) Math.ceil(object.get("max").getAsDouble())};
            }
            if (object.has("n")) {
                return new int[]{0, (int) Math.round(object.get("n").getAsDouble())};
            }
            if (object.has("value")) {
                return range(object.get("value"), fallbackMin, fallbackMax);
            }
        } catch (RuntimeException e) {
            // an odd provider: show the default count
        }
        return new int[]{fallbackMin, fallbackMax};
    }

    private static String strip(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(colon + 1);
    }

    /** The drops as the lines sent to the client: item, min, max, chance, flags, tab separated. */
    public static String toLines(List<Drop> drops) {
        StringBuilder out = new StringBuilder();
        for (Drop drop : drops) {
            out.append(drop.item()).append('\t').append(drop.min()).append('\t').append(drop.max()).append('\t')
                    .append(drop.chance()).append('\t').append(drop.flags()).append('\n');
        }
        return out.toString();
    }
}
