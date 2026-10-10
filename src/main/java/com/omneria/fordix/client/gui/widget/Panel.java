package com.omneria.fordix.client.gui.widget;

import com.omneria.fordix.client.gui.Glass;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;

/**
 * Pure background primitive: draws a Voidframe 2 glass panel ({@link Glass#panel}). It does not intercept input - that keeps it
 * trivially composable with real widgets, which the screen positions and adds
 * itself instead of routing events through this class.
 */
public class Panel implements Renderable {
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
        Glass.panel(guiGraphics, x, y, width, height);
    }
}
