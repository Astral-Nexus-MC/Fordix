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

/** The player's compass markers, one per world and dimension, kept in a small JSON file. Pure Java, unit tested. */
public final class MarkerStore {
    public record Marker(int x, int y, int z) {
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final Map<String, Marker> markers = new LinkedHashMap<>();

    public MarkerStore(Path file) {
        this.file = file;
        if (Files.isRegularFile(file)) {
            try {
                Map<String, Marker> read = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8),
                        new TypeToken<LinkedHashMap<String, Marker>>() { }.getType());
                if (read != null) {
                    markers.putAll(read);
                }
            } catch (IOException | RuntimeException ignored) {
                // an unreadable file just means no markers
            }
        }
    }

    public Marker get(String key) {
        return markers.get(key);
    }

    public void set(String key, Marker marker) {
        markers.put(key, marker);
        save();
    }

    public void remove(String key) {
        if (markers.remove(key) != null) {
            save();
        }
    }

    private void save() {
        try {
            Files.writeString(file, GSON.toJson(markers), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // the marker still works for this session
        }
    }
}
