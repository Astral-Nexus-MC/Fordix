package com.omneria.fordix.client.gui;

import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.Panel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Proves out Panel + FordixButton with the Voidframe palette: a small floating
 * panel with a description line and two buttons. Opened with the "Toggle Fordix
 * Demo" keybind (right bracket by default).
 */
public class FordixDemoScreen extends FordixScreen {
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 90;
    private static final Component DESCRIPTION = Component.literal("3 processus en attente de validation.");

    private Panel panel;

    public FordixDemoScreen() {
        super(Component.literal("SYS // TERMINAL_04"));
    }

    @Override
    protected void init() {
        int panelX = centerX(PANEL_WIDTH);
        int panelY = centerY(PANEL_HEIGHT);
        this.panel = new Panel(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);

        FordixButton runButton = new FordixButton(0, 0, 156, 20, Component.literal("Exécuter"), () ->
                this.minecraft.player.displayClientMessage(Component.literal("Fordix says hi"), true));
        FordixButton cancelButton = new FordixButton(0, 0, 156, 20, Component.literal("Annuler"), this::onClose);

        stackVertical(panelX + 10, panelY + 34, 6, runButton, cancelButton);

        addRenderableWidget(runButton);
        addRenderableWidget(cancelButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        panel.render(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, panel.getY() - 14, FordixPalette.ACCENT);
        guiGraphics.drawCenteredString(this.font, DESCRIPTION, this.width / 2, panel.getY() + 12, FordixPalette.TEXT);
    }
}
