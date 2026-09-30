package com.omneria.fordix.client.gui;

import com.omneria.fordix.admin.ServerboundAdminPayload;
import com.omneria.fordix.client.admin.ClientAdminData;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.Panel;
import com.omneria.fordix.panel.network.PanelNetwork;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Administration for ops, in game: send a notification (style, duration, title, message, and who:
 * everyone, a player, a team, a tag or a permission level) and read the history of what was sent;
 * list, create, edit, send and delete panels. The screen only asks the server, which checks the
 * player is an op and does the work - see {@code AdminHandler}.
 */
public class FordixAdminScreen extends FordixScreen {
    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 250;
    private static final int PADDING = 10;
    private static final List<String> STYLES = List.of("info", "success", "warning", "error");
    private static final int[] SECONDS = {3, 5, 8, 15, 30};
    private static final List<String> KINDS = List.of("tous", "joueur", "equipe", "tag", "permission");
    private static final int LIST_ROWS = 7;

    private final Screen parent;
    private Panel panel;
    private int tab;
    private int seenVersion = -1;
    private boolean awaitingPanel;

    // Notification form.
    private int styleIndex;
    private int secondsIndex = 1;
    private int kindIndex;
    private String targetValue = "";
    private String notifTitle = "";
    private String notifMessage = "";

    // Panel editor.
    private List<String> panelIds = List.of();
    private int listOffset;
    private String selectedId = "";
    private String newId = "";
    private String editTitle = "";
    private String editDescription = "";
    private String editButtons = "";

    public FordixAdminScreen(Screen parent) {
        super(Component.literal("FORDIX // ADMINISTRATION"));
        this.parent = parent;
        ClientAdminData.reset();
        send("panels");
        send("history");
    }

    private static void send(String action, String... args) {
        PanelNetwork.CHANNEL.sendToServer(new ServerboundAdminPayload(action, List.of(args)));
    }

    @Override
    protected void init() {
        int left = centerX(PANEL_WIDTH);
        int top = centerY(PANEL_HEIGHT);
        this.panel = new Panel(left, top, PANEL_WIDTH, PANEL_HEIGHT);
        int x = left + PADDING;
        int inner = PANEL_WIDTH - PADDING * 2;

        addRenderableWidget(new FordixButton(x, top + 22, 100, 18, Component.literal("Notifications"), () -> switchTab(0)));
        addRenderableWidget(new FordixButton(x + 104, top + 22, 100, 18, Component.literal("Panels"), () -> switchTab(1)));
        addRenderableWidget(new FordixButton(left + PANEL_WIDTH - PADDING - 70, top + 22, 70, 18, Component.literal("Fermer"), this::onClose));
        if (ClientAdminData.denied()) {
            return;
        }
        if (tab == 0) {
            initNotifications(x, top + 48, inner);
        } else {
            initPanels(x, top + 48, inner);
        }
    }

    private void switchTab(int next) {
        tab = next;
        rebuildWidgets();
    }

    private void initNotifications(int x, int y, int inner) {
        FordixButton style = cycle(x, y, 90, "", STYLES, () -> styleIndex, value -> styleIndex = value);
        addRenderableWidget(style);
        addRenderableWidget(new FordixButton(x + 96, y, 60, 18, Component.literal(SECONDS[secondsIndex] + " s"), () -> {
            secondsIndex = (secondsIndex + 1) % SECONDS.length;
            rebuildWidgets();
        }));
        addRenderableWidget(cycle(x + 162, y, 100, "", KINDS, () -> kindIndex, value -> {
            kindIndex = value;
            rebuildWidgets();
        }));
        if (kindIndex != 0) {
            String hint = kindIndex == 4 ? "niveau 0-4" : KINDS.get(kindIndex);
            addRenderableWidget(edit(x + 268, y, inner - 268, hint, targetValue, 32, text -> targetValue = text));
        }
        addRenderableWidget(edit(x, y + 24, inner, "Titre", notifTitle, 64, text -> notifTitle = text));
        addRenderableWidget(edit(x, y + 48, inner, "Message (optionnel)", notifMessage, 128, text -> notifMessage = text));
        addRenderableWidget(new FordixButton(x, y + 72, 130, 18, Component.literal("Envoyer"), this::sendNotification));
    }

    private void sendNotification() {
        send("notify", STYLES.get(styleIndex), String.valueOf(SECONDS[secondsIndex]), notifTitle, notifMessage,
                KINDS.get(kindIndex), targetValue);
    }

    private void initPanels(int x, int y, int inner) {
        addRenderableWidget(edit(x, y, 80, "nouveau-id", newId, 32, text -> newId = text.toLowerCase()));
        addRenderableWidget(new FordixButton(x + 84, y, 36, 18, Component.literal("Creer"), () -> {
            if (!newId.isBlank()) {
                selectedId = newId;
                editTitle = newId;
                editDescription = "";
                editButtons = "";
                awaitingPanel = false;
                send("save_panel", newId, newId, "");
                newId = "";
                rebuildWidgets();
            }
        }));
        int listY = y + 24;
        for (int i = 0; i < LIST_ROWS; i++) {
            int index = listOffset + i;
            if (index >= panelIds.size()) {
                break;
            }
            String id = panelIds.get(index);
            FordixButton row = new FordixButton(x, listY + i * 20, 120, 18, Component.literal(id.equals(selectedId) ? "> " + id : id), () -> select(id));
            addRenderableWidget(row);
        }
        addRenderableWidget(new FordixButton(x, listY + LIST_ROWS * 20, 58, 16, Component.literal("▲"), () -> {
            listOffset = Math.max(0, listOffset - LIST_ROWS);
            rebuildWidgets();
        }));
        addRenderableWidget(new FordixButton(x + 62, listY + LIST_ROWS * 20, 58, 16, Component.literal("▼"), () -> {
            if (listOffset + LIST_ROWS < panelIds.size()) {
                listOffset += LIST_ROWS;
                rebuildWidgets();
            }
        }));

        if (selectedId.isEmpty()) {
            return;
        }
        int ex = x + 130;
        int ew = inner - 130;
        addRenderableWidget(edit(ex, y, ew, "Titre", editTitle, 64, text -> editTitle = text));
        addRenderableWidget(edit(ex, y + 24, ew, "Description", editDescription, 200, text -> editDescription = text));
        MultiLineEditBox buttons = new MultiLineEditBox(this.font, ex, y + 62, ew, 84,
                Component.literal("Un bouton par ligne : Libelle | commande"), Component.literal("Boutons"));
        buttons.setValue(editButtons);
        buttons.setValueListener(text -> editButtons = text);
        addRenderableWidget(buttons);
        int by = y + 152;
        addRenderableWidget(new FordixButton(ex, by, 84, 18, Component.literal("Enregistrer"), this::savePanel));
        addRenderableWidget(new FordixButton(ex + 88, by, 84, 18, Component.literal("M'envoyer"), () -> send("send_panel", selectedId)));
        addRenderableWidget(new FordixButton(ex + 176, by, 84, 18, Component.literal("Supprimer"), () -> {
            send("delete_panel", selectedId);
            selectedId = "";
            rebuildWidgets();
        }));
    }

    private void select(String id) {
        selectedId = id;
        awaitingPanel = true;
        send("panel", id);
        rebuildWidgets();
    }

    private void savePanel() {
        List<String> args = new ArrayList<>(List.of(selectedId, editTitle, editDescription));
        for (String line : editButtons.split("\n")) {
            int bar = line.indexOf('|');
            if (bar > 0) {
                args.add(line.substring(0, bar).trim() + "\t" + line.substring(bar + 1).trim());
            }
        }
        PanelNetwork.CHANNEL.sendToServer(new ServerboundAdminPayload("save_panel", args));
    }

    private FordixButton cycle(int x, int y, int width, String prefix, List<String> values, java.util.function.IntSupplier current,
                               java.util.function.IntConsumer setter) {
        return new FordixButton(x, y, width, 18, Component.literal(prefix + values.get(current.getAsInt())),
                () -> setter.accept((current.getAsInt() + 1) % values.size()));
    }

    private EditBox edit(int x, int y, int width, String hint, String value, int maxLength, java.util.function.Consumer<String> responder) {
        EditBox box = new EditBox(this.font, x, y, width, 18, Component.literal(hint));
        box.setMaxLength(maxLength);
        box.setHint(Component.literal(hint));
        box.setValue(value);
        box.setResponder(responder);
        return box;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (ClientAdminData.version() != seenVersion) {
            seenVersion = ClientAdminData.version();
            refresh();
        }
        panel.render(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int x = panel.getX() + PADDING;
        Font font = this.font;
        guiGraphics.drawString(font, this.title, x, panel.getY() + 8, FordixPalette.ACCENT, false);
        int bottom = panel.getY() + PANEL_HEIGHT - 14;
        String status = ClientAdminData.status();
        if (!status.isEmpty()) {
            guiGraphics.drawString(font, font.plainSubstrByWidth(status, PANEL_WIDTH - PADDING * 2), x, bottom,
                    ClientAdminData.denied() ? FordixPalette.ERROR : FordixPalette.TEXT_BRIGHT, false);
        }
        if (tab == 0 && !ClientAdminData.denied()) {
            int y = panel.getY() + 48 + 100;
            guiGraphics.drawString(font, "Historique", x, y, FordixPalette.TEXT, false);
            y += 12;
            for (String line : ClientAdminData.history()) {
                guiGraphics.drawString(font, font.plainSubstrByWidth(line, PANEL_WIDTH - PADDING * 2), x, y, FordixPalette.TEXT, false);
                y += 10;
                if (y > bottom - 12) {
                    break;
                }
            }
        }
    }

    /** New data from the server: update the panel list, fill the editor when the panel asked for arrives. */
    private void refresh() {
        panelIds = ClientAdminData.panels();
        List<String> data = ClientAdminData.panel();
        if (awaitingPanel && data.size() >= 3 && data.get(0).equals(selectedId)) {
            awaitingPanel = false;
            editTitle = data.get(1);
            editDescription = data.get(2);
            StringBuilder lines = new StringBuilder();
            for (int i = 3; i < data.size(); i++) {
                String[] parts = data.get(i).split("\t", 2);
                lines.append(parts[0]).append(" | ").append(parts.length > 1 ? parts[1] : "").append('\n');
            }
            editButtons = lines.toString().stripTrailing();
        }
        rebuildWidgets();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
