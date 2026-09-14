package com.omneria.fordix.client.gui;

import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.Panel;
import com.omneria.fordix.panel.network.ServerboundPanelActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * A panel built entirely from what a ClientboundOpenPanelPayload carried - no
 * per-panel Screen subclass needed. Every button just reports its index back to
 * the server; the server (not this class) decides what that button does.
 */
public class GeneratedPanelScreen extends FordixScreen {
    private static final int PANEL_WIDTH = 176;
    private static final int BUTTON_WIDTH = 156;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 6;
    private static final int TOP_PADDING = 34;
    private static final int BOTTOM_PADDING = 10;

    private final String panelId;
    private final Component description;
    private final List<String> buttonLabels;
    private Panel panel;

    public GeneratedPanelScreen(String panelId, String title, String description, List<String> buttonLabels) {
        super(Component.literal(title));
        this.panelId = panelId;
        this.description = Component.literal(description);
        this.buttonLabels = buttonLabels;
    }

    @Override
    protected void init() {
        int buttonCount = Math.max(buttonLabels.size(), 1);
        int panelHeight = TOP_PADDING + buttonCount * (BUTTON_HEIGHT + BUTTON_GAP) + BOTTOM_PADDING;
        int panelX = centerX(PANEL_WIDTH);
        int panelY = centerY(panelHeight);
        this.panel = new Panel(panelX, panelY, PANEL_WIDTH, panelHeight);

        FordixButton[] buttons = new FordixButton[buttonLabels.size()];
        for (int i = 0; i < buttonLabels.size(); i++) {
            int buttonIndex = i;
            buttons[i] = new FordixButton(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal(buttonLabels.get(i)), () ->
                    PacketDistributor.sendToServer(new ServerboundPanelActionPayload(panelId, buttonIndex)));
        }

        stackVertical(panelX + 10, panelY + TOP_PADDING, BUTTON_GAP, buttons);
        for (FordixButton button : buttons) {
            addRenderableWidget(button);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        panel.render(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, panel.getY() - 14, FordixPalette.ACCENT);
        if (!description.getString().isEmpty()) {
            guiGraphics.drawCenteredString(this.font, description, this.width / 2, panel.getY() + 12, FordixPalette.TEXT);
        }
    }
}
