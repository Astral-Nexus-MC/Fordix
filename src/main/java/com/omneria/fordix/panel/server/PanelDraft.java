package com.omneria.fordix.panel.server;

import com.omneria.fordix.panel.PanelSpec;

import java.util.ArrayList;
import java.util.List;

/** Mutable, in-progress panel an admin is building with /fordix panel before sending it. */
public final class PanelDraft {
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

    public void addButton(String label, String command) {
        buttons.add(new PanelSpec.PanelButton(label, command));
    }

    public PanelSpec toSpec() {
        return new PanelSpec(id, title, description, List.copyOf(buttons));
    }
}
