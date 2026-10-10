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

    /** The game's interface scale when this screen opened, kept to give it back; 0 while this screen has not changed it. */
    private double restoreScale;
    private double appliedScale;

    protected FordixScreen(Component title) {
        super(title);
    }

    /**
     * The smallest interface (in scaled pixels) the screen's layout reads well in; 0 = it adapts to anything. A smaller
     * interface (the game never goes under 320 x 240) is shrunk to fit: the screen is laid out at this size and the
     * interface scale lowered for as long as it is open ({@link #fitInterface}).
     */
    protected int minWidth() {
        return 0;
    }

    protected int minHeight() {
        return 0;
    }

    /**
     * Called by the screen-init event, before the widgets are built, with {@code width} x {@code height} still the game's own:
     * when the minimum does not fit, lowers the interface scale and gives the screen the larger size it implies.
     */
    public void fitInterface(net.minecraft.client.Minecraft minecraft) {
        float fit = com.omneria.fordix.client.gui.layout.Fit.factor(this.width, this.height, minWidth(), minHeight());
        if (fit >= 1.0F) {
            return;
        }
        com.mojang.blaze3d.platform.Window window = minecraft.getWindow();
        // After a window resize the game has already picked a new scale: that one is the base, not the one kept.
        if (restoreScale == 0 || window.getGuiScale() != appliedScale) {
            restoreScale = window.getGuiScale();
        }
        appliedScale = restoreScale * fit;
        window.setGuiScale(appliedScale);
        this.width = window.getGuiScaledWidth();
        this.height = window.getGuiScaledHeight();
    }

    @Override
    public void removed() {
        if (restoreScale != 0 && this.minecraft != null) {
            if (this.minecraft.getWindow().getGuiScale() == appliedScale) {
                this.minecraft.getWindow().setGuiScale(restoreScale);
            }
            restoreScale = 0;
        }
        super.removed();
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
