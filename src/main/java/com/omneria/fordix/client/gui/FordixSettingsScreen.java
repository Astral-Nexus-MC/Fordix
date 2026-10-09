package com.omneria.fordix.client.gui;

import com.omneria.fordix.Tr;
import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.FordixClient;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.Panel;
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
 * In-game editor for the client options of {@link FordixConfig}, in tabs (appearance, HUD, chat, game, administration for
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
    private static final int ROW = 24;
    private static final int BUTTON_HEIGHT = 20;
    private static final int TAB_HEIGHT = 18;
    private static final int ROWS = 6;

    /** The tab left open last time, so the screen comes back where the player was. */
    private static Tab lastTab = Tab.HUD;

    private final Screen parent;
    private Panel panel;
    private Tab tab = lastTab;
    private final List<ForgeConfigSpec.ConfigValue<?>> tabOptions = new ArrayList<>();
    private int slot;
    private int gridLeft;
    private int gridTop;
    private int[] tabBar = new int[0];

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
        int panelHeight = 26 + TAB_HEIGHT + 10 + ROW * ROWS + 6 + BUTTON_HEIGHT + PADDING;
        int panelX = centerX(PANEL_WIDTH);
        int panelY = centerY(panelHeight);
        this.panel = new Panel(panelX, panelY, PANEL_WIDTH, panelHeight);
        gridLeft = panelX + PADDING;
        gridTop = panelY + 26 + TAB_HEIGHT + 10;
        tabOptions.clear();
        slot = 0;

        // The tab bar.
        List<Tab> tabs = tabs();
        int tabWidth = (PANEL_WIDTH - PADDING * 2 - (tabs.size() - 1) * 4) / tabs.size();
        tabBar = new int[tabs.size()];
        for (int i = 0; i < tabs.size(); i++) {
            Tab each = tabs.get(i);
            int x = gridLeft + i * (tabWidth + 4);
            tabBar[i] = x;
            addRenderableWidget(new FordixButton(x, panelY + 26, tabWidth, TAB_HEIGHT,
                    Tr.c("settings.tab." + each.name().toLowerCase(java.util.Locale.ROOT)), () -> {
                lastTab = each;
                this.tab = each;
                this.rebuildWidgets();
            }));
        }

        switch (tab) {
            case HUD -> buildHud();
            case GAME -> buildGame();
            case ADMIN -> buildAdmin();
        }

        int by = gridTop + ROW * ROWS + 6;
        addRenderableWidget(new FordixButton(gridLeft, by, COLUMN_WIDTH, BUTTON_HEIGHT, Tr.c("settings.reset_tab"), this::resetTab));
        addRenderableWidget(new FordixButton(gridLeft + COLUMN_WIDTH + GAP, by, COLUMN_WIDTH, BUTTON_HEIGHT,
                Tr.c("common.close"), this::onClose));
    }

    // ---- the tabs

    private void buildHud() {
        add(toggle(Tr.s("options.compact"), FordixConfig.HUD_COMPACT));
        add(choice(Tr.s("options.opacity"), FordixConfig.HUD_OPACITIES, FordixConfig.HUD_OPACITY, " %"));
        add(button(Tr.c("settings.hud_layout"), () -> this.minecraft.setScreen(new HudLayoutScreen(this))));
        add(button(Tr.c("settings.hud_alerts"), () -> this.minecraft.setScreen(new HudAlertsScreen(this))));
        add(button(Tr.c("settings.hud_lines"), () -> this.minecraft.setScreen(new HudLinesScreen(this))));
    }

    private void buildGame() {
        add(toggle(Tr.s("options.borderless_fullscreen"), FordixConfig.BORDERLESS_FULLSCREEN));
        add(toggle(Tr.s("options.inventory_tools"), FordixConfig.INVENTORY_TOOLS));
        add(toggle(Tr.s("settings.guide_hints"), FordixConfig.GUIDE_HINTS));
    }

    private void buildAdmin() {
        add(button(Tr.c("settings.admin"), () -> this.minecraft.setScreen(new FordixAdminScreen(this))));
        add(button(Tr.c("settings.test"), () -> NotificationOverlay.receive(
                new ClientboundNotificationPayload(NotificationStyle.INFO, Component.literal("Fordix"),
                        Tr.c("settings.test_message"), 100))));
    }

    // ---- widgets

    /** Puts a widget in the next free cell of the two-column grid. */
    private void add(FordixButton widget) {
        widget.setX(gridLeft + (slot % 2) * (COLUMN_WIDTH + GAP));
        widget.setY(gridTop + (slot / 2) * ROW);
        slot++;
        addRenderableWidget(widget);
    }

    private FordixButton button(Component label, Runnable action) {
        return new FordixButton(0, 0, COLUMN_WIDTH, BUTTON_HEIGHT, label, action);
    }

    /** A button showing "label : ON/OFF" that flips a boolean option. */
    private FordixButton toggle(String label, ForgeConfigSpec.BooleanValue option) {
        tabOptions.add(option);
        FordixButton[] self = new FordixButton[1];
        self[0] = new FordixButton(0, 0, COLUMN_WIDTH, BUTTON_HEIGHT, valueText(label, option.get() ? "ON" : "OFF"), () -> {
            boolean next = !option.get();
            save(option, next);
            self[0].setMessage(valueText(label, next ? "ON" : "OFF"));
        });
        return self[0];
    }

    private FordixButton choice(String label, List<String> values, ForgeConfigSpec.ConfigValue<String> option) {
        return choice(label, values, option, "");
    }

    /** A button showing "label : value" that steps through the allowed values of a string option. */
    private FordixButton choice(String label, List<String> values, ForgeConfigSpec.ConfigValue<String> option, String unit) {
        tabOptions.add(option);
        FordixButton[] self = new FordixButton[1];
        self[0] = new FordixButton(0, 0, COLUMN_WIDTH, BUTTON_HEIGHT, valueText(label, option.get() + unit), () -> {
            int next = (Math.max(0, values.indexOf(option.get())) + 1) % values.size();
            save(option, values.get(next));
            self[0].setMessage(valueText(label, values.get(next) + unit));
        });
        return self[0];
    }

    private static Component valueText(String label, String value) {
        return Component.literal(label + " : " + value);
    }

    private static <T> void save(ForgeConfigSpec.ConfigValue<T> option, T value) {
        option.set(value);
        option.save();
        FordixClient.applyConfig();
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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        panel.render(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, panel.getY() + 10, FordixPalette.ACCENT);
        // The open tab is underlined with the accent.
        List<Tab> tabs = tabs();
        int tabWidth = (PANEL_WIDTH - PADDING * 2 - (tabs.size() - 1) * 4) / tabs.size();
        int index = tabs.indexOf(tab);
        if (index >= 0 && index < tabBar.length) {
            int y = panel.getY() + 26 + TAB_HEIGHT;
            guiGraphics.fill(tabBar[index], y - 2, tabBar[index] + tabWidth, y, 0xFF000000 | FordixPalette.ACCENT);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
