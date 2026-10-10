package com.omneria.fordix.client.gui;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.ToggleRow;
import com.omneria.fordix.client.hud.HudStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Which lines the HUD's information panels show: one Voidframe 2 switch per line (coordinates, direction, biome, FPS, TPS,
 * ping, tool of the targeted block, creatures, durability, ammunition, clocks, weather, compass), grouped by panel and written
 * to the config right away.
 */
public class HudLinesScreen extends FordixScreen {
    private static final int COLUMN_WIDTH = 200;
    private static final int PADDING = 10;
    private static final int GAP = 10;
    private static final int PANEL_WIDTH = PADDING * 2 + COLUMN_WIDTH * 2 + GAP;
    private static final int ROW_HEIGHT = 13;
    private static final int GROUP_LABEL = 12;
    private static final int LABEL = 0xFF7FB7B3;

    private final Screen parent;
    private int panelX;
    private int panelY;
    private int panelHeight;
    private final List<int[]> labels = new ArrayList<>();
    private final List<String> labelTexts = new ArrayList<>();

    public HudLinesScreen(Screen parent) {
        super(Tr.c("lines.title"));
        this.parent = parent;
    }

    @Override
    protected int minWidth() {
        return PANEL_WIDTH + 10;
    }

    @Override
    protected int minHeight() {
        return 240;
    }

    @Override
    protected void init() {
        // Lines grouped by the panel they belong to (the word before the first underscore), half of the groups on each side.
        Map<String, List<String>> groups = new LinkedHashMap<>();
        for (String line : FordixConfig.HUD_LINES) {
            groups.computeIfAbsent(line.substring(0, line.indexOf('_')), key -> new ArrayList<>()).add(line);
        }
        List<String> names = new ArrayList<>(groups.keySet());
        int half = (names.size() + 1) / 2;
        int tallest = 0;
        for (int side = 0; side < 2; side++) {
            int height = 0;
            for (int i = side * half; i < Math.min(names.size(), (side + 1) * half); i++) {
                height += GROUP_LABEL + groups.get(names.get(i)).size() * (ROW_HEIGHT + 1) + 4;
            }
            tallest = Math.max(tallest, height);
        }
        panelHeight = 24 + 6 + tallest + 34;
        panelX = centerX(PANEL_WIDTH);
        panelY = centerY(panelHeight);
        labels.clear();
        labelTexts.clear();
        for (int side = 0; side < 2; side++) {
            int x = panelX + PADDING + side * (COLUMN_WIDTH + GAP);
            int y = panelY + 30;
            for (int i = side * half; i < Math.min(names.size(), (side + 1) * half); i++) {
                String name = names.get(i);
                labels.add(new int[]{x + 2, y});
                labelTexts.add(Tr.s("hud.line.group." + name).toUpperCase(java.util.Locale.ROOT));
                y += GROUP_LABEL;
                for (String line : groups.get(name)) {
                    addRenderableWidget(new ToggleRow(x, y, COLUMN_WIDTH, ROW_HEIGHT, "", 0, Tr.s("hud.line." + line), "", true,
                            FordixConfig.hudLineValue(line)));
                    y += ROW_HEIGHT + 1;
                }
                y += 4;
            }
        }
        addRenderableWidget(new FordixButton(panelX + PANEL_WIDTH - PADDING - 100, panelY + panelHeight - 27, 100, 20,
                Tr.c("common.back"), this::onClose).kind(Glass.Kind.PRIMARY));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        g.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, 0xF00B1118);
        HudStyle.panel(g, panelX, panelY, PANEL_WIDTH, panelHeight, FordixPalette.ACCENT & 0xFFFFFF, 0xEE);
        g.drawString(this.font, this.title, panelX + PADDING + 4, panelY + 8, FordixPalette.ACCENT, false);
        for (int i = 0; i < labels.size(); i++) {
            g.drawString(this.font, labelTexts.get(i), labels.get(i)[0], labels.get(i)[1] + 1, LABEL, false);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
