package com.omneria.fordix.client.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The player's own table of how visible each panel is in each situation, over the defaults of {@link HudContext}.
 * One list of five values per panel (exploration, combat, mining, boss, mount), each cycling through 100, 55, 15 and 0 %.
 * Kept in a small JSON file. Pure Java, unit tested.
 */
public final class HudContextMatrix {
    public static final float[] LEVELS = {1.0F, 0.55F, 0.15F, 0.0F};
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private Map<String, float[]> overrides = new LinkedHashMap<>();
    /** The saved profile slot loaded in each situation (by ordinal), -1 for none. */
    private int[] profiles = {-1, -1, -1, -1, -1};

    private static final class Data {
        Map<String, float[]> overrides = new LinkedHashMap<>();
        int[] profiles;
    }

    public HudContextMatrix(Path file) {
        this.file = file;
        if (file != null && Files.isRegularFile(file)) {
            try {
                Data read = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Data.class);
                if (read != null && read.overrides != null) {
                    read.overrides.values().removeIf(row -> row == null || row.length != HudContext.Kind.values().length);
                    overrides = read.overrides;
                    if (read.profiles != null && read.profiles.length == profiles.length) {
                        profiles = read.profiles;
                    }
                }
            } catch (IOException | RuntimeException ignored) {
                // an unreadable file means the defaults
            }
        }
    }

    /** The player's value, or the default of the situation when the player never changed it. */
    public float factor(HudContext.Kind kind, String element) {
        float[] row = overrides.get(element);
        return row != null ? row[kind.ordinal()] : HudContext.defaultFactor(kind, element);
    }

    /** The next lower level than the current one (100, 55, 15, 0), and back to 100 after 0. */
    public void cycle(HudContext.Kind kind, String element) {
        float[] row = overrides.computeIfAbsent(element, key -> {
            float[] copy = new float[HudContext.Kind.values().length];
            for (HudContext.Kind each : HudContext.Kind.values()) {
                copy[each.ordinal()] = HudContext.defaultFactor(each, key);
            }
            return copy;
        });
        float now = row[kind.ordinal()];
        float next = LEVELS[0];
        for (float level : LEVELS) {
            if (level < now - 0.02F) {
                next = level;
                break;
            }
        }
        row[kind.ordinal()] = next;
        save();
    }

    public void reset() {
        overrides.clear();
        profiles = new int[]{-1, -1, -1, -1, -1};
        save();
    }

    public int profile(HudContext.Kind kind) {
        return profiles[kind.ordinal()];
    }

    /** The next slot for a situation: none, then each of the {@code slots} profiles, then none again. */
    public void cycleProfile(HudContext.Kind kind, int slots) {
        int next = profiles[kind.ordinal()] + 1;
        profiles[kind.ordinal()] = next >= slots ? -1 : next;
        save();
    }

    private void save() {
        if (file == null) {
            return;
        }
        try {
            Data data = new Data();
            data.overrides = overrides;
            data.profiles = profiles;
            Files.writeString(file, GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // the table still works for this session
        }
    }
}
