package com.omneria.fordix.client.gui;

import net.minecraftforge.fml.ModList;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Display name of a mod from its id ("create" gives "Create"), or the id itself when it isn't loaded. */
public final class ModNames {
    /** The mods do not change while the game runs: a name is looked up once (the search and the tooltips ask on every frame). */
    private static final Map<String, String> NAMES = new ConcurrentHashMap<>();

    private ModNames() {
    }

    public static String of(String modId) {
        if (modId.equals("minecraft")) {
            return "Minecraft";
        }
        String known = NAMES.get(modId);
        if (known == null) {
            known = ModList.get().getModContainerById(modId)
                    .map(container -> container.getModInfo().getDisplayName())
                    .orElse(modId);
            NAMES.put(modId, known);
        }
        return known;
    }
}
