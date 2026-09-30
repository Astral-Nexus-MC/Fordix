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
 * Administration for ops, in game, in four tabs: send a notification (style, duration, title,
 * message, and who: everyone, a player, a team, a tag or a permission level) with the history of what
 * was sent; schedule one for later, once or repeating; keep notification templates; and list, create,
 * edit (title, description, one row per button), send and delete panels. The screen only asks the
 * server, which checks the player is an op and does the work - see {@code AdminHandler}.
 */
public class FordixAdminScreen extends FordixScreen {
    private static final int MAX_PANEL_WIDTH = 420;
    private static final int MAX_PANEL_HEIGHT = 250;
    private static final int PADDING = 10;
    private static final List<String> STYLES = List.of("info", "success", "warning", "error");
    private static final int[] SECONDS = {3, 5, 8, 15, 30};
    private static final List<String> KINDS = List.of("tous", "joueur", "equipe", "tag", "permission");
    private static final List<String> TAB_NAMES = List.of("Envoi", "Plan", "Modeles", "Panels", "Pages");
    private static final int MAX_LIST_ROWS = 7;
    private static final int SCHEDULE_ROWS = 5;
    private static final int BUTTON_ROWS = 4;
    private static final int MAX_BUTTONS = 12;
    private static final int TAB_NOTIFY = 0;
    private static final int TAB_SCHEDULE = 1;
    private static final int TAB_TEMPLATES = 2;
    private static final int TAB_PANELS = 3;
    private static final int TAB_PAGES = 4;

    private int panelWidth = MAX_PANEL_WIDTH;
    private int panelHeight = MAX_PANEL_HEIGHT;
    private final Screen parent;
    private Panel panel;
    private int tab;
    private int seenVersion = -1;
    private boolean awaitingPanel;

    // Notification form, shared by the first three tabs.
    private int styleIndex;
    private int secondsIndex = 1;
    private int kindIndex;
    private String targetValue = "";
    private String notifTitle = "";
    private String notifMessage = "";

    // Schedule.
    private boolean repeating;
    private String minutes = "30";
    private List<String> scheduleLines = List.of();

    // Templates.
    private String templateName = "";
    private List<String> templateLines = List.of();
    private int templateOffset;

    // Panel editor.
    private List<String> panelIds = List.of();
    private int listOffset;
    private String selectedId = "";
    private String newId = "";
    private String editTitle = "";
    private String editDescription = "";
    private final List<String[]> editButtons = new ArrayList<>();
    private int buttonOffset;

    // Guide pages.
    private List<String> pageLines = List.of();
    private int pageListOffset;
    private String selectedPage = "";
    private String newPageId = "";
    private String pageTitle = "";
    private String pageBody = "";
    private boolean awaitingPage;

    public FordixAdminScreen(Screen parent) {
        super(Component.literal("FORDIX // ADMINISTRATION"));
        this.parent = parent;
        ClientAdminData.reset();
        send("panels");
        send("history");
        send("schedules");
        send("templates");
        send("pages");
    }

    private static void send(String action, String... args) {
        PanelNetwork.CHANNEL.sendToServer(new ServerboundAdminPayload(action, List.of(args)));
    }

    @Override
    protected void init() {
        panelWidth = Math.min(MAX_PANEL_WIDTH, this.width - 10);
        panelHeight = Math.min(MAX_PANEL_HEIGHT, this.height - 10);
        int left = centerX(panelWidth);
        int top = centerY(panelHeight);
        this.panel = new Panel(left, top, panelWidth, panelHeight);
        int x = left + PADDING;
        int inner = panelWidth - PADDING * 2;

        int tabWidth = Math.min(96, (inner - 74) / TAB_NAMES.size() - 4);
        for (int i = 0; i < TAB_NAMES.size(); i++) {
            int index = i;
            FordixButton button = new FordixButton(x + i * (tabWidth + 4), top + 22, tabWidth, 18,
                    Component.literal((tab == i ? "> " : "") + TAB_NAMES.get(i)), () -> switchTab(index));
            addRenderableWidget(button);
        }
        addRenderableWidget(new FordixButton(left + panelWidth - PADDING - 70, top + 22, 70, 18, Component.literal("Fermer"), this::onClose));
        if (ClientAdminData.denied()) {
            return;
        }
        int y = top + 48;
        switch (tab) {
            case TAB_NOTIFY -> {
                int next = initForm(x, y, inner);
                addRenderableWidget(new FordixButton(x, next, 130, 18, Component.literal("Envoyer"), this::sendNotification));
            }
            case TAB_SCHEDULE -> initSchedule(x, y, inner);
            case TAB_TEMPLATES -> initTemplates(x, y, inner);
            case TAB_PAGES -> initPages(x, y, inner);
            default -> initPanels(x, y, inner);
        }
    }

    /** Rows of the panel and template lists: fewer on a short window so the paging buttons stay clear of the status line. */
    private int listRows() {
        return Math.max(2, Math.min(MAX_LIST_ROWS, (panelHeight - 48 - 24 - 20 - 18) / 20));
    }

    /** Shows a tab: 0 notifications, 1 schedule, 2 templates, 3 panels. */
    public void switchTab(int next) {
        tab = next;
        rebuildWidgets();
    }

    /** The notification form; returns the y below it. */
    private int initForm(int x, int y, int inner) {
        addRenderableWidget(cycle(x, y, 90, "", STYLES, () -> styleIndex, value -> styleIndex = value));
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
        return y + 72;
    }

    private void sendNotification() {
        send("notify", STYLES.get(styleIndex), String.valueOf(SECONDS[secondsIndex]), notifTitle, notifMessage,
                KINDS.get(kindIndex), targetValue);
    }

    // ---- Schedule -------------------------------------------------------------------------------

    private void initSchedule(int x, int y, int inner) {
        int next = initForm(x, y, inner);
        addRenderableWidget(new FordixButton(x, next, 96, 18, Component.literal(repeating ? "Toutes les" : "Dans"), () -> {
            repeating = !repeating;
            rebuildWidgets();
        }));
        addRenderableWidget(edit(x + 100, next, 44, "min", minutes, 5, text -> minutes = text.replaceAll("[^0-9]", "")));
        addRenderableWidget(new FordixButton(x + 148, next, 34, 18, Component.literal("min"), () -> {
        }));
        addRenderableWidget(new FordixButton(x + 186, next, 110, 18, Component.literal("Programmer"), this::schedule));
        int listY = next + 40;
        for (int i = 0; i < scheduleLines.size() && i < SCHEDULE_ROWS; i++) {
            String[] parts = scheduleLines.get(i).split("\t", 2);
            String id = parts[0];
            addRenderableWidget(new FordixButton(x + inner - 18, listY + i * 14 - 2, 18, 13, Component.literal("x"), () -> send("schedule_delete", id)));
        }
    }

    private void schedule() {
        send("schedule_add", STYLES.get(styleIndex), String.valueOf(SECONDS[secondsIndex]), notifTitle, notifMessage,
                KINDS.get(kindIndex), targetValue, repeating ? "every" : "once", minutes.isEmpty() ? "0" : minutes);
    }

    // ---- Templates ------------------------------------------------------------------------------

    private void initTemplates(int x, int y, int inner) {
        addRenderableWidget(edit(x, y, inner - 190, "Nom du modele", templateName, 32, text -> templateName = text));
        addRenderableWidget(new FordixButton(x + inner - 186, y, 186, 18, Component.literal("Enregistrer le formulaire"), () -> {
            if (!templateName.isBlank()) {
                send("template_save", templateName.trim(), STYLES.get(styleIndex), String.valueOf(SECONDS[secondsIndex]),
                        notifTitle, notifMessage);
            }
        }));
        int listY = y + 24;
        for (int i = 0; i < listRows(); i++) {
            int index = templateOffset + i;
            if (index >= templateLines.size()) {
                break;
            }
            String[] parts = templateLines.get(index).split("\t", 5);
            String name = parts[0];
            String label = name + "  ·  " + (parts.length > 3 ? parts[3] : "");
            addRenderableWidget(new FordixButton(x, listY + i * 20, inner - 22, 18,
                    Component.literal(this.font.plainSubstrByWidth(label, inner - 34)), () -> loadTemplate(parts)));
            addRenderableWidget(new FordixButton(x + inner - 18, listY + i * 20, 18, 18, Component.literal("x"),
                    () -> send("template_delete", name)));
        }
        addRenderableWidget(new FordixButton(x, listY + listRows() * 20, 58, 16, Component.literal("▲"), () -> {
            templateOffset = Math.max(0, templateOffset - listRows());
            rebuildWidgets();
        }));
        addRenderableWidget(new FordixButton(x + 62, listY + listRows() * 20, 58, 16, Component.literal("▼"), () -> {
            if (templateOffset + listRows() < templateLines.size()) {
                templateOffset += listRows();
                rebuildWidgets();
            }
        }));
    }

    /** Fills the form with a template ("name, style, seconds, title, message") and goes back to the form. */
    private void loadTemplate(String[] parts) {
        if (parts.length < 5) {
            return;
        }
        int style = STYLES.indexOf(parts[1]);
        styleIndex = Math.max(0, style);
        try {
            int seconds = Integer.parseInt(parts[2]);
            secondsIndex = 1;
            for (int i = 0; i < SECONDS.length; i++) {
                if (SECONDS[i] == seconds) {
                    secondsIndex = i;
                }
            }
        } catch (NumberFormatException ignored) {
            secondsIndex = 1;
        }
        notifTitle = parts[3];
        notifMessage = parts[4];
        templateName = parts[0];
        tab = TAB_NOTIFY;
        rebuildWidgets();
    }

    // ---- Guide pages ----------------------------------------------------------------------------

    private void initPages(int x, int y, int inner) {
        addRenderableWidget(edit(x, y, 80, "nouvel-id", newPageId, 32, text -> newPageId = text.toLowerCase()));
        addRenderableWidget(new FordixButton(x + 84, y, 36, 18, Component.literal("Creer"), () -> {
            if (!newPageId.isBlank()) {
                selectedPage = newPageId;
                pageTitle = newPageId;
                pageBody = "";
                awaitingPage = false;
                send("page_save", newPageId, newPageId, "");
                newPageId = "";
                rebuildWidgets();
            }
        }));
        int listY = y + 24;
        for (int i = 0; i < listRows(); i++) {
            int index = pageListOffset + i;
            if (index >= pageLines.size()) {
                break;
            }
            String[] parts = pageLines.get(index).split("\t", 2);
            String id = parts[0];
            addRenderableWidget(new FordixButton(x, listY + i * 20, 120, 18,
                    Component.literal((id.equals(selectedPage) ? "> " : "") + this.font.plainSubstrByWidth(parts.length > 1 ? parts[1] : id, 100)),
                    () -> selectPage(id)));
        }
        addRenderableWidget(new FordixButton(x, listY + listRows() * 20, 58, 16, Component.literal("▲"), () -> {
            pageListOffset = Math.max(0, pageListOffset - listRows());
            rebuildWidgets();
        }));
        addRenderableWidget(new FordixButton(x + 62, listY + listRows() * 20, 58, 16, Component.literal("▼"), () -> {
            if (pageListOffset + listRows() < pageLines.size()) {
                pageListOffset += listRows();
                rebuildWidgets();
            }
        }));
        if (selectedPage.isEmpty()) {
            return;
        }
        int ex = x + 130;
        int ew = inner - 130;
        addRenderableWidget(edit(ex, y, ew, "Titre de la page", pageTitle, 64, text -> pageTitle = text));
        int boxHeight = Math.max(50, panelHeight - 48 - 24 - 14 - 20 - 26 - 22);
        MultiLineEditBox body = new MultiLineEditBox(this.font, ex, y + 24, ew, boxHeight,
                Component.literal("# Titre | texte | @button Libelle | command:/spawn"), Component.literal("Contenu"));
        body.setValue(pageBody);
        body.setValueListener(text -> pageBody = text);
        addRenderableWidget(body);
        int by = y + 24 + boxHeight + 4;
        addRenderableWidget(new FordixButton(ex, by, 100, 18, Component.literal("Enregistrer"),
                () -> send("page_save", selectedPage, pageTitle, pageBody)));
        addRenderableWidget(new FordixButton(ex + 104, by, 100, 18, Component.literal("Supprimer"), () -> {
            send("page_delete", selectedPage);
            selectedPage = "";
            rebuildWidgets();
        }));
    }

    /** Opens a guide page in the editor (also used by the dev test). */
    public void pickPage(String id) {
        selectPage(id);
    }

    private void selectPage(String id) {
        selectedPage = id;
        awaitingPage = true;
        send("page", id);
        rebuildWidgets();
    }

    // ---- Panels ---------------------------------------------------------------------------------

    private void initPanels(int x, int y, int inner) {
        addRenderableWidget(edit(x, y, 80, "nouveau-id", newId, 32, text -> newId = text.toLowerCase()));
        addRenderableWidget(new FordixButton(x + 84, y, 36, 18, Component.literal("Creer"), () -> {
            if (!newId.isBlank()) {
                selectedId = newId;
                editTitle = newId;
                editDescription = "";
                editButtons.clear();
                buttonOffset = 0;
                awaitingPanel = false;
                send("save_panel", newId, newId, "");
                newId = "";
                rebuildWidgets();
            }
        }));
        int listY = y + 24;
        for (int i = 0; i < listRows(); i++) {
            int index = listOffset + i;
            if (index >= panelIds.size()) {
                break;
            }
            String id = panelIds.get(index);
            addRenderableWidget(new FordixButton(x, listY + i * 20, 120, 18, Component.literal(id.equals(selectedId) ? "> " + id : id), () -> select(id)));
        }
        addRenderableWidget(new FordixButton(x, listY + listRows() * 20, 58, 16, Component.literal("▲"), () -> {
            listOffset = Math.max(0, listOffset - listRows());
            rebuildWidgets();
        }));
        addRenderableWidget(new FordixButton(x + 62, listY + listRows() * 20, 58, 16, Component.literal("▼"), () -> {
            if (listOffset + listRows() < panelIds.size()) {
                listOffset += listRows();
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

        // One row per button: label, command, remove.
        int rowY = y + 50;
        int labelWidth = Math.min(100, ew / 3);
        for (int i = 0; i < BUTTON_ROWS; i++) {
            int index = buttonOffset + i;
            if (index >= editButtons.size()) {
                break;
            }
            String[] row = editButtons.get(index);
            int yy = rowY + i * 20;
            addRenderableWidget(edit(ex, yy, labelWidth, "Libelle", row[0], 48, text -> row[0] = text));
            addRenderableWidget(edit(ex + labelWidth + 3, yy, ew - labelWidth - 25, "Commande (ex. /spawn)", row[1], 200, text -> row[1] = text));
            addRenderableWidget(new FordixButton(ex + ew - 18, yy, 18, 18, Component.literal("x"), () -> {
                editButtons.remove(index);
                buttonOffset = Math.max(0, Math.min(buttonOffset, Math.max(0, editButtons.size() - BUTTON_ROWS)));
                rebuildWidgets();
            }));
        }
        int controlsY = rowY + BUTTON_ROWS * 20 + 2;
        addRenderableWidget(new FordixButton(ex, controlsY, 84, 16, Component.literal("+ Bouton"), () -> {
            if (editButtons.size() < MAX_BUTTONS) {
                editButtons.add(new String[]{"", ""});
                buttonOffset = Math.max(0, editButtons.size() - BUTTON_ROWS);
                rebuildWidgets();
            }
        }));
        addRenderableWidget(new FordixButton(ex + 88, controlsY, 30, 16, Component.literal("▲"), () -> {
            buttonOffset = Math.max(0, buttonOffset - 1);
            rebuildWidgets();
        }));
        addRenderableWidget(new FordixButton(ex + 122, controlsY, 30, 16, Component.literal("▼"), () -> {
            if (buttonOffset + BUTTON_ROWS < editButtons.size()) {
                buttonOffset++;
                rebuildWidgets();
            }
        }));
        int by = controlsY + 22;
        addRenderableWidget(new FordixButton(ex, by, 84, 18, Component.literal("Enregistrer"), this::savePanel));
        addRenderableWidget(new FordixButton(ex + 88, by, 84, 18, Component.literal("M'envoyer"), () -> send("send_panel", selectedId)));
        addRenderableWidget(new FordixButton(ex + 176, by, 84, 18, Component.literal("Supprimer"), () -> {
            send("delete_panel", selectedId);
            selectedId = "";
            editButtons.clear();
            rebuildWidgets();
        }));
    }

    /** Opens a panel in the editor (also used by the dev test). */
    public void selectPanel(String id) {
        select(id);
    }

    private void select(String id) {
        selectedId = id;
        awaitingPanel = true;
        buttonOffset = 0;
        send("panel", id);
        rebuildWidgets();
    }

    private void savePanel() {
        List<String> args = new ArrayList<>(List.of(selectedId, editTitle, editDescription));
        for (String[] row : editButtons) {
            if (!row[0].isBlank() && !row[1].isBlank()) {
                args.add(row[0].trim() + "\t" + row[1].trim());
            }
        }
        PanelNetwork.CHANNEL.sendToServer(new ServerboundAdminPayload("save_panel", args));
    }

    // ---- Widgets and drawing --------------------------------------------------------------------

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
        int bottom = panel.getY() + panelHeight - 14;
        String status = ClientAdminData.status();
        if (!status.isEmpty()) {
            guiGraphics.drawString(font, font.plainSubstrByWidth(status, panelWidth - PADDING * 2), x, bottom,
                    ClientAdminData.denied() ? FordixPalette.ERROR : FordixPalette.TEXT_BRIGHT, false);
        }
        if (ClientAdminData.denied()) {
            return;
        }
        int width = panelWidth - PADDING * 2;
        if (tab == TAB_NOTIFY) {
            int y = panel.getY() + 48 + 100;
            guiGraphics.drawString(font, "Historique", x, y, FordixPalette.TEXT, false);
            y += 12;
            for (String line : ClientAdminData.history()) {
                guiGraphics.drawString(font, font.plainSubstrByWidth(line, width), x, y, FordixPalette.TEXT, false);
                y += 10;
                if (y > bottom - 12) {
                    break;
                }
            }
        } else if (tab == TAB_SCHEDULE) {
            int y = panel.getY() + 48 + 72 + 40 - 12;
            guiGraphics.drawString(font, scheduleLines.isEmpty() ? "Aucune programmation" : "Programmations", x, y, FordixPalette.TEXT, false);
            y += 12;
            for (int i = 0; i < scheduleLines.size() && i < SCHEDULE_ROWS; i++) {
                String[] parts = scheduleLines.get(i).split("\t", 2);
                guiGraphics.drawString(font, font.plainSubstrByWidth(parts.length > 1 ? parts[1] : parts[0], width - 24), x, y, FordixPalette.TEXT_BRIGHT, false);
                y += 14;
            }
        } else if (tab == TAB_TEMPLATES) {
            if (templateLines.isEmpty()) {
                guiGraphics.drawString(font, "Aucun modele : remplis le formulaire (onglet Notifications), puis donne-lui un nom.",
                        x, panel.getY() + 48 + 30, FordixPalette.TEXT, false);
            }
        } else if (tab == TAB_PAGES) {
            if (selectedPage.isEmpty()) {
                guiGraphics.drawString(font, "Choisis une page, ou cree-en une.", x + 130, panel.getY() + 52, FordixPalette.TEXT, false);
            } else {
                guiGraphics.drawString(font, font.plainSubstrByWidth("# titre · @button Libelle | command:/x · ligne vide = paragraphe",
                        panelWidth - 150), x + 130, panel.getY() + panelHeight - 26, FordixPalette.BEVEL_HIGHLIGHT, false);
            }
        } else if (selectedId.isEmpty()) {
            guiGraphics.drawString(font, "Choisis un panel, ou cree-en un.", x + 130, panel.getY() + 52, FordixPalette.TEXT, false);
        } else if (editButtons.isEmpty()) {
            guiGraphics.drawString(font, "Aucun bouton : ajoute-en avec + Bouton.", x + 130, panel.getY() + 48 + 56, FordixPalette.TEXT, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (tab == TAB_PANELS && editButtons.size() > BUTTON_ROWS && mouseX > panel.getX() + 140) {
            buttonOffset = Math.max(0, Math.min(editButtons.size() - BUTTON_ROWS, buttonOffset - (int) Math.signum(delta)));
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    /** New data from the server: update the lists, fill the panel editor when the panel asked for arrives. */
    private void refresh() {
        panelIds = ClientAdminData.panels();
        scheduleLines = ClientAdminData.schedules();
        templateLines = ClientAdminData.templates();
        pageLines = ClientAdminData.pages();
        List<String> pageData = ClientAdminData.page();
        if (awaitingPage && pageData.size() >= 3 && pageData.get(0).equals(selectedPage)) {
            awaitingPage = false;
            pageTitle = pageData.get(1);
            pageBody = pageData.get(2);
        }
        List<String> data = ClientAdminData.panel();
        if (awaitingPanel && data.size() >= 3 && data.get(0).equals(selectedId)) {
            awaitingPanel = false;
            editTitle = data.get(1);
            editDescription = data.get(2);
            editButtons.clear();
            for (int i = 3; i < data.size(); i++) {
                String[] parts = data.get(i).split("\t", 2);
                editButtons.add(new String[]{parts[0], parts.length > 1 ? parts[1] : ""});
            }
        }
        rebuildWidgets();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
