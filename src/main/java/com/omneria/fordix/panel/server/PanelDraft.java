package com.omneria.fordix.panel.server;

import com.omneria.fordix.panel.PanelSpec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/** Mutable, in-progress panel an admin is building with /fordix panel before sending it. */
public final class PanelDraft {
    private static final String TAG_TITLE = "title";
    private static final String TAG_DESCRIPTION = "description";
    private static final String TAG_BUTTONS = "buttons";
    private static final String TAG_LABEL = "label";
    private static final String TAG_COMMAND = "command";

    private final String id;
    private final List<PanelSpec.PanelButton> buttons = new ArrayList<>();
    private String title;
    private String description = "";

    public PanelDraft(String id) {
        this.id = id;
        this.title = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** Replaces every button at once, as the admin screen's editor saves them. */
    public void replaceButtons(List<PanelSpec.PanelButton> newButtons) {
        buttons.clear();
        buttons.addAll(newButtons);
    }

    public void addButton(String label, String command) {
        buttons.add(new PanelSpec.PanelButton(label, command));
    }

    public PanelSpec toSpec() {
        return new PanelSpec(id, title, description, List.copyOf(buttons));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_TITLE, title);
        tag.putString(TAG_DESCRIPTION, description);
        ListTag buttonTags = new ListTag();
        for (PanelSpec.PanelButton button : buttons) {
            CompoundTag buttonTag = new CompoundTag();
            buttonTag.putString(TAG_LABEL, button.label());
            buttonTag.putString(TAG_COMMAND, button.command());
            buttonTags.add(buttonTag);
        }
        tag.put(TAG_BUTTONS, buttonTags);
        return tag;
    }

    public static PanelDraft load(String id, CompoundTag tag) {
        PanelDraft draft = new PanelDraft(id);
        draft.setTitle(tag.getString(TAG_TITLE));
        draft.setDescription(tag.getString(TAG_DESCRIPTION));
        ListTag buttonTags = tag.getList(TAG_BUTTONS, Tag.TAG_COMPOUND);
        for (int i = 0; i < buttonTags.size(); i++) {
            CompoundTag buttonTag = buttonTags.getCompound(i);
            draft.addButton(buttonTag.getString(TAG_LABEL), buttonTag.getString(TAG_COMMAND));
        }
        return draft;
    }
}
