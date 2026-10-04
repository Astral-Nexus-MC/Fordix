package com.omneria.fordix.client.gui;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.Panel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Which lines the HUD's information panels show: one switch per line (coordinates, direction, biome, FPS, TPS, ping, tool
 * of the targeted block, creatures, durability, ammunition, clocks, weather, compass), written to the config right away.
 */
public class HudLinesScreen extends FordixScreen {
    private static final int COLUMN_WIDTH = 200;
    private static final int PADDING = 10;
    private static final int GAP = 10;
    private static final int PANEL_WIDTH = PADDING * 2 + COLUMN_WIDTH * 2 + GAP;
    private static final int ROW = 18;
    private static final int BUTTON_HEIGHT = 16;

    private final Screen parent;
    private Panel panel;

    public HudLinesScreen(Screen parent) {
        super(Tr.c("lines.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int lines = FordixConfig.HUD_LINES.size();
        int rows = (lines + 1) / 2;
        int panelHeight = 24 + ROW * rows + 4 + BUTTON_HEIGHT + PADDING;
        int panelX = centerX(PANEL_WIDTH);
        int panelY = centerY(panelHeight);
        this.panel = new Panel(panelX, panelY, PANEL_WIDTH, panelHeight);
        int y = panelY + 24;
        for (int i = 0; i < lines; i++) {
            String line = FordixConfig.HUD_LINES.get(i);
            int x = panelX + PADDING + (i % 2) * (COLUMN_WIDTH + GAP);
            int rowY = y + (i / 2) * ROW;
            FordixButton[] self = new FordixButton[1];
            self[0] = new FordixButton(x, rowY, COLUMN_WIDTH, BUTTON_HEIGHT, label(line, FordixConfig.hudLine(line)), () -> {
                boolean next = !FordixConfig.hudLine(line);
                FordixConfig.hudLineValue(line).set(next);
                FordixConfig.hudLineValue(line).save();
                self[0].setMessage(label(line, next));
            });
            addRenderableWidget(self[0]);
        }
        addRenderableWidget(new FordixButton(panelX + PADDING, y + ROW * rows + 4, PANEL_WIDTH - PADDING * 2, BUTTON_HEIGHT,
                Tr.c("common.close"), this::onClose));
    }

    private static Component label(String line, boolean on) {
        return Component.literal(Tr.s("hud.line." + line) + " : " + (on ? "ON" : "OFF"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        panel.render(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, panel.getY() + 8, FordixPalette.ACCENT);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
