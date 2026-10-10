package com.omneria.fordix.client.gui;

import com.omneria.fordix.client.gui.layout.Layout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for Fordix screens: adds simple layout helpers on top of vanilla
 * {@link Screen}. The actual position math lives in {@link Layout}, which has no
 * Minecraft dependency and is unit tested on its own - these methods are just the
 * thin glue that applies it to real widgets.
 */
public abstract class FordixScreen extends Screen {

    protected FordixScreen(Component title) {
        super(title);
    }

    /** Positions widgets one below another starting at (x, y), separated by gap. Returns the next free y. */
    protected int stackVertical(int x, int y, int gap, AbstractWidget... widgets) {
        Layout.Result result = Layout.stackVertical(x, y, gap, sizesOf(widgets));
        applyPositions(widgets, result.positions());
        return result.nextY();
    }

    /** Positions widgets side by side starting at (x, y), separated by gap. Returns the next free x. */
    protected int stackHorizontal(int x, int y, int gap, AbstractWidget... widgets) {
        Layout.Result result = Layout.stackHorizontal(x, y, gap, sizesOf(widgets));
        applyPositions(widgets, result.positions());
        return result.nextX();
    }

    protected int centerX(int width) {
        return Layout.center(this.width, width);
    }

    protected int centerY(int height) {
        return Layout.center(this.height, height);
    }

    private static List<Layout.Size> sizesOf(AbstractWidget[] widgets) {
        List<Layout.Size> sizes = new ArrayList<>(widgets.length);
        for (AbstractWidget widget : widgets) {
            sizes.add(new Layout.Size(widget.getWidth(), widget.getHeight()));
        }
        return sizes;
    }

    private static void applyPositions(AbstractWidget[] widgets, List<Layout.Position> positions) {
        for (int i = 0; i < widgets.length; i++) {
            widgets[i].setX(positions.get(i).x());
            widgets[i].setY(positions.get(i).y());
        }
    }

    /** The Voidframe 2 backdrop ({@link Glass#backdrop}) instead of the game's flat gray dim. */
    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        Glass.backdrop(guiGraphics, this.width, this.height);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.ScreenEvent.BackgroundRendered(this, guiGraphics));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
