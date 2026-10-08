package com.omneria.fordix.client.wiki;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * What a player remembers of the wikis: favourite pages and the pages read last, kept in a small file of the client's config
 * folder. Pure Java (Gson), unit tested.
 */
public final class WikiMemory {
    public static final int MAX_RECENT = 12;
    public static final int MAX_FAVORITES = 60;

    /** A remembered page. */
    public record Ref(String wiki, String slug, String title) {
    }

    private static final class Data {
        List<Ref> favorites = new ArrayList<>();
        List<Ref> recent = new ArrayList<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private Data data = new Data();

    public WikiMemory(Path file) {
        this.file = file;
        if (file != null && Files.isRegularFile(file)) {
            try {
                Data read = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Data.class);
                if (read != null) {
                    data = read;
                    if (data.favorites == null) {
                        data.favorites = new ArrayList<>();
                    }
                    if (data.recent == null) {
                        data.recent = new ArrayList<>();
                    }
                }
            } catch (IOException | RuntimeException ignored) {
                // an unreadable file means nothing remembered
            }
        }
    }

    public List<Ref> favorites() {
        return List.copyOf(data.favorites);
    }

    public List<Ref> recent() {
        return List.copyOf(data.recent);
    }

    public boolean isFavorite(String wiki, String slug) {
        return data.favorites.stream().anyMatch(ref -> ref.wiki().equals(wiki) && ref.slug().equals(slug));
    }

    /** Adds the page to the favourites, or removes it when it already is one. Returns whether it is a favourite now. */
    public boolean toggleFavorite(String wiki, String slug, String title) {
        boolean removed = data.favorites.removeIf(ref -> ref.wiki().equals(wiki) && ref.slug().equals(slug));
        if (!removed && data.favorites.size() < MAX_FAVORITES) {
            data.favorites.add(0, new Ref(wiki, slug, title));
        }
        save();
        return !removed && isFavorite(wiki, slug);
    }

    /** Puts the page first among the pages read last. */
    public void visited(String wiki, String slug, String title) {
        data.recent.removeIf(ref -> ref.wiki().equals(wiki) && ref.slug().equals(slug));
        data.recent.add(0, new Ref(wiki, slug, title));
        while (data.recent.size() > MAX_RECENT) {
            data.recent.remove(data.recent.size() - 1);
        }
        save();
    }

    private void save() {
        if (file == null) {
            return;
        }
        try {
            Files.writeString(file, GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // not saved: it is only a convenience
        }
    }
}
