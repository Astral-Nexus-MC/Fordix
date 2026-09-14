package com.omneria.fordix.client.gui;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base class for Fordix screens: adds simple layout helpers on top of vanilla
 * {@link Screen} instead of a full flex/constraint engine, since the demo/widget
 * set is still small. Extend this as more layout needs show up.
 */
public abstract class FordixScreen extends Screen {

    protected FordixScreen(Component title) {
        super(title);
    }

    /** Positions widgets one below another starting at (x, y), separated by gap. Returns the next free y. */
    protected int stackVertical(int x, int y, int gap, AbstractWidget... widgets) {
        int currentY = y;
        for (AbstractWidget widget : widgets) {
            widget.setX(x);
            widget.setY(currentY);
            currentY += widget.getHeight() + gap;
        }
        return currentY;
    }

    /** Positions widgets side by side starting at (x, y), separated by gap. Returns the next free x. */
    protected int stackHorizontal(int x, int y, int gap, AbstractWidget... widgets) {
        int currentX = x;
        for (AbstractWidget widget : widgets) {
            widget.setX(currentX);
            widget.setY(y);
            currentX += widget.getWidth() + gap;
        }
        return currentX;
    }

    protected int centerX(int width) {
        return (this.width - width) / 2;
    }

    protected int centerY(int height) {
        return (this.height - height) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
