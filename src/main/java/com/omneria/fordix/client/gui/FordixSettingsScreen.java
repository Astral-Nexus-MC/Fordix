package com.omneria.fordix.client.gui;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.FordixClient;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.Panel;
import com.omneria.fordix.client.notification.NotificationCorner;
import com.omneria.fordix.client.notification.NotificationOverlay;
import com.omneria.fordix.notification.NotificationStyle;
import com.omneria.fordix.notification.network.ClientboundNotificationPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * In-game editor for the client options of {@link FordixConfig}: each change is written to
 * config/fordix-client.toml and applied right away, so the theme can be judged on this very
 * screen. Opened from the Mods list ("Config") or with the "Fordix settings" key.
 * The guide tab list stays in the toml file - it's free text, one entry per line.
 */
public class FordixSettingsScreen extends FordixScreen {
    private static final int PANEL_WIDTH = 220;
    private static final int PADDING = 10;
    private static final int ROW = 24;
    private static final int BUTTON_HEIGHT = 20;

    private final Screen parent;
    private Panel panel;

    public FordixSettingsScreen(Screen parent) {
        super(Component.literal("FORDIX // CONFIGURATION"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int innerWidth = PANEL_WIDTH - PADDING * 2;
        int panelHeight = 28 + ROW * 5 + 12 + ROW + 4 + BUTTON_HEIGHT + PADDING;
        int panelX = centerX(PANEL_WIDTH);
        int panelY = centerY(panelHeight);
        this.panel = new Panel(panelX, panelY, PANEL_WIDTH, panelHeight);
        int x = panelX + PADDING;
        int y = panelY + 28;

        addRenderableWidget(toggle(x, y, innerWidth, "Ecrans vanilla Voidframe", FordixConfig.VOIDFRAME_VANILLA_SCREENS));
        y += ROW;

        addRenderableWidget(toggle(x, y, innerWidth, "Barre de vie Voidframe", FordixConfig.VOIDFRAME_HUD));
        y += ROW;

        addRenderableWidget(choice(x, y, innerWidth, "Theme", Theme.NAMES,
                FordixConfig.THEME));
        y += ROW;

        addRenderableWidget(choice(x, y, innerWidth, "Notifications", NotificationCorner.OPTIONS, FordixConfig.NOTIFICATION_POSITION));
        y += ROW;

        addRenderableWidget(new FordixButton(x, y, innerWidth, BUTTON_HEIGHT,
                Component.literal("Tester une notification"), () -> NotificationOverlay.receive(
                new ClientboundNotificationPayload(NotificationStyle.INFO, Component.literal("Fordix"),
                        Component.literal("Ceci est une notification de test."), 100))));
        y += ROW;

        // Accent: a label line, then the hex box (empty keeps the theme's own accent).
        y += 12;
        EditBox accent = new EditBox(this.font, x, y, innerWidth, BUTTON_HEIGHT, Component.literal("Accent"));
        accent.setMaxLength(7);
        accent.setHint(Component.literal("#RRGGBB (vide = theme)"));
        accent.setValue(FordixConfig.ACCENT_COLOR.get());
        accent.setResponder(text -> {
            boolean valid = text.isEmpty() || Theme.parseHex(text) != null;
            accent.setTextColor(valid ? 0xE0E0E0 : 0xFF6060);
            if (valid) {
                save(FordixConfig.ACCENT_COLOR, text);
            }
        });
        addRenderableWidget(accent);
        y += ROW + 4;

        addRenderableWidget(new FordixButton(x, y, innerWidth, BUTTON_HEIGHT, Component.literal("Fermer"), this::onClose));
    }

    /** A button showing "label : ON/OFF" that flips a boolean option. */
    private FordixButton toggle(int x, int y, int width, String label, ForgeConfigSpec.BooleanValue option) {
        FordixButton[] self = new FordixButton[1];
        self[0] = new FordixButton(x, y, width, BUTTON_HEIGHT, valueText(label, option.get() ? "ON" : "OFF"), () -> {
            boolean next = !option.get();
            save(option, next);
            self[0].setMessage(valueText(label, next ? "ON" : "OFF"));
        });
        return self[0];
    }

    /** A button showing "label : value" that steps through the allowed values of a string option. */
    private FordixButton choice(int x, int y, int width, String label, List<String> values,
                                ForgeConfigSpec.ConfigValue<String> option) {
        FordixButton[] self = new FordixButton[1];
        self[0] = new FordixButton(x, y, width, BUTTON_HEIGHT, valueText(label, option.get()), () -> {
            int next = (Math.max(0, values.indexOf(option.get())) + 1) % values.size();
            save(option, values.get(next));
            self[0].setMessage(valueText(label, values.get(next)));
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

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        panel.render(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, panel.getY() + 10, FordixPalette.ACCENT);
        int accentLabelY = panel.getY() + 28 + ROW * 5;
        guiGraphics.drawString(this.font, "Couleur d'accent", panel.getX() + PADDING, accentLabelY, FordixPalette.TEXT, false);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
