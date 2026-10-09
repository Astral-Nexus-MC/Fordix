package com.omneria.fordix.client.gui;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.Panel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * The effects list and the alerts (worn out item, no ammunition, slow server, critical health) with their sound: one switch
 * each, written to the config right away.
 */
public class HudAlertsScreen extends FordixScreen {
    private static final int COLUMN_WIDTH = 200;
    private static final int PADDING = 10;
    private static final int GAP = 10;
    private static final int PANEL_WIDTH = PADDING * 2 + COLUMN_WIDTH * 2 + GAP;
    private static final int ROW = 22;
    private static final int BUTTON_HEIGHT = 20;

    private record Option(String label, ForgeConfigSpec.BooleanValue value) {
    }

    private final Screen parent;
    private Panel panel;

    public HudAlertsScreen(Screen parent) {
        super(Tr.c("alerts.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        List<Option> options = new ArrayList<>();
        options.add(new Option(Tr.s("options.effects"), FordixConfig.HUD_EFFECTS_LIST));
        for (String alert : FordixConfig.HUD_ALERTS) {
            options.add(new Option(Tr.s("hud.alert." + alert), FordixConfig.hudAlertValue(alert)));
        }
        int rows = (options.size() + 1) / 2;
        int panelHeight = 26 + ROW * rows + 8 + BUTTON_HEIGHT + PADDING;
        int panelX = centerX(PANEL_WIDTH);
        int panelY = centerY(panelHeight);
        this.panel = new Panel(panelX, panelY, PANEL_WIDTH, panelHeight);
        int y = panelY + 26;
        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            int x = panelX + PADDING + (i % 2) * (COLUMN_WIDTH + GAP);
            int rowY = y + (i / 2) * ROW;
            FordixButton[] self = new FordixButton[1];
            self[0] = new FordixButton(x, rowY, COLUMN_WIDTH, BUTTON_HEIGHT, label(option.label(), option.value().get()), () -> {
                boolean next = !option.value().get();
                option.value().set(next);
                option.value().save();
                self[0].setMessage(label(option.label(), next));
            });
            addRenderableWidget(self[0]);
        }
        addRenderableWidget(new FordixButton(panelX + PADDING, y + ROW * rows + 8, PANEL_WIDTH - PADDING * 2, BUTTON_HEIGHT,
                Tr.c("common.close"), this::onClose));
    }

    private static Component label(String name, boolean on) {
        return Component.literal(name + " : " + (on ? "ON" : "OFF"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        panel.render(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, panel.getY() + 10, FordixPalette.ACCENT);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
