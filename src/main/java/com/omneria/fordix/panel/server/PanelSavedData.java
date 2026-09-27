package com.omneria.fordix.panel.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The panels admins have authored with /fordix panel, saved with the world
 * (data/fordix_panels.dat) so they survive a server restart. Stored on the
 * overworld's data storage, the usual place for server-wide saved data.
 */
public final class PanelSavedData extends SavedData {
    public static final String FILE_ID = "fordix_panels";
    private static final String TAG_PANELS = "panels";

    private final Map<String, PanelDraft> drafts = new LinkedHashMap<>();

    public PanelDraft draft(String id) {
        return drafts.computeIfAbsent(id, PanelDraft::new);
    }

    public Set<String> ids() {
        return drafts.keySet();
    }

    /** Returns whether a panel with this id existed. */
    public boolean delete(String id) {
        return drafts.remove(id) != null;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag panels = new CompoundTag();
        drafts.forEach((id, draft) -> panels.put(id, draft.save()));
        tag.put(TAG_PANELS, panels);
        return tag;
    }

    public static PanelSavedData load(CompoundTag tag) {
        PanelSavedData data = new PanelSavedData();
        CompoundTag panels = tag.getCompound(TAG_PANELS);
        for (String id : panels.getAllKeys()) {
            data.drafts.put(id, PanelDraft.load(id, panels.getCompound(id)));
        }
        return data;
    }
}
