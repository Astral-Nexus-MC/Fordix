package com.omneria.fordix.client.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;

/**
 * Pure background primitive: draws a filled, bordered rectangle behind a group of
 * regular widgets that the screen positions and adds itself. It does not intercept
 * input - that keeps it trivially composable with vanilla widgets instead of
 * re-implementing click/drag/focus routing.
 */
public class Panel implements Renderable {
    private int x;
    private int y;
    private int width;
    private int height;
    private int backgroundColor = 0xC0101014;
    private int borderColor = 0xFF3A3A42;

    public Panel(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Panel backgroundColor(int argb) {
        this.backgroundColor = argb;
        return this;
    }

    public Panel borderColor(int argb) {
        this.borderColor = argb;
        return this;
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
        guiGraphics.fill(x, y, x + width, y + height, backgroundColor);
        guiGraphics.renderOutline(x, y, width, height, borderColor);
    }
}
