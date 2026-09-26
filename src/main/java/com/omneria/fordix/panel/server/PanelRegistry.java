package com.omneria.fordix.panel.server;

import com.omneria.fordix.panel.PanelSpec;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side, in-memory only - panels admins draft with /fordix panel do not
 * survive a restart. Drafts are what /fordix panel button etc. edit; sent snapshots
 * are what a button click is resolved against, so editing a draft after sending it
 * doesn't change what an already-open panel's buttons do.
 *
 * Sent snapshots are tracked per player: a click is only honored for a panel that
 * was actually sent to the clicking player, so a modified client can't trigger the
 * buttons of a panel meant for someone else. A player's entries are dropped when
 * they log out.
 */
public final class PanelRegistry {
    private static final Map<String, PanelDraft> DRAFTS = new LinkedHashMap<>();
    private static final Map<UUID, Map<String, PanelSpec>> SENT = new HashMap<>();

    private PanelRegistry() {
    }

    public static PanelDraft draft(String id) {
        return DRAFTS.computeIfAbsent(id, PanelDraft::new);
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
