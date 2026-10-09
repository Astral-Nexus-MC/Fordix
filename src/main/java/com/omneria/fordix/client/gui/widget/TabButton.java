package com.omneria.fordix.client.gui.widget;

import com.omneria.fordix.client.gui.FordixPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** A tab of the Voidframe 2 screens: plain text, brighter when open; the screen draws the sliding underline. */
public class TabButton extends AbstractWidget {
    private final Runnable action;
    private boolean selected;
    private final String badge;

    public TabButton(int x, int y, int width, int height, Component label, String badge, Runnable action) {
        super(x, y, width, height, label);
        this.action = action;
        this.badge = badge;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        if (isHoveredOrFocused()) {
            g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x0EFFFFFF);
        }
        String text = getMessage().getString();
        int total = font.width(text) + (badge == null ? 0 : font.width(badge) + 10);
        int tx = getX() + (getWidth() - total) / 2;
        int ty = getY() + (getHeight() - 8) / 2 - 1;
        g.drawString(font, text, tx, ty, selected ? 0xFFFFFFFF : 0xFF7D93A3, false);
        if (badge != null) {
            int bx = tx + font.width(text) + 5;
            g.fill(bx, ty - 1, bx + font.width(badge) + 6, ty + 9, 0xFFE8A64A);
            g.drawString(font, badge, bx + 3, ty, 0xFF241500, false);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        action.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    /** Accent used by the underline. */
    public static int accent() {
        return 0xFF000000 | (FordixPalette.ACCENT & 0xFFFFFF);
    }
}
