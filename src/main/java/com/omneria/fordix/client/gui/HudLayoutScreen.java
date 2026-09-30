package com.omneria.fordix.client.gui;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.widget.FordixButton;
import com.omneria.fordix.client.gui.widget.Panel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Moves each Voidframe HUD element with arrow buttons (4 px per click, 1 px with Shift). The HUD
 * stays visible behind this small panel, so the result is seen as you move it. Offsets are saved
 * in fordix-client.toml ([hud]).
 */
public class HudLayoutScreen extends FordixScreen {
    private static final int PANEL_WIDTH = 300;
    private static final int ROW = 22;
    private static final int PADDING = 10;
    private static final List<String> LABELS = List.of("Vie", "Faim", "Armure", "Air", "Experience", "Boss");

    private final Screen parent;
    private Panel panel;

    public HudLayoutScreen(Screen parent) {
        super(Component.literal("FORDIX // DISPOSITION DU HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int elements = FordixConfig.HUD_ELEMENTS.size();
        int panelHeight = 34 + ROW * elements + 30;
        int panelX = centerX(PANEL_WIDTH);
        // Kept near the top so the HUD at the bottom of the screen stays in view.
        int panelY = Math.max(6, this.height / 2 - panelHeight - 10);
        this.panel = new Panel(panelX, panelY, PANEL_WIDTH, panelHeight);
        for (int i = 0; i < elements; i++) {
            String element = FordixConfig.HUD_ELEMENTS.get(i);
            int y = panelY + 28 + i * ROW;
            int x = panelX + PADDING + 96;
            addRenderableWidget(arrow(x, y, "←", element, -1, 0));
            addRenderableWidget(arrow(x + 22, y, "→", element, 1, 0));
            addRenderableWidget(arrow(x + 44, y, "↑", element, 0, -1));
            addRenderableWidget(arrow(x + 66, y, "↓", element, 0, 1));
            addRenderableWidget(new FordixButton(x + 92, y, 50, 18, Component.literal("Defaut"), () -> move(element, 0, 0, true)));
        }
        int bottom = panelY + panelHeight - 26;
        addRenderableWidget(new FordixButton(panelX + PADDING, bottom, 140, 18, Component.literal("Tout reinitialiser"), () -> {
            for (String element : FordixConfig.HUD_ELEMENTS) {
                move(element, 0, 0, true);
            }
        }));
        addRenderableWidget(new FordixButton(panelX + PANEL_WIDTH - PADDING - 140, bottom, 140, 18, Component.literal("Fermer"), this::onClose));
    }

    private FordixButton arrow(int x, int y, String glyph, String element, int dx, int dy) {
        return new FordixButton(x, y, 20, 18, Component.literal(glyph), () -> {
            int step = hasShiftDown() ? 1 : 4;
            move(element, dx * step, dy * step, false);
        });
    }

    private static void move(String element, int dx, int dy, boolean reset) {
        var x = FordixConfig.hudOffsetX(element);
        var y = FordixConfig.hudOffsetY(element);
        x.set(reset ? 0 : Math.max(-400, Math.min(400, x.get() + dx)));
        y.set(reset ? 0 : Math.max(-400, Math.min(400, y.get() + dy)));
        x.save();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        panel.render(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawString(this.font, this.title, panel.getX() + PADDING, panel.getY() + 10, FordixPalette.ACCENT, false);
        for (int i = 0; i < LABELS.size(); i++) {
            String element = FordixConfig.HUD_ELEMENTS.get(i);
            int[] offset = FordixConfig.hudOffset(element);
            int y = panel.getY() + 28 + i * ROW + 5;
            guiGraphics.drawString(this.font, LABELS.get(i), panel.getX() + PADDING, y, FordixPalette.TEXT_BRIGHT, false);
            guiGraphics.drawString(this.font, offset[0] + ", " + offset[1], panel.getX() + PADDING + 96 + 146, y, FordixPalette.TEXT, false);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
