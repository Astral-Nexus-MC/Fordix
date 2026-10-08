package com.omneria.fordix.client.gui;

import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.Panel;
import com.omneria.fordix.client.hud.HudContext;
import com.omneria.fordix.client.hud.HudContextTicker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The table of situations: one row per information panel, one column per situation (exploration, combat, mining, boss, mount);
 * a click on a cell cycles how visible the panel is there (100, 55, 15, 0 %). Applies when the automatic situations are on.
 */
public class HudContextsScreen extends FordixScreen {
    private static final int LABEL_WIDTH = 110;
    private static final int CELL_WIDTH = 56;
    private static final int ROW = 18;
    private static final int PADDING = 10;

    private final Screen parent;
    private Panel panel;
    private int tableTop;
    private int tableLeft;

    public HudContextsScreen(Screen parent) {
        super(Tr.c("contexts.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        HudContext.Kind[] kinds = HudContext.Kind.values();
        int rows = HudContext.PANELS.size();
        int panelWidth = PADDING * 2 + LABEL_WIDTH + CELL_WIDTH * kinds.length;
        int panelHeight = 24 + ROW * (rows + 1) + 6 + 16 + PADDING;
        int panelX = centerX(panelWidth);
        int panelY = centerY(panelHeight);
        panel = new Panel(panelX, panelY, panelWidth, panelHeight);
        tableLeft = panelX + PADDING + LABEL_WIDTH;
        tableTop = panelY + 24 + ROW;
        for (int row = 0; row < rows; row++) {
            String element = HudContext.PANELS.get(row);
            for (HudContext.Kind kind : kinds) {
                FordixButton[] self = new FordixButton[1];
                self[0] = new FordixButton(tableLeft + kind.ordinal() * CELL_WIDTH + 1, tableTop + row * ROW, CELL_WIDTH - 2, ROW - 2,
                        cell(kind, element), () -> {
                    HudContextTicker.matrix().cycle(kind, element);
                    self[0].setMessage(cell(kind, element));
                });
                addRenderableWidget(self[0]);
            }
        }
        int bottom = tableTop + rows * ROW + 6;
        addRenderableWidget(new FordixButton(panelX + PADDING, bottom, 120, 16, Tr.c("contexts.reset"), () -> {
            HudContextTicker.matrix().reset();
            rebuildWidgets();
        }));
        addRenderableWidget(new FordixButton(panelX + panelWidth - PADDING - 120, bottom, 120, 16, Tr.c("common.close"), this::onClose));
    }

    private static Component cell(HudContext.Kind kind, String element) {
        int percent = Math.round(HudContextTicker.matrix().factor(kind, element) * 100.0F);
        return Component.literal(percent == 0 ? "—" : percent + " %");
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        panel.render(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, panel.getY() + 8, FordixPalette.ACCENT);
        for (HudContext.Kind kind : HudContext.Kind.values()) {
            guiGraphics.drawCenteredString(this.font, Tr.s("hud.context." + kind.name().toLowerCase(java.util.Locale.ROOT)),
                    tableLeft + kind.ordinal() * CELL_WIDTH + CELL_WIDTH / 2, tableTop - ROW + 4, FordixPalette.TEXT);
        }
        for (int row = 0; row < HudContext.PANELS.size(); row++) {
            guiGraphics.drawString(this.font, Tr.s("contexts.panel." + HudContext.PANELS.get(row)),
                    panel.getX() + PADDING, tableTop + row * ROW + 4, FordixPalette.TEXT_BRIGHT, false);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
