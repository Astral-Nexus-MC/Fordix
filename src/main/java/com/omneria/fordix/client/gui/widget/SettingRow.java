package com.omneria.fordix.client.gui.widget;

import com.omneria.fordix.client.gui.FordixPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * One line of the Voidframe 2 settings screens: a tinted icon, a title and a short explanation on the left, the control
 * (switch, choice, arrow) on the right. A faint gradient and an accent outline show the hovered or focused row.
 * A compact row (no icon, no explanation) is a single thin line.
 */
public abstract class SettingRow extends AbstractWidget {
    protected final String icon;
    protected final int tint;
    protected final String title;
    protected final String description;
    protected final boolean compact;
    /** How much a setting costs in frames per second: 0 not shown, 1 low, 2 medium, 3 high. */
    protected int cost;

    protected SettingRow(int x, int y, int width, int height, String icon, int tint, String title, String description, boolean compact) {
        super(x, y, width, height, Component.literal(title));
        this.icon = icon;
        this.tint = tint & 0xFFFFFF;
        this.title = title;
        this.description = description;
        this.compact = compact;
    }

    /** Shows a three-bar frame cost mark left of the control (video settings). */
    public SettingRow cost(int level) {
        this.cost = level;
        return this;
    }

    /** Width the control takes on the right, so the text knows where to stop. */
    protected abstract int controlWidth();

    protected abstract void renderControl(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY);

    /** The row was clicked (or activated by the keyboard with {@code mouseX} -1). */
    protected abstract void activate(double mouseX);

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        Font font = Minecraft.getInstance().font;
        boolean hot = this.active && (isHoveredOrFocused());
        int accent = FordixPalette.ACCENT & 0xFFFFFF;
        g.fillGradient(x, y, x + w, y + h, hot ? 0x2A000000 | accent : 0x16FFFFFF, hot ? 0x0C000000 | accent : 0x06FFFFFF);
        g.renderOutline(x, y, w, h, hot ? 0x66000000 | accent : 0x14FFFFFF);
        int textX = x + 6;
        if (!compact) {
            int boxY = y + (h - 18) / 2;
            g.fill(x + 4, boxY, x + 22, boxY + 18, 0x26000000 | tint);
            g.renderOutline(x + 4, boxY, 18, 18, 0x59000000 | tint);
            g.drawString(font, icon, x + 13 - font.width(icon) / 2, boxY + 5, 0xFF000000 | tint, false);
            textX = x + 28;
        }
        int room = w - (textX - x) - controlWidth() - 10 - (cost > 0 ? 14 : 0);
        boolean hasDescription = !compact && description != null && !description.isEmpty();
        int titleY = hasDescription ? y + 5 : y + (h - 8) / 2;
        g.drawString(font, fit(font, title, room), textX, titleY, this.active ? 0xFFEAF6F8 : 0xFF7D93A3, false);
        if (hasDescription) {
            g.drawString(font, fit(font, description, room), textX, y + 16, 0xFF7D93A3, false);
        }
        if (cost > 0) {
            drawCost(g, x + w - 6 - controlWidth() - 12, y + (h - 8) / 2, cost);
        }
        renderControl(g, x, y, w, h, mouseX, mouseY);
    }

    /** Three bars of rising height; the first {@code level} are lit green, amber or red. */
    private static void drawCost(GuiGraphics g, int x, int y, int level) {
        int color = level == 1 ? 0xFF5ED68A : level == 2 ? 0xFFE8A64A : 0xFFE5534B;
        int[] heights = {3, 5, 8};
        for (int i = 0; i < 3; i++) {
            g.fill(x + i * 3, y + 8 - heights[i], x + i * 3 + 2, y + 8, i < level ? color : 0x24FFFFFF);
        }
    }

    private static String fit(Font font, String text, int room) {
        if (room <= 0) {
            return "";
        }
        if (font.width(text) <= room) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, room - font.width("..."))) + "...";
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        activate(mouseX);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.active && this.visible && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE)) {
            activate(-1.0D);
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
