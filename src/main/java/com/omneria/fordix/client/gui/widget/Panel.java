package com.omneria.fordix.client.gui.widget;

import com.omneria.fordix.client.gui.FordixPalette;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;

/**
 * Pure background primitive: draws a filled rectangle with the carved 2px bevel
 * used across Fordix (Voidframe). It does not intercept input - that keeps it
 * trivially composable with real widgets, which the screen positions and adds
 * itself instead of routing events through this class.
 */
public class Panel implements Renderable {
    private static final int BEVEL_WIDTH = 2;

    private int x;
    private int y;
    private int width;
    private int height;

    public Panel(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        draw(guiGraphics, x, y, width, height);
    }

    /** Draws the panel background at an arbitrary rectangle, for screens that lay out their own frame. */
    public static void draw(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, FordixPalette.PANEL_FILL);
        // Raised bevel: light along top/left, dark along bottom/right.
        guiGraphics.fill(x, y, x + width, y + BEVEL_WIDTH, FordixPalette.BEVEL_HIGHLIGHT);
        guiGraphics.fill(x, y, x + BEVEL_WIDTH, y + height, FordixPalette.BEVEL_HIGHLIGHT);
        guiGraphics.fill(x, y + height - BEVEL_WIDTH, x + width, y + height, FordixPalette.BEVEL_SHADOW);
        guiGraphics.fill(x + width - BEVEL_WIDTH, y, x + width, y + height, FordixPalette.BEVEL_SHADOW);
    }
}
