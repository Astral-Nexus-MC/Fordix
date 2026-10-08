package com.omneria.fordix.client.gui.layout;

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
 * Saved HUD layouts: a few numbered slots, each holding the placement of every element. The player fills a slot from the
 * layout screen and flips between filled slots with a key. Pure Java (Gson), unit tested.
 */
public final class HudProfiles {
    public static final int SLOTS = 6;

    /** One element's saved placement. */
    public static final class Placement {
        public String anchor = "default";
        public int offsetX;
        public int offsetY;
        public boolean hidden;
        public float scale = 1.0F;
        /** 0 in an older file: same as the width factor. */
        public float scaleY;

        public Placement() {
        }

        public Placement(String anchor, int offsetX, int offsetY, boolean hidden, float scale) {
            this.anchor = anchor;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.hidden = hidden;
            this.scale = scale;
        }
    }

    private static final class Data {
        int active = -1;
        Map<String, Map<String, Placement>> slots = new LinkedHashMap<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private Data data = new Data();

    public HudProfiles(Path file) {
        this.file = file;
        if (Files.isRegularFile(file)) {
            try {
                Data read = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), new TypeToken<Data>() { }.getType());
                if (read != null && read.slots != null) {
                    data = read;
                }
            } catch (IOException | RuntimeException ignored) {
                // an unreadable file means no saved profile
            }
        }
    }

    public boolean has(int slot) {
        return data.slots.containsKey(Integer.toString(slot));
    }

    public Map<String, Placement> get(int slot) {
        return data.slots.get(Integer.toString(slot));
    }

    /** The slot last saved or loaded, -1 if none. */
    public int active() {
        return data.active;
    }

    public void setActive(int slot) {
        data.active = slot;
        save();
    }

    public void put(int slot, Map<String, Placement> layout) {
        data.slots.put(Integer.toString(slot), new LinkedHashMap<>(layout));
        data.active = slot;
        save();
    }

    /** The next filled slot after the active one, wrapping around; -1 when nothing is saved. */
    public int next() {
        for (int step = 1; step <= SLOTS; step++) {
            int slot = Math.floorMod(data.active + step, SLOTS);
            if (has(slot)) {
                return slot;
            }
        }
        return -1;
    }

    private void save() {
        try {
            Files.writeString(file, GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // the profile still works for this session
        }
    }
}
