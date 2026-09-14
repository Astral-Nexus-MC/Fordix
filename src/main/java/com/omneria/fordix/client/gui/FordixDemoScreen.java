package com.omneria.fordix.client.gui;

import com.omneria.fordix.client.gui.widget.Panel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Proves out the Panel + layout helpers: a small floating panel with two buttons.
 * Opened with the "Toggle Fordix Demo" keybind (right bracket by default).
 */
public class FordixDemoScreen extends FordixScreen {
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 90;

    private Panel panel;

    public FordixDemoScreen() {
        super(Component.literal("Fordix Demo"));
    }

    @Override
    protected void init() {
        int panelX = centerX(PANEL_WIDTH);
        int panelY = centerY(PANEL_HEIGHT);
        this.panel = new Panel(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);

        Button pingButton = Button.builder(Component.literal("Ping"), button ->
                this.minecraft.player.displayClientMessage(Component.literal("Fordix says hi"), true))
                .bounds(0, 0, 156, 20)
                .build();
        Button closeButton = Button.builder(Component.literal("Close"), button -> this.onClose())
                .bounds(0, 0, 156, 20)
                .build();

        stackVertical(panelX + 10, panelY + 10, 6, pingButton, closeButton);

        addRenderableWidget(pingButton);
        addRenderableWidget(closeButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        panel.render(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, panel.getY() - 14, 0xFFFFFF);
    }
}
