package com.omneria.fordix.panel.server;

import com.omneria.fordix.panel.PanelSpec;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Server-side, in-memory only - panels admins draft with /fordix panel do not
 * survive a restart. Drafts are what /fordix panel button etc. edit; sent snapshots
 * are what a button click is resolved against, so editing a draft after sending it
 * doesn't change what an already-open panel's buttons do.
 */
public final class PanelRegistry {
    private static final Map<String, PanelDraft> DRAFTS = new LinkedHashMap<>();
    private static final Map<String, PanelSpec> SENT = new LinkedHashMap<>();

    private PanelRegistry() {
    }

    public static PanelDraft draft(String id) {
        return DRAFTS.computeIfAbsent(id, PanelDraft::new);
    }

    public static void markSent(PanelSpec spec) {
        SENT.put(spec.id(), spec);
    }

    public static PanelSpec getSent(String id) {
        return SENT.get(id);
    }
}
