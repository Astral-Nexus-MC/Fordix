package com.omneria.fordix.client.gui;

import com.omneria.fordix.Tr;
import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.FordixClient;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.ChoiceRow;
import com.omneria.fordix.client.gui.widget.NavRow;
import com.omneria.fordix.client.gui.widget.TabButton;
import com.omneria.fordix.client.gui.widget.ToggleRow;
import com.omneria.fordix.client.hud.HudRules;
import com.omneria.fordix.client.hud.HudStyle;
import com.omneria.fordix.client.notification.NotificationOverlay;
import com.omneria.fordix.notification.NotificationStyle;
import com.omneria.fordix.notification.network.ClientboundNotificationPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game editor for the client options of {@link FordixConfig}, in tabs (HUD, game, administration for
 * operators): each change is written to config/fordix-client.toml and applied right away, so the theme can be judged on this
 * very screen. Opened from the Mods list ("Config") or with the "Fordix settings" key. "Reset tab" puts the options of the
 * open tab back to their defaults and leaves the other tabs alone. The guide tab list stays in the toml file (free text).
 * Keyboard shortcuts are the game's own: Options > Controls.
 */
public class FordixSettingsScreen extends FordixScreen {
    private enum Tab { HUD, GAME, ADMIN }

    private static final int COLUMN_WIDTH = 200;
    private static final int PADDING = 10;
    private static final int GAP = 10;
    private static final int PANEL_WIDTH = PADDING * 2 + COLUMN_WIDTH * 2 + GAP;
    private static final int ROW_HEIGHT = 28;
    private static final int ROW_GAP = 4;
    private static final int TAB_HEIGHT = 18;
    private static final int HEADER = 24;
    private static final int CONTENT = 146;
    private static final int FOOTER = 34;
    private static final int PANEL_HEIGHT = HEADER + TAB_HEIGHT + 8 + CONTENT + FOOTER;
    private static final int LABEL = 0xFF7FB7B3;

    /** The tab left open last time, so the screen comes back where the player was. */
    private static Tab lastTab = Tab.HUD;

    private final Screen parent;
    private Tab tab = lastTab;
    private final List<ForgeConfigSpec.ConfigValue<?>> tabOptions = new ArrayList<>();
    private final List<TabButton> tabButtons = new ArrayList<>();
    private int panelX;
    private int panelY;
    private int contentTop;
    private float inkX = -1;
    private float inkWidth;

    public FordixSettingsScreen(Screen parent) {
        super(Tr.c("settings.title"));
        this.parent = parent;
    }

    /** For the in-game test: opens on a given tab (0 = HUD, 1 = game, 2 = administration). */
    public FordixSettingsScreen(Screen parent, int tabIndex) {
        this(parent);
        this.tab = Tab.values()[Math.max(0, Math.min(Tab.values().length - 1, tabIndex))];
    }

    private boolean operator() {
        return this.minecraft != null && this.minecraft.player != null && this.minecraft.player.hasPermissions(2);
    }

    private List<Tab> tabs() {
        List<Tab> tabs = new ArrayList<>(List.of(Tab.HUD, Tab.GAME));
        if (operator()) {
            tabs.add(Tab.ADMIN);
        }
        return tabs;
    }

    @Override
    protected void init() {
        if (!tabs().contains(tab)) {
            tab = Tab.HUD;
        }
        panelX = centerX(PANEL_WIDTH);
        panelY = centerY(PANEL_HEIGHT);
        contentTop = panelY + HEADER + TAB_HEIGHT + 8;
        tabOptions.clear();
        tabButtons.clear();

        List<Tab> tabs = tabs();
        int tabWidth = 96;
        for (int i = 0; i < tabs.size(); i++) {
            Tab each = tabs.get(i);
            TabButton button = new TabButton(panelX + PADDING + i * (tabWidth + 4), panelY + HEADER, tabWidth, TAB_HEIGHT,
                    Tr.c("settings.tab." + each.name().toLowerCase(java.util.Locale.ROOT)), each == Tab.ADMIN ? "OP" : null, () -> {
                lastTab = each;
                this.tab = each;
                this.rebuildWidgets();
            });
            button.setSelected(each == tab);
            tabButtons.add(button);
            addRenderableWidget(button);
        }

        switch (tab) {
            case HUD -> buildHud();
            case GAME -> buildGame();
            case ADMIN -> buildAdmin();
        }

        int by = panelY + PANEL_HEIGHT - FOOTER + 7;
        addRenderableWidget(new FordixButton(panelX + PADDING, by, 130, 20, Tr.c("settings.reset_tab"), this::resetTab));
        addRenderableWidget(new FordixButton(panelX + PANEL_WIDTH - PADDING - 100, by, 100, 20, Tr.c("common.close"), this::onClose));
    }

    // ---- the tabs

    private int colX(int column) {
        return panelX + PADDING + column * (COLUMN_WIDTH + GAP);
    }

    private int rowY(int index) {
        return contentTop + 12 + index * (ROW_HEIGHT + ROW_GAP);
    }

    private void buildHud() {
        addRenderableWidget(toggle(0, 0, "\u25AD", 0x3FD1C7, "options.compact", "settings.desc.compact", FordixConfig.HUD_COMPACT));
        addRenderableWidget(choice(0, 1, "\u25D0", 0x3FD1C7, "options.opacity", FordixConfig.HUD_OPACITIES, FordixConfig.HUD_OPACITY));
        addRenderableWidget(new NavRow(colX(1), rowY(0), COLUMN_WIDTH, ROW_HEIGHT, "+", 0x4DA3FF, Tr.s("settings.hud_layout"),
                Tr.s("settings.desc.layout"), null, () -> this.minecraft.setScreen(new HudLayoutScreen(this))));
        addRenderableWidget(new NavRow(colX(1), rowY(1), COLUMN_WIDTH, ROW_HEIGHT, "!", 0xE8A64A, Tr.s("settings.hud_alerts"),
                Tr.s("settings.desc.alerts"), null, () -> this.minecraft.setScreen(new HudAlertsScreen(this))));
        addRenderableWidget(new NavRow(colX(1), rowY(2), COLUMN_WIDTH, ROW_HEIGHT, "=", 0xB45FE0, Tr.s("settings.hud_lines"),
                Tr.s("settings.desc.lines"), null, () -> this.minecraft.setScreen(new HudLinesScreen(this))));
    }

    private void buildGame() {
        addRenderableWidget(toggle(0, 0, "F", 0x4DA3FF, "options.borderless_fullscreen", "settings.desc.fullscreen", FordixConfig.BORDERLESS_FULLSCREEN));
        addRenderableWidget(toggle(1, 0, "?", 0xE8A64A, "settings.guide_hints", "settings.desc.hints", FordixConfig.GUIDE_HINTS));
        addRenderableWidget(toggle(0, 1, "S", 0x5ED68A, "options.inventory_tools", "settings.desc.inventory", FordixConfig.INVENTORY_TOOLS));
    }

    private void buildAdmin() {
        addRenderableWidget(new NavRow(colX(0), rowY(0), COLUMN_WIDTH, ROW_HEIGHT, "*", 0xE5534B, Tr.s("settings.admin"),
                Tr.s("settings.desc.admin"), null, () -> this.minecraft.setScreen(new FordixAdminScreen(this))));
        addRenderableWidget(new NavRow(colX(1), rowY(0), COLUMN_WIDTH, ROW_HEIGHT, "@", 0x5ED68A, Tr.s("settings.test"),
                Tr.s("settings.desc.test"), Tr.s("settings.test_word"), () -> NotificationOverlay.receive(
                new ClientboundNotificationPayload(NotificationStyle.INFO, Component.literal("Fordix"),
                        Tr.c("settings.test_message"), 100))));
    }

    // ---- widgets

    private ToggleRow toggle(int column, int row, String icon, int tint, String titleKey, String descriptionKey,
                             ForgeConfigSpec.BooleanValue option) {
        tabOptions.add(option);
        return new ToggleRow(colX(column), rowY(row), COLUMN_WIDTH, ROW_HEIGHT, icon, tint, Tr.s(titleKey), Tr.s(descriptionKey), false, option);
    }

    private ChoiceRow choice(int column, int row, String icon, int tint, String titleKey, List<String> values,
                             ForgeConfigSpec.ConfigValue<String> option) {
        tabOptions.add(option);
        return new ChoiceRow(colX(column), rowY(row), COLUMN_WIDTH, ROW_HEIGHT, icon, tint, Tr.s(titleKey), "", values, option);
    }

    /** The options of the open tab back to their defaults. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void resetTab() {
        for (ForgeConfigSpec.ConfigValue option : tabOptions) {
            option.set(option.getDefault());
            option.save();
        }
        FordixClient.applyConfig();
        this.rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int accent = FordixPalette.ACCENT & 0xFFFFFF;
        g.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xF00B1118);
        HudStyle.panel(g, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, accent, 0xEE);
        g.drawString(this.font, this.title, panelX + PADDING + 4, panelY + 8, FordixPalette.ACCENT, false);
        String subtitle = "Voidframe 2";
        g.drawString(this.font, subtitle, panelX + PANEL_WIDTH - PADDING - this.font.width(subtitle), panelY + 8, 0xFF7D93A3, false);
        // Tabs: the underline slides to the open one.
        int line = panelY + HEADER + TAB_HEIGHT;
        g.fill(panelX + 4, line, panelX + PANEL_WIDTH - 4, line + 1, 0x22FFFFFF);
        List<Tab> tabs = tabs();
        int index = tabs.indexOf(tab);
        if (index >= 0 && index < tabButtons.size()) {
            TabButton open = tabButtons.get(index);
            if (inkX < 0) {
                inkX = open.getX();
                inkWidth = open.getWidth();
            }
            inkX += (open.getX() - inkX) * 0.3F;
            inkWidth += (open.getWidth() - inkWidth) * 0.3F;
            g.fill(Math.round(inkX) - 1, line - 3, Math.round(inkX + inkWidth) + 1, line + 1, 0x223FD1C7);
            g.fill(Math.round(inkX), line - 1, Math.round(inkX + inkWidth), line + 1, 0xFF000000 | accent);
        }
        switch (tab) {
            case HUD -> drawHudLabels(g);
            case GAME -> g.drawString(this.font, Tr.s("settings.section.comfort").toUpperCase(java.util.Locale.ROOT), colX(0) + 2, contentTop + 1, LABEL, false);
            case ADMIN -> g.drawString(this.font, Tr.s("settings.section.operators").toUpperCase(java.util.Locale.ROOT), colX(0) + 2, contentTop + 1, LABEL, false);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    /** Section titles, the live preview and the box of what the server decides. */
    private void drawHudLabels(GuiGraphics g) {
        g.drawString(this.font, Tr.s("settings.section.panels").toUpperCase(java.util.Locale.ROOT), colX(0) + 2, contentTop + 1, LABEL, false);
        g.drawString(this.font, Tr.s("settings.section.more").toUpperCase(java.util.Locale.ROOT), colX(1) + 2, contentTop + 1, LABEL, false);
        int previewTop = rowY(2) + 12;
        g.drawString(this.font, Tr.s("settings.preview").toUpperCase(java.util.Locale.ROOT), colX(0) + 2, previewTop - 11, LABEL, false);
        int pw = COLUMN_WIDTH;
        int ph = 40;
        g.fillGradient(colX(0), previewTop, colX(0) + pw, previewTop + ph, 0xFF4F6F9A, 0xFF3F6A33);
        int percent;
        try {
            percent = Integer.parseInt(FordixConfig.HUD_OPACITY.get());
        } catch (NumberFormatException e) {
            percent = 70;
        }
        int alpha = Math.max(0, Math.min(255, percent * 255 / 100));
        boolean compact = FordixConfig.HUD_COMPACT.get();
        int accent = FordixPalette.ACCENT & 0xFFFFFF;
        int h1 = compact ? 12 : 17;
        HudStyle.panel(g, colX(0) + 6, previewTop + 4, 104, h1, accent, alpha);
        g.drawString(this.font, "X 128  Y 64  Z -312", colX(0) + 12, previewTop + 4 + (h1 - 8) / 2, 0xFFFFFFFF, true);
        int y2 = previewTop + 4 + h1 + 4;
        HudStyle.panel(g, colX(0) + 6, y2, 104, h1, 0x5ED68A, alpha);
        g.drawString(this.font, "FPS 120  TPS 19.9", colX(0) + 12, y2 + (h1 - 8) / 2, 0xFFFFFFFF, true);
        // What the server decides, read-only.
        int boxTop = rowY(2) + ROW_HEIGHT + 4 + 11 - 2;
        g.drawString(this.font, Tr.s("settings.section.server").toUpperCase(java.util.Locale.ROOT), colX(1) + 2, boxTop - 11, LABEL, false);
        g.fill(colX(1), boxTop, colX(1) + COLUMN_WIDTH, boxTop + 22, 0x14E8A64A);
        g.renderOutline(colX(1), boxTop, COLUMN_WIDTH, 22, 0x59E8A64A);
        String imposed = Tr.s("settings.server_imposed", HudRules.targetRange());
        g.drawString(this.font, this.font.plainSubstrByWidth(imposed, COLUMN_WIDTH - 12), colX(1) + 6, boxTop + 7, 0xFFE9C48A, false);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
