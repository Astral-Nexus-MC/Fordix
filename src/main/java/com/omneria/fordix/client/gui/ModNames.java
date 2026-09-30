package com.omneria.fordix.client.gui;

import net.minecraftforge.fml.ModList;

/** Display name of a mod from its id ("create" gives "Create"), or the id itself when it isn't loaded. */
public final class ModNames {
    private ModNames() {
    }

    public static String of(String modId) {
        if (modId.equals("minecraft")) {
            return "Minecraft";
        }
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(modId);
    }
}
