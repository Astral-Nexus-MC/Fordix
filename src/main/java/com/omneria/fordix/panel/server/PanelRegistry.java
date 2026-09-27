package com.omneria.fordix.panel.server;

import com.omneria.fordix.panel.PanelSpec;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Drafts are what /fordix panel button etc. edit; they're saved with the world
 * (see {@link PanelSavedData}) so authored panels survive a restart. Sent
 * snapshots are what a button click is resolved against, so editing a draft
 * after sending it doesn't change what an already-open panel's buttons do.
 *
 * Sent snapshots are tracked per player and kept in memory only: a click is only
 * honored for a panel that was actually sent to the clicking player, so a
 * modified client can't trigger the buttons of a panel meant for someone else.
 * A player's entries are dropped when they log out.
 */
public final class PanelRegistry {
    private static final Map<UUID, Map<String, PanelSpec>> SENT = new HashMap<>();

    private PanelRegistry() {
    }

    /** The draft for {@code id}, created if missing. Marks the saved data dirty, as callers edit it. */
    public static PanelDraft draft(MinecraftServer server, String id) {
        PanelSavedData data = server.overworld().getDataStorage()
                .computeIfAbsent(PanelSavedData::load, PanelSavedData::new, PanelSavedData.FILE_ID);
        data.setDirty();
        return data.draft(id);
    }

    public static void markSent(UUID player, PanelSpec spec) {
        SENT.computeIfAbsent(player, uuid -> new HashMap<>()).put(spec.id(), spec);
    }

    /** The snapshot of panel {@code id} last sent to {@code player}, or null if it never was. */
    public static PanelSpec getSent(UUID player, String id) {
        Map<String, PanelSpec> panels = SENT.get(player);
        return panels == null ? null : panels.get(id);
    }

    public static void forgetPlayer(UUID player) {
        SENT.remove(player);
    }
}
