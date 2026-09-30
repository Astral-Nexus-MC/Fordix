package com.omneria.fordix.client.guide;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.omneria.fordix.Fordix;
import com.omneria.fordix.guide.GuidePage;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Which version of each server guide page the player has read: a page edited since is "new".
 * Kept in config/fordix-seen-pages.json, per page id (shared between servers, which is fine:
 * the worst case is a badge that shows once more).
 */
public final class SeenPages {
    private static final Gson GSON = new Gson();
    private static Map<String, Long> seen;

    private SeenPages() {
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("fordix-seen-pages.json");
    }

    private static Map<String, Long> load() {
        if (seen == null) {
            seen = new HashMap<>();
            try {
                if (Files.isRegularFile(file())) {
                    Map<String, Long> read = GSON.fromJson(Files.readString(file(), StandardCharsets.UTF_8),
                            new TypeToken<Map<String, Long>>() { }.getType());
                    if (read != null) {
                        seen.putAll(read);
                    }
                }
            } catch (IOException | RuntimeException e) {
                Fordix.LOGGER.warn("Could not read {}", file(), e);
            }
        }
        return seen;
    }

    public static boolean isNew(GuidePage page) {
        return page.updatedAt > load().getOrDefault(page.id, 0L);
    }

    public static void markSeen(GuidePage page) {
        Map<String, Long> map = load();
        if (map.getOrDefault(page.id, 0L) >= page.updatedAt) {
            return;
        }
        map.put(page.id, page.updatedAt);
        try {
            Files.writeString(file(), GSON.toJson(map), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Fordix.LOGGER.warn("Could not save {}", file(), e);
        }
    }
}
